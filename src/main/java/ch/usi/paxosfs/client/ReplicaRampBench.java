package ch.usi.paxosfs.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.Response;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.UnixConstants;

public class ReplicaRampBench {
	// bench parameters
	private static String replicaAddr;
	private static long durationMillis;
	private static int maxThreads;
	private static int globals;
	private static String prefix;
	private static Random r = new Random();
	private static int startThreads;
		
	private static class Worker implements Runnable {
		private FuseOps.Client c;
		private int id;
		private BufferedWriter out;
		private long workerDuration;
		private String localPath;
		private String globalPath;
		private Map<Byte, Long> instanceMap = new HashMap<>();
		
		public Worker(int id, long durationMillis, String path) throws IOException {
			this.id = id;
			this.workerDuration = durationMillis;
			this.localPath = path + "/f" + Integer.toString(id);
			this.globalPath = "/f" + Integer.toString(id);
			
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

		/**
		 * create files that should exist before starting the benchmark
		 */
		private void setupFiles() {
			try {
				Response r = c.mknod(localPath, 0, 0, 0, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				Response r = c.mknod(globalPath, 0, 0, 0, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
			} catch (TException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			/*
			 * Setup
			 */
			setupFiles();
			FileHandle globalFh;
			FileHandle localFh;
			try {
				Response r = c.open(globalPath, UnixConstants.O_RDWR.getValue(), instanceMap); 
				instanceMap.putAll(r.instanceMap);
				globalFh = r.open;
				r = c.open(localPath, UnixConstants.O_RDWR.getValue(), instanceMap); 
				instanceMap.putAll(r.instanceMap);
				localFh = r.open;
			} catch (TException e){
				throw new RuntimeException(e);
			}
			
			/*
			 * Actual benchmark
			 */
			long benchStart = System.currentTimeMillis();
			long benchNow = System.currentTimeMillis();
			while ((benchNow - benchStart) < workerDuration) {
				boolean global = doGlobal(); // should we submit a global command?
				long start = System.currentTimeMillis();
				try {
					List<DBlock> blocks = new ArrayList<>();
					blocks.add(new DBlock(null, 0, 1024, new HashSet<Byte>()));
					blocks.get(0).setId(UUIDUtils.longToBytes(r.nextLong()));
					if (global) {
						Response r = c.writeBlocks(globalPath, globalFh, 0, blocks, instanceMap);
						instanceMap.putAll(r.instanceMap);
					} else {
						Response r = c.writeBlocks(localPath, localFh, 0, blocks, instanceMap);
						instanceMap.putAll(r.instanceMap);
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
				Response r = c.release(globalPath, globalFh, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
				r = c.release(localPath, localFh, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
			} catch (TException e) {}
			
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
			c.mkdir(path, 0, 0, 0, new HashMap<Byte, Long>());
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
		// launch the starting number of threads
		for (int n = 0; n < startThreads; n++) {
			Thread w = new Thread(new Worker(0, durationMillis - runningMillis, path));
			workers.add(w);
			w.start();
		}
		while (runningMillis < durationMillis) {
			Thread.sleep(500); // launch more workers every 500ms
			runningMillis = System.currentTimeMillis() - benchStart;
			int target = targetRunningThreads(durationMillis, runningMillis, nThreads - startThreads);
			int toStart = Math.max(0, target + startThreads - workers.size());
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
			System.err.println("bench <replicaAddr> <partition> <duration> <startthreads> <maxthreads> <globals> <logprefix>");
			return;
		}
		replicaAddr = args[0];
		String partition = args[1];
		durationMillis = Long.parseLong(args[2])*1000;
		startThreads = Integer.parseInt(args[3]);
		maxThreads = Integer.parseInt(args[4]);
		globals = Integer.parseInt(args[5]);
		prefix = args[6];
		
		/*
		 * Start the benchmark
		 */
		writeBench(maxThreads, "/" + partition);
	}
}
