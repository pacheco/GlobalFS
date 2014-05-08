#!/bin/bash

export CLASSPATH="./target/paxosfs-fuse-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
java -Xms1G -Xmx3G ch.usi.paxosfs.replica.FSMain $@
