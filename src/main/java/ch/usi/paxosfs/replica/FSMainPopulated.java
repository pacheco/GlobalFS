package ch.usi.paxosfs.replica;


import ch.usi.da.paxos.Util;
import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.paxosfs.filesystem.FileSystem;
import ch.usi.paxosfs.filesystem.memory.MemFileSystem;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.util.UUIDUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class FSMainPopulated {
	private static Logger log = Logger.getLogger(FSMainPopulated.class);
	private static Thread replica;
	private static Random rand = new Random();

	private static class Options {
		public int serverPort = 7777;
		public String zookeeperHost = "127.0.0.1:2181";
		public int replicaId;
		public byte replicaPartition;
		public int nPartitions;
		public int nCli;
		public int nBlocks;
		public int blockSize;
	}

	private static Options parseArgs(String[] args) {
		Options opt = new Options();
		if (args.length < 2) {
			System.err.println("usage: FSMain n_partitions replicaPartition replicaId [serverPort] [zooHost] nCli nBlocks blockSize");
			System.exit(1);
		}
		opt.nPartitions = Integer.parseInt(args[0]);
		opt.replicaPartition = Byte.parseByte(args[1]);
		opt.replicaId = Integer.parseInt(args[2]);
	    if (args.length > 3) {
	    	opt.serverPort = Integer.parseInt(args[3]);
	    }
	    if (args.length > 4) {
	    	opt.zookeeperHost = args[4];
	    }
		opt.nCli = Integer.parseInt(args[5]);
		opt.nBlocks = Integer.parseInt(args[6]);
		opt.blockSize = Integer.parseInt(args[7]);
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
	 * @throws org.apache.thrift.transport.TTransportException
	 */
	private static void startReplica(int nPartitions, int id, byte partition, String host, int port, CommunicationService comm, String zoohost, FileSystem fs) throws TTransportException {
		// start replica thread
		FileSystemReplica learner = new FileSystemReplica(nPartitions, id, partition, comm, host, port, zoohost, fs);
		replica = new Thread(learner, "FS Replica");
		replica.start();
	}
	
	public static void main(String[] rawargs) throws TException {
		// argument parsing
		Options args = parseArgs(rawargs);
		int globalRing = 0;
		int globalid = args.replicaId + args.replicaPartition*100; // id of the node in the global ring

		
		List<RingDescription> rings = new LinkedList<RingDescription>();
		// replicas are not acceptors on the big ring
		rings.add(new RingDescription(globalRing, globalid, Arrays.asList(PaxosRole.Learner, PaxosRole.Proposer)));
		// colocate replicas/acceptors - don't run more than 3 replicas per group!!!
		rings.add(new RingDescription(args.replicaPartition, args.replicaId, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));
		final Node node = startPaxos(rings, args.zookeeperHost);
		if (node == null) {
			log.error("Error starting paxos");
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

		// setup the filesystem
		int ctime = (int) (System.currentTimeMillis() / 1000);
		FileSystem fs = new MemFileSystem(ctime, 0, 0);
		// create partition directories
		for (int i = 1; i <= args.nPartitions; i++) {
			fs.createDir("/" + i, 0, ctime, 0, 0);
		}
		// create files files
		for (int i = 0; i < args.nCli; i++) {
			String localPath = "/f" + i;
			String globalPath = "/" + args.replicaPartition + "/f" + i;
			fs.createFile(localPath, 0, ctime, 0, 0);
			fs.createFile(globalPath, 0, ctime, 0, 0);
			// add file content
			List<DBlock> blocks = new ArrayList<>(args.nBlocks);
			for (int j = 0; j < args.nBlocks; j++) {
				DBlock b = new DBlock(null, 0, args.blockSize, new HashSet<Byte>());
				b.setId(UUIDUtils.longToBytes(rand.nextLong()));
				blocks.add(b);
			}
			fs.setFileData(localPath, blocks);
			fs.setFileData(globalPath, blocks);
		}

		// start the replica
		try {
			InetSocketAddress addr = new InetSocketAddress(Util.getHostAddress(), args.serverPort);

			startReplica(args.nPartitions, args.replicaId, args.replicaPartition, addr.getHostString(), args.serverPort, comm, args.zookeeperHost, fs);
		} catch (TTransportException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
