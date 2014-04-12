#!/bin/bash

ZKDIR=${HOME}/usr/zookeeper-3.4.5/
ZOOHOST=localhost:2181
UPAXOSDIR=${HOME}/programming/doutorado/URingPaxos/
PAXOSFSDIR=${HOME}/Documents/workspace/paxosfs-fuse/

# start zookeeper
$ZKDIR/bin/zkServer.sh stop
rm -r /tmp/zookeeper
$ZKDIR/bin/zkServer.sh start
xterm -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh '0,0:A;1,0:A;2,0:A'"

sleep 2

# write configs
echo "
create /ringpaxos ''
create /ringpaxos/config ''
create /ringpaxos/config/multi_ring_start_time ''
create /ringpaxos/ring0 ''
create /ringpaxos/ring1 ''
create /ringpaxos/ring2 ''
create /ringpaxos/ring0/config ''
create /ringpaxos/ring1/config ''
create /ringpaxos/ring2/config ''
create /ringpaxos/ring0/config/stable_storage ''
create /ringpaxos/ring1/config/stable_storage ''
create /ringpaxos/ring2/config/stable_storage ''
set /ringpaxos/config/multi_ring_start_time `date +%s`000
set /ringpaxos/ring0/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring1/config/stable_storage ch.usi.da.paxos.storage.InMemory
set /ringpaxos/ring2/config/stable_storage ch.usi.da.paxos.storage.InMemory
" | $ZKDIR/bin/zkCli.sh -server $ZOOHOST

# # start acceptors
#xterm -geometry 120x20+0+900 -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh '0,0:A' localhost:2181" &
# sleep 0.2
# xterm -geometry 120x20+0+300 -e "cd $UPAXOSDIR; target/Paxos-trunk/ringpaxos.sh '0,1:A;1,1:A;2,1:A' localhost:2181" &
# sleep 0.2

# start replicas
xterm -geometry 120x20+0+0 -e "cd $PAXOSFSDIR; ./runreplica.sh 2 1 1 7777 localhost:2181" &
sleep 0.5
xterm -geometry 120x20+0+300 -e "cd $PAXOSFSDIR; ./runreplica.sh 2 2 1 7778 localhost:2181" &
sleep 0.5

xterm -geometry 120x20+900+0 -e "cd $PAXOSFSDIR; ./runreplica.sh 2 1 2 7779 localhost:2181" &
sleep 0.5
xterm -geometry 120x20+900+300 -e "cd $PAXOSFSDIR; ./runreplica.sh 2 2 2 7780 localhost:2181" &
sleep 0.5

# start dht
xterm -geometry 120x20+0+600 -e "cd $PAXOSFSDIR; ./src/main/python/dht.py" &
sleep 0.2


# start a terminal
xterm -geometry 120X20+900+600

wait
