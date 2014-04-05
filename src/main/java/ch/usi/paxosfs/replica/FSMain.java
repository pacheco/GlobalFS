package ch.usi.paxosfs.replica;


import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.paxosfs.partitioning.SinglePartitionOracle;
import ch.usi.paxosfs.rpc.FuseOps;

public class FSMain {
	static Thread[] thriftProposers;
	static Thread fuseOpsServer;
	static Thread replica;
	static TServer thriftServer;
	static int workerThreads = 20;
	
	private static class Options {
		public int serverPort = 7777;
		public String zookeeperHost = "127.0.0.1:2181";
		public int replicaId;
		public byte replicaPartition;
	}

	private static Options parseArgs(String[] args) {
		Options opt = new Options();
		if (args.length < 2) {
			System.err.println("usage: FSMain replicaPartition replicaId [serverPort] [zooHost]");
			System.exit(1);
		}
		opt.replicaPartition = Byte.parseByte(args[0]);
		opt.replicaId = Integer.parseInt(args[1]);
	    if (args.length > 2) {
	    	opt.serverPort = Integer.parseInt(args[2]);
	    }
	    if (args.length > 3) {
	    	opt.zookeeperHost = args[3];
	    }
		return opt;
	}
	
	private static Node startPaxos(List<RingDescription> rings, String zoohost) {
		final Node node = new Node(zoohost, rings);
		try {
			node.start();
		} catch (IOException | KeeperException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		return node;
	}
	
	/**
	 * Start replica and serve clients. Does not return.
	 * @param comm
	 * @param partition
	 * @throws TTransportException
	 */
	private static void startReplica(int id, byte partition, int port, CommunicationService comm) throws TTransportException {
		// start replica thread
		FileSystemReplica learner = new FileSystemReplica(id, partition, comm);
		replica = new Thread(learner);
		replica.start();
		// start thrift server
		FuseOpsHandler fuseHandler = new FuseOpsHandler(id, partition, learner, new SinglePartitionOracle(partition));
		TProcessor fuseProcessor = new FuseOps.Processor<FuseOpsHandler>(fuseHandler);
		TServerTransport serverTransport = new TServerSocket(port);
		TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
		args.maxWorkerThreads(workerThreads);
		args.minWorkerThreads(workerThreads);
		TThreadPoolServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(fuseProcessor));
		server.serve();
	}
	
	public static void main(String[] rawargs) {
		// argument parsing
		Options args = parseArgs(rawargs);
		int globalRing = 0;
		int globalid = args.replicaId + args.replicaPartition*100; // id of the node in the global ring

		
		// start paxos node
		List<RingDescription> rings = new LinkedList<RingDescription>();
		rings.add(new RingDescription(globalRing, globalid, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));
		rings.add(new RingDescription(args.replicaPartition, args.replicaId, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));
		final Node node = startPaxos(rings, args.zookeeperHost);
		if (node == null) {
			System.err.println("Error starting paxos");
			System.exit(1);
		}
		
		// start communication service
		final CommunicationService comm = new CommunicationService(node);
		comm.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				try {
					node.stop();
					comm.stop();
				} catch (InterruptedException e) {
				}
			}
		});

		// start the replica
		try {
			startReplica(args.replicaId, args.replicaPartition, args.serverPort, comm);
		} catch (TTransportException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
