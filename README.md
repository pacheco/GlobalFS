# SinergiaFS

Geo-distributed filesystem built on top of Multi-Ring Paxos ([URingPaxos](github.com/sambenz/URingPaxos))

## Compiling and running

First, go through the REQUIREMENTS file and compile/install the dependencies.
Once that is done, compile the project:

1. `mvn install -Dgo-build`

2. copy or symlink sinergiafs/target/sinergiafs-\*-deploy to ~/usr/sinergiafs

## Deployment overview
The simplest deployment consists of the following components:

- zookeeper
- 3 URingPaxos acceptors for the global ring
- 3 FileSystemReplica's (1 partition)
- 1 storage deployment (DHT)
- a client FUSE mountpoint

## Running locally

The fabfile-local.py can be used to start a simple local deployment,
as long as the setup is done as described in the REQUIREMENTS file.
Run the following command:

    fab -f ~/usr/sinergiafs/fabfile-local.py start_all:1

The system will be mounted under `/tmp/fs`. To umount and kill everything:

    fab -f ~/usr/sinergiafs/fabfile-local.py kill_and_clear

## Tips/Caveats

- 'dtach' is used to keep processes running in the background
- after starting everything, it can take some time for URingPaxos to
  'stabilize'. There is a small java program which can be used to
  check it is up and running (`ch.usi.paxosfs.client.CheckIfRunning`)
- **IMPORTANT** it might happen that URingPaxos doesn't boot up
  correctly, specially over high latency links (some race
  condition?). When this happens, `CheckIfRunning` will keep
  returning 1. If that is the case, kill everything and restart the
  system
- The scripts that were used to deploy on ec2 are provided under
  `src/scripts`: `fabfile-ec2.py` and `fabfile-ec2deploy.py`. The
  former is used to handle images/VMs and the latter to start/stop the
  system. They *most likely* need adjustments to work for you.

