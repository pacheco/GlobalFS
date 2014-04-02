package ch.inf.paxosfs.replica;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.inf.paxosfs.partitioning.SinglePartitionOracle;
import ch.inf.paxosfs.rpc.FuseOps;
import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;

public class FSMain {
	static Thread[] thriftProposers;
	static Thread fuseOpsServer;
	static Thread replica;
	static TServer thriftServer;
	static int thriftPort;
	static int workerThreads = 20;
	
	private static Options cmdOptions() {
		Options opts = new Options();
		opts.addOption("port", true, "Port to serve clients");
		opts.getOption("port").setRequired(true);
		opts.getOption("port").setType(Number.class);
		opts.addOption("zoo", true, "ZooKeeper host");
		opts.addOption("id", true, "Node id");
		opts.getOption("id").setRequired(true);
		opts.getOption("id").setType(Number.class);
		opts.addOption("partition", true, "Partition number");
		opts.getOption("partition").setRequired(true);
		opts.getOption("partition").setType(Number.class);
		opts.addOption("global", true, "Global ring number");
		opts.getOption("global").setType(Number.class);
		return opts;
	}
	
	private static void printUsage() {
		Options opts = cmdOptions();
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("FSMain", opts);
	}
	
	private static CommandLine parseArgs(String[] args) {
		Options opts = cmdOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine line;
		try {
			line = parser.parse(opts, args);
		} catch (ParseException e) {
			return null;
		}		
		return line;
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
	private static void startReplica(int id, byte partition, CommunicationService comm) throws TTransportException {
		// start replica thread
		FileSystemReplica learner = new FileSystemReplica(id, partition, comm);
		replica = new Thread(learner);
		replica.start();
		// start thrift server
		FuseOpsHandler fuseHandler = new FuseOpsHandler(id, partition, learner, new SinglePartitionOracle(partition));
		TProcessor fuseProcessor = new FuseOps.Processor<FuseOpsHandler>(fuseHandler);
		TServerTransport serverTransport = new TServerSocket(thriftPort);
		TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
		args.maxWorkerThreads(workerThreads);
		args.minWorkerThreads(workerThreads);
		TThreadPoolServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(fuseProcessor));
		server.serve();
	}
	
	public static void main(String[] rawargs) {
		String zoohost = "127.0.0.1:2181";
		
		byte partition;
		int nodeid;
		int globalRing = 0;
		int globalid; // id of the node in the global ring
		
		// argument parsing
		CommandLine args = parseArgs(rawargs);
		if (args == null) {
			printUsage();
			System.exit(1);
		}
		if (args.hasOption("zoo")) {
			zoohost = args.getOptionValue("zoo");
		}
		try {
			if (args.hasOption("global")) {
				globalRing = (Integer) args.getParsedOptionValue("global");
			}
			partition = ((Long) args.getParsedOptionValue("partition")).byteValue();
			nodeid = ((Long) args.getParsedOptionValue("id")).intValue();
			globalid = partition * 100 + nodeid;
			thriftPort = ((Long) args.getParsedOptionValue("port")).intValue();
		} catch (ParseException e) {
			e.printStackTrace();
			printUsage();
			System.exit(1);
			return;
		}

		// start paxos node
		List<RingDescription> rings = new LinkedList<RingDescription>();
		rings.add(new RingDescription(globalRing, globalid, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));
		rings.add(new RingDescription(partition, nodeid, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));
		final Node node = startPaxos(rings, zoohost);
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
			startReplica(0, partition, comm);
		} catch (TTransportException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
