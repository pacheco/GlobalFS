#!/bin/bash

export CLASSPATH="./target/foo/"
java ch.usi.paxosfs.replica.FSMain $@
