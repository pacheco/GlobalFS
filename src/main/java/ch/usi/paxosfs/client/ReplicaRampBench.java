package ch.usi.paxosfs.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FuseOps;

public class ReplicaRampBench {
	// bench parameters
	private static String zoohost;
	private static int partition;
	private static int replicaId;
	private static long durationMillis;
	private static int maxThreads;
	private static int globals;
	private static String prefix;
	
	private static ReplicaManager rm;
	private static Random r = new Random();
		
	private static class Worker implements Runnable {
		private FuseOps.Client c;
		private int id;
		private BufferedWriter out;
		private long workerDuration;
		private String path;
		
		public Worker(int id, long durationMillis, String path) throws IOException {
			this.id = id;
			this.workerDuration = durationMillis;
			this.path = path;
			
			String replicaAddr;
			
			try {
				replicaAddr = rm.getReplicaAddress((byte)partition, replicaId);
				//replicaAddr = rm.getRandomReplicaAddress((byte)1);
			} catch (KeeperException | InterruptedException e) {
				throw new RuntimeException(e);
			}
			String replicaHost = replicaAddr.split(":")[0];
			int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
			TTransport transport = new TSocket(replicaHost, replicaPort);
			try {
				transport.open();
			} catch (TTransportException e) {
				throw new RuntimeException(e);
			}
			TProtocol protocol = new TBinaryProtocol(transport);
			c = new FuseOps.Client(protocol);
			out = new BufferedWriter(new FileWriter(new File(prefix + this.id)));
		}
		
		private boolean doGlobal() {
			return r.nextInt(100) < globals;
		}
		
		private String outputLine(long start, long now, boolean global){
			return start + "\t" + now + "\t" + (now - start) + "\t" + (global?1:0);
		}		
		
		@Override
		public void run() {
			long benchStart = System.currentTimeMillis();
			long benchNow = System.currentTimeMillis();
			while ((benchNow - benchStart) < workerDuration) {
				boolean global = doGlobal(); // should we submit a global command?
				long start = System.currentTimeMillis();
				try {
					if (global) {
						c.getdir("/");
					} else {
						c.getdir(path);
					}
					long end = System.currentTimeMillis();
					benchNow = end;
					try {
						out.write(outputLine(start, end, global));
						out.newLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (FSError e) {
					System.err.println("# " + e.getMessage());
				} catch (TException e) {
					System.err.println("# Error (connection closed?)");
				}
			}
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static int targetRunningThreads(long durationMillis, long runningTimeMillis, long totalThreads) {
		return (int)Math.ceil((runningTimeMillis/(double)durationMillis) * totalThreads);
	}	

	private static void writeBench(int nThreads, String path) throws InterruptedException, IOException {	
		/*
		 * Create paths used by the benchmark 
		 */
		String replicaAddr;
		try {
			replicaAddr = rm.getReplicaAddress((byte)partition, replicaId);
			//replicaAddr = rm.getRandomReplicaAddress((byte)1);
		} catch (KeeperException | InterruptedException e) {
			throw new RuntimeException(e);
		}
		String replicaHost = replicaAddr.split(":")[0];
		int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
		TTransport transport = new TSocket(replicaHost, replicaPort);
		try {
			transport.open();
		} catch (TTransportException e) {
			throw new RuntimeException(e);
		}
		TProtocol protocol = new TBinaryProtocol(transport);
		FuseOps.Client c = new FuseOps.Client(protocol);
		try {
			c.mkdir(path, 0, 0, 0);
		} catch (TException e) {
			e.printStackTrace();
		}
		transport.close();
		
		System.err.println("# Starting " + nThreads + " threads...");

		/*
		 * runningTime/duration * nThreads should be the number of threads running at a given time... number of clients is ramped up until
		 * it reaches nThreads at the end of the execution at a linear rate.
		 */
		List<Thread> workers = new LinkedList<Thread>();
		long runningMillis = 0;
		long benchStart = System.currentTimeMillis();
		// minimum of 1 worker thread
		workers.add(new Thread(new Worker(0, durationMillis - runningMillis, path)));
		workers.get(workers.size()-1).start();
		while (runningMillis < durationMillis) {
			Thread.sleep(500); // launch more workers every 500ms
			runningMillis = System.currentTimeMillis() - benchStart;
			int target = targetRunningThreads(durationMillis, runningMillis, nThreads);
			int toStart = Math.max(0, target - workers.size());
			for (int i = 0; i < toStart; i++){
				workers.add(new Thread(new Worker(workers.size(), durationMillis - runningMillis, path)));
				workers.get(workers.size()-1).start();
			}
		}
		// wait for started threads to finish
		for (Thread worker: workers) {
			worker.join();
		}
		System.err.println("# done");		
	}
	
	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException {
		/*
		 * Get cmdline parameters
		 */
		if (args.length != 7) {
			System.err.println("bench <zkhost> <partition> <replicaId> <duration> <maxthreads> <globals> <logprefix>");
			return;
		}
		zoohost = args[0];
		partition = Integer.parseInt(args[1]);
		replicaId = Integer.parseInt(args[2]);
		durationMillis = Long.parseLong(args[3])*1000;
		maxThreads = Integer.parseInt(args[4]);
		globals = Integer.parseInt(args[5]);
		prefix = args[6];
		
		/*
		 * Setup the ReplicaManager
		 */
		rm = new ReplicaManager(zoohost);
		rm.start();
		
		/*
		 * Start the benchmark
		 */
		writeBench(maxThreads, "/" + Integer.toString(partition));
	}
}
