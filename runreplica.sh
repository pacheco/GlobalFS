#!/bin/bash

mvn exec:java -Dexec.mainClass="ch.usi.paxosfs.replica.FSMain" -Dexec.argument="--partition" -Dexec.argument="1"
# --global 0 --id 2 --partition 1 --port 7777 --zoo 127.0.0.1"
