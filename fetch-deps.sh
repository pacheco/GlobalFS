#!/bin/bash

echo "> fetching correct versions of dependencies into 'deps' dir..."

mkdir -p deps
pushd deps

echo "> fetching URingPaxos"
git clone https://github.com/sambenz/URingPaxos
pushd URingPaxos
git checkout -f 8bfe7f4f999a64dd2c247d92c2341e4047e4ac24
popd

echo "> fetching Fuse4J"
git clone https://github.com/pacheco/fuse4j

echo "> fetching RocksDB"
git clone https://github.com/facebook/rocksdb/
pushd rocksdb
git checkout -f rocksdb-3.12.1
popd

popd
