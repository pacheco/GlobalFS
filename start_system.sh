#!/bin/bash

ZKDIR=${HOME}/usr/zookeeper-3.4.5/
ZOOHOST=localhost:2181
UPAXOSDIR=${HOME}/programming/doutorado/URingPaxos/
#UPAXOSDIR=${HOME}/programming/doutorado/eduardo_uringpaxos/
PAXOSFSDIR=${HOME}/Documents/workspace/paxosfs-fuse/

PARTITIONS=$1

if [ -z $PARTITIONS ]; then
    echo "start <partitions>"
    exit
fi

# start zookeeper
$ZKDIR/bin/zkServer.sh stop
rm -r /tmp/zookeeper
$ZKDIR/bin/zkServer.sh start
xterm -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh '0,0:A;1,0:A;2,0:A;3,0:A;4,0:A;5,0:A;6,0:A;7,0:A;8,0:A'"

sleep 2

# write configs
echo "
set /ringpaxos/config/multi_ring_start_time `date +%s`000
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
" | $ZKDIR/bin/zkCli.sh -server $ZOOHOST


# start replicas
X=200
for p in `seq 1 $PARTITIONS`; do
    #if [[ $p -ne 1 ]]; then
        xterm -geometry 120x10+0+$X -e "cd $PAXOSFSDIR; ./runreplica.sh $PARTITIONS $p 1 $[31000 + 2*p] $ZOOHOST" &
    #fi
    X=$[X+100]
    sleep 0.5
    xterm -geometry 120x10+0+$X -e "cd $PAXOSFSDIR; ./runreplica.sh $PARTITIONS $p 2 $[31000 + 2*p+1] $ZOOHOST" &
    X=$[X+100]
    sleep 0.5
done

# start acceptors for the big ring (these HAVE to be started last due to a strange behaviour of ring paxos latency)
xterm -geometry 120x20+0+0 -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh 0,0:A $ZOOHOST" &
sleep 0.5
xterm -geometry 120x20+0+100 -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh 0,1:A $ZOOHOST" &
sleep 2

# start dht
xterm -geometry 120x20+900+0 -e "cd $PAXOSFSDIR; ./src/main/python/dht.py" &
sleep 0.2

# start a terminal
xterm -geometry 120X20+900+600

wait
