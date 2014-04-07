# SinergiaFS

Consistent distributed filesystem using Paxos.

## Dependencies

Most dependencies will be fetched by maven. These you'll have to build yourself

- Fuse4J: https://github.com/dtrott/fuse4j
          You'll need to have the javafs native lib (from Fuse4J) in the java.library.path to mount the filesystem
- URingPaxos: https://github.com/sambenz/uringpaxos

You also need to have Zookeeper running. It is used by URingPaxos and also by the client to find replicas.

## Running

