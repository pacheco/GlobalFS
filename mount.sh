#!/bin/bash

JAVAFS_PATH="/Users/pacheco/usr/lib"

export CLASSPATH="./target/paxosfs-fuse-0.0.1-SNAPSHOT-jar-with-dependencies.jar":$CLASSPATH

java -Djava.library.path=$JAVAFS_PATH ch.usi.paxosfs.client.PaxosFileSystem 2 127.0.0.1:2181 $@
