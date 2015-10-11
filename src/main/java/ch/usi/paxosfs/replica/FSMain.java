package ch.usi.paxosfs.replica;


import ch.usi.da.paxos.Util;
import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FSMain {
	private static Logger log = Logger.getLogger(FSMain.class); 
	private static Thread replica;

	/**
	 * Definition of command line options
	 * @return
	 */
	private static Options cmdlineOptions() {
		Options opts = new Options();
		Option.Builder optionBuilder;

		optionBuilder = Option.builder("n");
		optionBuilder
				.argName("N_PARTITIONS")
				.desc("number of partitions")
				.longOpt("num-part")
				.hasArg(true)
				.type(Number.class)
				.required(true);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("p");
		optionBuilder
				.argName("PARTITION")
				.desc("replica partition")
				.longOpt("rep-part")
				.hasArg(true)
				.type(Number.class)
				.required(true);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("i");
		optionBuilder
				.argName("ID")
				.desc("replica id")
				.longOpt("rep-id")
				.hasArg(true)
				.type(Number.class)
				.required(true);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("s");
		optionBuilder
				.argName("PORT")
				.desc("replica server port")
				.longOpt("rep-port")
				.hasArg(true)
				.type(Number.class)
				.required(true);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("z");
		optionBuilder.longOpt("zoo")
				.argName("ZOOADDR")
				.desc("zookeeper host:port")
				.hasArg(true)
				.type(String.class)
				.required(true);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("g");
		optionBuilder.longOpt("global-id")
				.argName("GLOBAL-ID")
				.desc("global ring id")
				.hasArg(true)
				.type(Number.class)
				.required(false);
		opts.addOption(optionBuilder.build());

		optionBuilder = Option.builder("A");
		optionBuilder.longOpt("as-global-acc")
				.desc("replica is a global acceptor")
				.hasArg(false)
				.required(false);
		opts.addOption(optionBuilder.build());

		return opts;
	}

	/**
	 * Start ring paxos node
	 * @param rings
	 * @param zoohost
	 * @return
	 */
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
	private static void startReplica(int nPartitions, int id, byte partition, String host, int port, CommunicationService comm, String zoohost) throws TTransportException {
		// start replica thread
		FileSystemReplica learner = new FileSystemReplica(nPartitions, id, partition, comm, host, port, zoohost);
		replica = new Thread(learner, "FS Replica");
		replica.start();
	}
	
	public static void main(String[] rawArgs) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		CommandLineParser parser = new DefaultParser();
		Options opts = cmdlineOptions();
		CommandLine args;

		int nPartitions;
		byte replicaPartition;
		int replicaId;
		int globalRing = 0;
		int globalId;
		int serverPort;
		String zookeeperHost;

		try {
			args = parser.parse(opts, rawArgs);
			nPartitions = ((Number) args.getParsedOptionValue("n")).intValue();
			replicaId = ((Number) args.getParsedOptionValue("i")).intValue();
			replicaPartition = ((Number) args.getParsedOptionValue("p")).byteValue();
			if (!args.hasOption("g")) {
				globalId = replicaId + replicaPartition*100;
			} else {
				globalId = ((Number) args.getParsedOptionValue("g")).intValue();
			}
			serverPort = ((Number) args.getParsedOptionValue("s")).intValue();
			zookeeperHost = (String) args.getParsedOptionValue("z");
		} catch (ParseException e) {
			formatter.printHelp("FSMain", "Start a metadata replica", opts, null, true);
			System.exit(1);
			return; // required even after exit to avoid compilation errors (null args)
		}

		List<RingDescription> rings = new LinkedList<RingDescription>();
		// whether replica is an acceptor in the global ring or not
		if (!args.hasOption("A")) {
			rings.add(new RingDescription(globalRing, globalId, Arrays.asList(PaxosRole.Learner, PaxosRole.Proposer)));
		} else {
			rings.add(new RingDescription(globalRing, globalId, Arrays.asList(PaxosRole.Learner, PaxosRole.Proposer, PaxosRole.Acceptor)));
		}

		// colocating replicas/acceptors - don't run more than 3 replicas per group!!!
		// FIXME: change to an argument option?
		rings.add(new RingDescription(replicaPartition, replicaId, Arrays.asList(PaxosRole.Acceptor, PaxosRole.Learner, PaxosRole.Proposer)));

		// start ring paxos node
		final Node node = startPaxos(rings, zookeeperHost);
		if (node == null) {
			log.error("Error starting paxos");
			System.exit(1);
		}
		
		// start communication service
		final CommunicationService comm = new CommunicationService(replicaId, replicaPartition, node);
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
			InetSocketAddress addr = new InetSocketAddress(Util.getHostAddress(), serverPort);
			startReplica(nPartitions, replicaId, replicaPartition, addr.getHostString(), serverPort, comm, zookeeperHost);
		} catch (TTransportException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
