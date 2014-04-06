package ch.usi.paxosfs.replica;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.examples.Util;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;

public class FSMain {
	static Thread replica;
	static TServer thriftServer;
	
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
	private static void startReplica(int id, byte partition, String host, int port, CommunicationService comm, String zoohost) throws TTransportException {
		// start replica thread
		FileSystemReplica learner = new FileSystemReplica(id, partition, comm, host, port, zoohost);
		replica = new Thread(learner);
		replica.start();
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
		final CommunicationService comm = new CommunicationService(args.replicaId, args.replicaPartition, node);
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
			InetSocketAddress addr = new InetSocketAddress(Util.getHostAddress(false), args.serverPort);
			startReplica(args.replicaId, args.replicaPartition, addr.getHostString(), args.serverPort, comm, args.zookeeperHost);
		} catch (TTransportException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
