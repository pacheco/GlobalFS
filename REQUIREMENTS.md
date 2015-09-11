# General VM setup
I use ubuntu VMs and recomend the same for easy setup. Install the following packages (`apt-get install`):

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

Then install python packages

    sudo pip install boto

# Java
You need at least java 7. Get the Oracle JVM. I use ubuntu and an external PPA:

    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java7-installer
    sudo update-java-alternatives java-7-oracle

## Maven
Install maven 3 (`sudo apt-get install maven`)
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
Fuse4J can be obtained from <https://github.com/dtrott/fuse4j>.

    git clone https://github.com/dtrott/fuse4j
    cd fuse4j
    git checkout -f 729b3bb4c62b66650d97fe7f71eb21d568102a34
    mvn install

# Compiled libraries
I keep external libraries and includes inside `~/usr/lib` and `~/usr/include` respectively.

Set the following variables in your `.bashrc`:

    export LIBRARY_PATH=${HOME}/usr/lib/:$LIBRARY_PATH
    export LD_LIBRARY_PATH=${HOME}/usr/lib/:$LD_LIBRARY_PATH
    export C_INCLUDE_PATH=${HOME}/usr/include/:$C_INCLUDE_PATH
    export CPLUS_INCLUDE_PATH=${HOME}/usr/include/:$CPLUS_INCLUDE_PATH

## Fuse4J
You need to compile (`make`) the code inside `fuse4j/native` and copy `libjavafs.so` into `~/usr/lib`.
Change `make.flags` appropriatelly to make it compile.

## RocksDB
Install the following libraries (assuming Ubuntu):

    sudo apt-get install libstdc++-4.8-dev libstdc++6 zlib1g-dev zlib1g \
                         libbz2-dev libbz2-1.0 libsnappy1 libsnappy-dev

Get the code from github and compile

    git clone https://github.com/facebook/rocksdb/
    cd rocksdb
    git checkout -f rocksdb-3.12.1
    make shared_lib
    cp -r include/rocksdb ~/usr/include
    cp -r librock* ~/usr/lib

# Go
Download the binaries (1.5) and extract it inside `~/usr/`. Full path should be `~/usr/go`.j

I keep go packages inside `~/usr/gousr`. For that, set the following variables in your `.bashrc`:

    export PATH=${HOME}/usr/go/bin:$PATH
    export GOROOT=${HOME}/usr/go/
    export GOPATH=${HOME}/usr/gousr/

## gorocksdb
You need to have compiled/installed the RocksDB shared libraries and have setup golang. After that, run:

    export CGO_LDFLAGS="-lrocksdb -lstdc++ -lm -lz -lbz2 -lsnappy"
    go get github.com/tecbot/gorocksdb
