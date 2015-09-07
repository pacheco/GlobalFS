# SinergiaFS

Geo distributed filesystem built on top of Multi-Ring Paxos ([URingPaxos](github.com/sambenz/URingPaxos))

## Compiling

Install the following packages (considering Ubuntu 14.04):

Oracle Java 7
   sudo add-apt-repository ppa:webupd8team/java
   sudo apt-get update
   sudo apt-get install oracle-java7-installer
zookeeper
zookeeperd
dtach
fuse
libfuse2
libfuse-dev
maven
fabric
python-kazoo
python-flask
dstat

The zookeeper executables (zkCli.sh specifically) should be available on PATH. They
are generally inside '/usr/share/zookeeper/bin'.

2 other projects are needed:

- URingPaxos - commit 8bfe7f4f999a64dd2c247d92c2341e4047e4ac24 from <https://github.com/sambenz/URingPaxos>
- fuse4j - commit 729b3bb4c62b66650d97fe7f71eb21d568102a34 from <https://github.com/dtrott/fuse4j>

For URingPaxos:
  - mvn clean install -DskipTests
  - unzip the target/Paxos-trunk.zip archive into ~/usr/Paxos-trunk

For fuse4j:
  - mvn clean install
  - compile the code (make) inside 'native' and copy libjavafs.so into ~/usr/lib
    - set the correct make.flags and paths appropriately

Once everything is setup, compile sinergiafs:
  - mvn clean install
  - copy the 'target/sinergia-0.0.1-SNAPSHOT-deploy folder into ~/usr/sinergiafs
    - Or can create a symlink there instead

## Running

The following components need to be deployed/started:

- zookeeper
- 2-3 URingPaxos acceptors for the global ring
- 2-3 FileSystemReplica's for each partition
- 1 storage deployment (DHT) for each partition
- client mount points

The fabfile-local.py should start a simple local deployment, you can
check it to figure out how to deploy everything and in what order it
should be done.

## Distributed deployment


## Tips/Caveats

- I use 'dtach' to keep processes running in the background
- After starting everything, it takes some time for URingPaxos to
  'stabilize'. There is a small java program which can be used to
  check it is up and running (ch.usi.paxosfs.client.CheckIfRunning)
- **IMPORTANT** it might happen that URingPaxos doesn't boot up
  correctly, specially over high latency links. When this happens,
  CheckIfRunning will keep returning 1. If that is the case, kill
  everything and restart the system
- I've been using fabric (python lib) to script the deployment on
  EC2. It has been useful but you can use whatever is easier
