#!/bin/bash

export CLASSPATH="./target/paxosfs-fuse-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
java ch.usi.paxosfs.client.CommandLineClient 127.0.0.1 7777
