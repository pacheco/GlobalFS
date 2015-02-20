#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh

ZKHOST=localhost:2181
UPAXOSDIR=${HOME}/usr/Paxos-trunk/
FSDIR=${HOME}/usr/sinergiafs/

LIBPATH=${HOME}/usr/lib
JVMOPT="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
GC="-XX:+UseConcMarkSweepGC"
PORT_START=20000


PARTITIONS=$1
NCLI=$2
NBLOCKS=$3
BLOCKSIZE=$4

if [[ -z $PARTITIONS || -z $NCLI || -z $NBLOCKS || -z $BLOCKSIZE ]]; then
    echo "start <partitions> <nclients> <nblocks> <blocksize>"
    exit
fi

## start zookeeper
# $ZKDIR/bin/zkServer.sh stop
# rm -r /tmp/zookeeper
# $ZKDIR/bin/zkServer.sh start
# pushd $UPAXOSDIR;
# target/Paxos-trunk/ringpaxos.sh '0,0:A;1,0:A;2,0:A;3,0:A;4,0:A;5,0:A;6,0:A;7,0:A;8,0:A' &
# popd

#sleep 3
#killall -INT ringpaxos.sh

STORAGE=ch.usi.da.paxos.storage.CyclicArray #  BufferArray uses too much memory to run multiple instances locally

echo "
set /ringpaxos/config/multi_ring_start_time `date +%s`000
set /ringpaxos/config/multi_ring_lambda 20000
set /ringpaxos/config/multi_ring_delta_t 10
set /ringpaxos/topology0/config/stable_storage $STORAGE
set /ringpaxos/topology0/config/tcp_nodelay 1
set /ringpaxos/topology1/config/stable_storage $STORAGE
set /ringpaxos/topology1/config/tcp_nodelay 1
set /ringpaxos/topology2/config/stable_storage $STORAGE
set /ringpaxos/topology2/config/tcp_nodelay 1
set /ringpaxos/topology3/config/stable_storage $STORAGE
set /ringpaxos/topology3/config/tcp_nodelay 1
set /ringpaxos/topology4/config/stable_storage $STORAGE
set /ringpaxos/topology4/config/tcp_nodelay 1
set /ringpaxos/topology5/config/stable_storage $STORAGE
set /ringpaxos/topology5/config/tcp_nodelay 1
set /ringpaxos/topology6/config/stable_storage $STORAGE
set /ringpaxos/topology6/config/tcp_nodelay 1
set /ringpaxos/topology7/config/stable_storage $STORAGE
set /ringpaxos/topology7/config/tcp_nodelay 1
" | zkCli.sh -server $ZKHOST


# start paxos small rings
port=$PORT_START
CLASSPATH=$FSDIR
for p in `seq 1 $PARTITIONS`; do
    for id in `seq 0 2`; do
        echo "java -ea -cp $CLASSPATH $JVMOPT $GC -Djava.library.path=$LIBPATH ch.usi.paxosfs.replica.FSMainPopulated $PARTITIONS $p $id $port $ZKHOST $NCLI $NBLOCKS $BLOCKSIZE; sleep 10"
        xterm -e "java -ea -cp $CLASSPATH $JVMOPT $GC -Djava.library.path=$LIBPATH ch.usi.paxosfs.replica.FSMainPopulated $PARTITIONS $p $id $port $ZKHOST $NCLI $NBLOCKS $BLOCKSIZE; sleep 10" &
        ((port++))
    done
done

# start acceptors for the big ring (these HAVE to be started last due to a strange behaviour of ring paxos latency)
xterm -e "cd $UPAXOSDIR; ./ringpaxos.sh 0,0:A $ZKHOST; sleep 10" &
sleep 0.5
xterm -e "cd $UPAXOSDIR; ./ringpaxos.sh 0,1:A $ZKHOST; sleep 10" &
sleep 2

wait
