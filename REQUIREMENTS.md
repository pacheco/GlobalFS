Instalation instructions for Ubuntu as that was the platform used for
development and testing, but should work without problems in other
linuxes.

Three installation scripts are provided to make things easier. You can run them in the following order:

    install-packages-ubuntu.sh # use apt-get to install other dependencies
    fetch-deps.sh              # clone/checkout correct version of dependencies that need to be compiled
    compile-install-deps.sh    # compile and install dependencies at ~/usr/

The scripts used to deploy/run the system expects compiled
dependencies to be under `~/usr` (`~/usr/lib` and `~/usr/include` for
libraries) - this is assumed by the following instructions.


# General dependencies
Install the following packages(`apt-get install`):

    zookeeper
    zookeeperd
    dtach
    fuse
    libfuse2
    libfuse-dev
    maven
    fabric
    dstat
    python-pip
    openjdk-7-jdk   # any jdk >7
    golang
    zenity
    build-essential
    zlib1g-dev
    libbz2-dev
    libsnappy-dev

Then install python packages

    sudo pip install boto

# Compiled dependencies

These dependencies should be fetched (`fetch-deps.sh`) and compiled.

## URingPaxos

URingPaxos can be obtained from <https://github.com/sambenz/URingPaxos>.

    git clone https://github.com/sambenz/URingPaxos
    cd URingPaxos
    git checkout -f 8bfe7f4f999a64dd2c247d92c2341e4047e4ac24
    mvn install -DskipTests
    cd target
    unzip Paxos-trunk.zip
    cp -r Paxos-trunk ~/usr

## Fuse4J

Fuse4J can be obtained from <https://github.com/pacheco/fuse4j>.
Forked from <https://github.com/dtrott/fuse4j> to make it compile on
Ubuntu 64 by default.

    git clone https://github.com/pacheco/fuse4j
    cd fuse4j
    git checkout -f 729b3bb4c62b66650d97fe7f71eb21d568102a34
    mvn install

## Compiled libraries

This assumes external libraries and includes can be installed inside
`~/usr/lib` and `~/usr/include` respectively.  Set the following
variables in your `.bashrc` (if using bash):

    export LIBRARY_PATH=${HOME}/usr/lib/:$LIBRARY_PATH
    export LD_LIBRARY_PATH=${HOME}/usr/lib/:$LD_LIBRARY_PATH
    export C_INCLUDE_PATH=${HOME}/usr/include/:$C_INCLUDE_PATH
    export CPLUS_INCLUDE_PATH=${HOME}/usr/include/:$CPLUS_INCLUDE_PATH

## Fuse4J

You need to compile (`make`) the code inside `fuse4j/native` and copy
`libjavafs.so` into `~/usr/lib`.
Change `make.flags` if necessary.

## RocksDB

Get the code from github and compile

    git clone https://github.com/facebook/rocksdb/
    cd rocksdb
    git checkout -f rocksdb-3.12.1
    make shared_lib
    cp -r include/rocksdb ~/usr/include
    cp -r librock* ~/usr/lib

# Go

The storage implementation is done in go.
Install golang through `apt-get` or download the binaries and set
enviroment variables as appropriate: Assuming go is kept in
`${HOME}/usr/go` and go packages under `${HOME}/usr/gousr`:

    export PATH=${HOME}/usr/go/bin:$PATH
    export GOROOT=${HOME}/usr/go/
    export GOPATH=${HOME}/usr/gousr/

## gorocksdb

You need to have compiled/installed the RocksDB shared libraries and
have setup golang. After that, run:

    export CGO_LDFLAGS="-lrocksdb -lstdc++ -lm -lz -lbz2 -lsnappy"
    go get github.com/tecbot/gorocksdb

## goleveldb

You need to install `libleveldb-dev`

    go get github.com/jmhodges/levigo
