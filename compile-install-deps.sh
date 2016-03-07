#!/bin/bash

PREFIX=${HOME}/usr
mkdir -p $PREFIX
mkdir -p $PREFIX/lib
mkdir -p $PREFIX/include

pushd deps

# URingPaxos
pushd URingPaxos
mvn install -DskipTests
cd target
unzip Paxos-trunk.zip
cp -r Paxos-trunk ~/usr
popd

# Fuse4j
pushd fuse4j/maven
mvn install
popd

pushd fuse4j/native
make
cp libjavafs.so $PREFIX/lib
popd

# RocksDB
pushd rocksdb
make shared_lib
cp -r include/rocksdb ~/usr/include
cp -r librock* ~/usr/lib
popd

popd
