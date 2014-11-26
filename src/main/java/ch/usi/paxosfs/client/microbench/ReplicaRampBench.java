package ch.usi.paxosfs.client.microbench;

import ch.usi.da.paxos.api.StableStorage;
import ch.usi.paxosfs.client.microbench.MicroBench;
import ch.usi.paxosfs.client.microbench.MicroBenchGetdir;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.Response;
import fuse.Errno;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class ReplicaRampBench {
	// bench parameters
	private static String replicaAddr;
	private static long durationMillis;
	private static int globals;
	private static String prefix;
	private static int startThreads;
	private static int maxThreads;
	private static byte partition;
	private static MicroBench bench;

	private static int targetRunningThreads(long durationMillis, long runningTimeMillis, long totalThreads) {
		return (int)Math.ceil((runningTimeMillis/(double)durationMillis) * totalThreads);
	}	

	private static void start() throws InterruptedException, IOException, TException {
		bench.setup(replicaAddr, partition, prefix, new Random());

		System.err.println("# Starting " + startThreads + ".." + maxThreads + " threads...");

		/*
		 * runningTime/duration * nThreads should be the number of threads running at a given time... number of clients is ramped up until
		 * it reaches nThreads at the end of the execution at a linear rate.
		 */
		List<Thread> workers = new LinkedList<>();
		long runningMillis = 0;
		long benchStart = System.currentTimeMillis();
		// launch the starting number of threads
		for (int n = 0; n < startThreads; n++) {
			workers.add(bench.startWorker(n, durationMillis - runningMillis, globals));
		}
		while (runningMillis < durationMillis) {
			Thread.sleep(500); // launch more workers every 500ms
			runningMillis = System.currentTimeMillis() - benchStart;
			int target = targetRunningThreads(durationMillis, runningMillis, maxThreads - startThreads);
			int toStart = Math.max(0, target + startThreads - workers.size());
			for (int i = 0; i < toStart; i++){
				workers.add(bench.startWorker(workers.size(), durationMillis - runningMillis, globals));
			}
		}
		// wait for started threads to finish
		for (Thread worker: workers) {
			worker.join();
		}
		System.err.println("# done");		
	}
	
	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException, TException {
		/*
		 * Get cmdline parameters
		 */
		if (args.length != 8) {
			System.err.println("bench <benchType> <replicaAddr> <partition> <duration> <startthreads> <maxthreads> <globals> <logprefix>");
			return;
		}
		String benchType = args[0];
		replicaAddr = args[1];
		partition = Byte.parseByte(args[2]);
		durationMillis = Long.parseLong(args[3])*1000;
		startThreads = Integer.parseInt(args[4]);
		maxThreads = Integer.parseInt(args[5]);
		globals = Integer.parseInt(args[6]);
		prefix = args[7];

		/*
		 * Instantiate the microbench
		 */
		try {
			Class<?> benchClass = Class.forName("ch.usi.paxosfs.client.microbench." + benchType);
			bench = (MicroBench) benchClass.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
			System.exit(1);
		}

		/*
		 * Start the benchmark
		 */
		start();
	}
}
