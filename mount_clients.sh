#!/bin/bash

ZKDIR=/home/pacheco/usr/zookeeper-3.4.5/
ZOOHOST=node249:2182
UPAXOSDIR=/home/pacheco/programming/eduardo_URingPaxos/
PAXOSFSDIR=/home/pacheco/programming/paxosfs-fuse/


PARTITIONS=$1
CLIENTS=$2
START_NODE=1

if [[ -z $PARTITIONS || -z $CLIENTS ]]; then
    echo "start <n_partitions> <clients>"
    exit 1
fi

# STORAGE_NODES=(30 31 32 33 34 35 36 37 38 39 40)
STORAGE_NODES=(36 37 38 39 40)
STORAGE_PORT=30001

# STORAGE_NODES=(26 27 28 29)
# STORAGE_PORT=5000

STORAGE_NODES_N=${#STORAGE_NODES[@]}

#CLIENT_NODES=(24 25 26 27 28 29)
CLIENT_NODES=(35)
CLIENT_NODES_N=${#CLIENT_NODES[@]}

REP_IDS=(1)
REP_IDS_N=${#REP_IDS[@]}

# kill and umount all
for C in ${CLIENT_NODES[@]}; do
    ssh node$C <<EOF
sudo killall -9 java
for dir in \`find /tmp -maxdepth 1 -name 'fs*'\`; do
    sudo umount -lf \$dir;
    rmdir \$dir;
done
EOF
done

# mount the clients
for C in `seq 0 $[CLIENTS-1]`; do
    node=node${CLIENT_NODES[C % CLIENT_NODES_N]}
    storage=node${STORAGE_NODES[C % STORAGE_NODES_N]}
    rep_id=${REP_IDS[C % REP_IDS_N]}
    dir=/tmp/fs$C
    echo "mounting client $C on $node at $dir"
    ssh $node <<EOF
cd $PAXOSFSDIR;
mkdir -p $dir;
nohup ./mount.sh $PARTITIONS $ZOOHOST http://${storage}:${STORAGE_PORT} ${rep_id} $dir -f -o direct_io 2> /dev/null > /dev/null < /dev/null &
EOF
done
