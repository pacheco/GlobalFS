#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh

JVMOPT="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
PORT=20000

N_PARTITIONS=$1

# get node information
source ~/whoami.sh

LOG=~/${NAME}.log
rm $LOG

# letency compensation for ring 0,1,2,3...
# from wait queue*delta
#
LATCOMP=(0 0 0 0 0 0 0 0 0 0) # no latency compensation
#LATCOMP=(0 130 30 70) # us-west-2 us-east-1 eu-west-1
#LATCOMP=(0 70 0 40) # dep3 as first three dcs of dep9 (all US)
#LATCOMP=(0 260 0 50 100 100 220) # dep6 as first six dcs of dep9
#LATCOMP=(0 600 20 70 90 100 210 270 340 500) # dep9



DB=/mnt/

case $NAME in
    rep*)
        # start replica
        # ----------------------------------------------------
        # GLOBALID=$[RING*100 + ID]
        # echo "export EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${GLOBALID}:PL\;${RING},${ID}:PAL $ZKHOST:2182" | tee -a $LOG
        # sudo sh -c "export EC2=$EC2; $UPAXOSDIR/ringpaxos.sh 0,${GLOBALID}:PL\;${RING},${ID}:PAL $ZKHOST:2182" | tee -a $LOG
        # ;;
        FULLCMD="export DB=$DB; export LAT=${LATCOMP[RING]}; export EC2=$EC2; java -Xmx2G -Xms1G -ea -cp $CLASSPATH $JVMOPT $GC -Djava.library.path=$LIBPATH ch.usi.paxosfs.replica.FSMain -n $N_PARTITIONS -p $RING -i $ID -s $PORT -z $ZKHOST:2182"

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
