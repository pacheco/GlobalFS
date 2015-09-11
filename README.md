# SinergiaFS

Geo distributed filesystem built on top of Multi-Ring Paxos ([URingPaxos](github.com/sambenz/URingPaxos))

## Compiling and running

First, go through REQUIREMENTS.md to see what needs to be installed and compiled.
Once that is done, compile the project:

1) `mvn install -Dgo-build` sinergiafs
2) copy or symlink sinergiafs/target/sinergiafs-\*-deploy to ~/usr/sinergiafs
3) to run the system locally, with one partition:

# Deployment overview

The following components need to be deployed/started:

- zookeeper
- 2-3 URingPaxos acceptors for the global ring
- 2-3 FileSystemReplica's for each partition
- 1 storage deployment (DHT) for each partition
- client mount points

## Running locally

The fabfile-local.py should start a simple local deployment, you can
check it to figure out how to deploy everything and in what order it
should be done.

    fab -f ~/usr/sinergiafs/fabfile-local.py start_all:1

The system will be mounted under `/tmp/fs`. To umount and kill everything:

    fab -f ~/usr/sinergiafs/fabfile-local.py kill_and_clear

## Distributed deployment on EC2

To deploy on ec2 you can use the provided fabric scripts (src/scripts) fabfile-ec2.py and fabfile-ec2deploy.py.
They assume the VM has been setup as per REQUIREMENTS.md.

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
