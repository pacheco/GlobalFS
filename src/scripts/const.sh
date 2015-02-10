#!/bin/bash

export CLASSPATH=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

export ZKDIR=${HOME}/usr/zookeeper
export UPAXOSDIR=${HOME}/usr/Paxos-trunk
export FSDIR=${HOME}/usr/sinergiafs
export LIBPATH=${HOME}/usr/lib

export JVMOPT="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
export GC="-XX:+UseConcMarkSweepGC"
#export GC="-XX:+UseParallelGC"
#export GC="-XX:+UseG1GC"
#export GC="-XX:+UseG1GC -XX:MaxGCPauseMillis=20"

export M=1
export DELTA=10
export LAMBDA=100000 # set LAMBDA to 0 when disabling MRP

export STORAGE=ch.usi.da.paxos.storage.BufferArray
#export STORAGE=ch.usi.da.paxos.storage.BerkeleyStorage
#export STORAGE=ch.usi.da.paxos.storage.SyncBerkeleyStorage
#export STORAGE=ch.usi.da.paxos.storage.CyclicArray

#export BATCH=ch.usi.da.paxos.batching.SmallBatchPolicy
#export BATCH=ch.usi.da.paxos.batching.LargeBatchPolicy
#export BATCH=ch.usi.da.paxos.batching.Batch5Milli
#export BATCH=ch.usi.da.paxos.batching.Batch10Milli
#export BATCH=ch.usi.da.paxos.batching.IntervalBatchPolicy
export BATCH=none

export RECOVERY=1
export TRIM_MOD=0
export TRIM_AUTO=0
export P1_TIMEOUT=10000
export PROPOSER_TIMEOUT=10000

export REF_RING=0 # set REF_RING to non-existant ring do disable latency compensation

# these are used only by the embedded URingPaxos benchmark
export VALUE_SIZE=32
export VALUE_COUNT=900000
