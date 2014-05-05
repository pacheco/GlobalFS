#!/bin/bash

ZKDIR=/home/pacheco/usr/zookeeper-3.4.5/
ZOOHOST=node249:2182
#UPAXOSDIR=/home/pacheco/programming/eduardo_URingPaxos/
UPAXOSDIR=/home/pacheco/programming/URingPaxos/
PAXOSFSDIR=/home/pacheco/programming/paxosfs-fuse/


PARTITIONS=$1
START_NODE=22

if [ -z $PARTITIONS ]; then
    echo "start <n_partitions>"
    exit 1
fi


# start zookeeper
ssh dslab <<EOF
$ZKDIR/bin/zkServer.sh stop
rm -r /tmp/zookeeper
$ZKDIR/bin/zkServer.sh start
cd $UPAXOSDIR
target/Paxos-trunk/ringpaxos.sh '0,0:A;1,0:A;2,0:A;3,0:A;4,0:A;5,0:A;6,0:A;7,0:A;8,0:A' $ZOOHOST &
sleep 6
EOF

# write configs
ssh dslab <<EOF
echo "
set /ringpaxos/config/multi_ring_start_time \`date +%s\`000
set /ringpaxos/config/multi_ring_lambda 900
set /ringpaxos/ring0/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring0/config/tcp_nodelay 1
set /ringpaxos/ring1/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring1/config/tcp_nodelay 1
set /ringpaxos/ring2/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring2/config/tcp_nodelay 1
set /ringpaxos/ring3/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring3/config/tcp_nodelay 1
set /ringpaxos/ring4/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring4/config/tcp_nodelay 1
set /ringpaxos/ring5/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring5/config/tcp_nodelay 1
set /ringpaxos/ring6/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring6/config/tcp_nodelay 1
set /ringpaxos/ring7/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring7/config/tcp_nodelay 1
set /ringpaxos/ring8/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring8/config/tcp_nodelay 1
" | $ZKDIR/bin/zkCli.sh -server $ZOOHOST
EOF


# 2 replicas (with acceptor) per ring plus the 2 acceptors for the big ring
for n in `seq $START_NODE $[START_NODE + PARTITIONS*2 + 1]`; do
    ssh node$n sudo killall -9 java
done

sleep 3

# start replicas
X=200
N=$START_NODE
for p in `seq 1 $PARTITIONS`; do
    xterm -geometry 120x10+0+$X -e ssh node$N "cd $PAXOSFSDIR; ./runreplica.sh $PARTITIONS $p 1 31000 $ZOOHOST" &
    N=$[N+1]
    X=$[X+100]
    sleep 0.5
    xterm -geometry 120x10+0+$X -e ssh node$N "cd $PAXOSFSDIR; ./runreplica.sh $PARTITIONS $p 2 31000 $ZOOHOST" &
    N=$[N+1]
    X=$[X+100]
    sleep 0.5
done

# start acceptors for the big ring (these HAVE to be started last due to a strange behaviour of ring paxos latency)
xterm -geometry 120x20+0+0 -e ssh node$N cd $UPAXOSDIR\; target/Paxos-trunk/ringpaxos.sh 0,0:A $ZOOHOST &
N=$[N + 1]
sleep 0.5
xterm -geometry 120x20+0+100 -e ssh node$N cd $UPAXOSDIR\; target/Paxos-trunk/ringpaxos.sh 0,1:A $ZOOHOST &
N=$[N + 1]
sleep 0.5

wait