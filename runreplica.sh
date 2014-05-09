#!/bin/bash

export CLASSPATH="./target/paxosfs/"
java -Xms1G -Xmx3G ch.usi.paxosfs.replica.FSMain $@
