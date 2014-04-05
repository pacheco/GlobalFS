#!/bin/bash

thrift --gen py -out . fuseops.thrift
cp -r paxosfs ../python
mv ../python/paxosfs/FuseOps-remote ../python/
rm __init__.py
