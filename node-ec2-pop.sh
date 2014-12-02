#!/bin/bash

CLASSPATH=sinergiafs/
UPAXOSDIR=Paxos-trunk/
LIBPATH=usr/lib/
JVMOPT="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
#export GC="-XX:+UseParallelGC"
GC="-XX:+UseConcMarkSweepGC"
PORT=20000

# get node information
source ~/whoami.sh

LOG=~/${NAME}.log
rm $LOG

# letency compensation for ring 0,1,2,3...
# from wait queue*delta
#
# LATCOMP=(0 160 20 100) # us-east-1 us-west-2 eu-west-1

LATCOMP=(0 170 10 90)
DB=/ssd/storage/

# PARAMS
# ------------------
NCLI=$1
NBLOCKS=$2
BLOCKSIZE=$3

if [[ $# != 3 ]]; then
    echo "node.sh <ncli> <nblocks> <blocksize>"
    exit 1
fi


case $NAME in
    rep*)
        # start replica
        # ----------------------------------------------------
        # GLOBALID=$[RING*100 + ID]
        # echo "export EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${GLOBALID}:PL\;${RING},${ID}:PAL $ZKHOST:2182" | tee -a $LOG
        # sudo sh -c "export EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${GLOBALID}:PL\;${RING},${ID}:PAL $ZKHOST:2182" | tee -a $LOG
        # ;;
        FULLCMD="export DB=$DB; export LAT=${LATCOMP[RING]}; export EC2=$EC2; java -Xmx2G -Xms1G -ea -cp $CLASSPATH $JVMOPT $GC -Djava.library.path=$LIBPATH ch.usi.paxosfs.replica.FSMainPopulated 3 $RING $ID $PORT $ZKHOST:2182 $NCLI $NBLOCKS $BLOCKSIZE"

        echo $FULLCMD | tee -a $LOG
        sh -c "$FULLCMD"  | tee -a $LOG
        ;;
    acc*)
        # start acceptor
        # ----------------------------------------------------

        # set the id so as to build the ring for optimal latency
        if [[ $RING == 1 ]]; then
            # for the global coordinator, we place it after the learner/replicas (which have id's 101, 102...) to avoid going around the ring to propose (double the latency!)
            ID=$[RING*100 + 50 - ID]
        else
            # learners/replicas are 101, 102 in the global ring... the global acceptors in this DC will be 99, 98... so they come before the replicas
            ID=$[RING*100 - 1 - ID]
        fi
        echo "export DB=$DB; export EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${ID}:A $ZKHOST:2182" | tee -a $LOG
        sh -c "export DB=$DB; EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${ID}:A $ZKHOST:2182" | tee -a $LOG
        ;;
esac
