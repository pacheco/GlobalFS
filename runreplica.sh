#!/bin/bash

export CLASSPATH="./target/paxosfs-fuse-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
java ch.usi.paxosfs.replica.FSMain 1 2
