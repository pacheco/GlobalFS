#!/bin/bash

export ZKDIR=${HOME}/usr/zookeeper-3.4.5/
export UPAXOSDIR=Paxos-trunk/
#export UPAXOSDIR=/home/pacheco/programming/EduardoPaxos-trunk/
export CLASSPATH=paxos-bench/
export LIBPATH=usr/lib/
export JVMOPT="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
#export GC="-XX:+UseParallelGC"
export GC="-XX:+UseConcMarkSweepGC"
#export GC="-XX:+UseG1GC"
#export GC="-XX:+UseG1GC -XX:MaxGCPauseMillis=20"

export M=1
export LAMBDA=100000
#export LAMBDA=0
export DELTA=10
#export STORAGE=ch.usi.da.paxos.storage.BerkeleyStorage
#export STORAGE=ch.usi.da.paxos.storage.SyncBerkeleyStorage
#export STORAGE=ch.usi.da.paxos.storage.CyclicArray
export STORAGE=ch.usi.da.paxos.storage.BufferArray
#export BATCH=ch.usi.da.paxos.batching.SimpleBatchPolicy
#export BATCH=ch.usi.da.paxos.batching.Batch5Milli
#export BATCH=ch.usi.da.paxos.batching.Batch10Milli
#export BATCH=ch.usi.da.paxos.batching.IntervalBatchPolicy
export BATCH=none
export VALUE_SIZE=32
export VALUE_COUNT=900000
export RECOVERY=1
export TRIM_MOD=0
export TRIM_AUTO=0
export P1_TIMEOUT=10000
export PROPOSER_TIMEOUT=10000
#export REF_RING=13
export REF_RING=0
