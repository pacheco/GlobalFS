#!/bin/bash

thrift --gen java -out . fuseops.thrift
thrift --gen java -out . commands.thrift
cp -r ch ../java/
