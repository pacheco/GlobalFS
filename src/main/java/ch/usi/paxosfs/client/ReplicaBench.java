package ch.usi.paxosfs.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.partitioning.DefaultStorageOracle;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.storage.FakeStorage;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.storage.StorageFactory;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.UnixConstants;

import com.google.common.collect.Sets;

public class ReplicaBench {
	private static long DURATION_SECS = 20;
	private static AtomicLong opcount = new AtomicLong(0);
	private int numberOfPartitions;
	private String zoohost;
	private HashMap<Object, Object> storages;
	private DefaultStorageOracle storageOracle;
	private DefaultMultiPartitionOracle partitionOracle;
	private int replicaId;
	private ReplicaManager rm;
	private Random r = new Random();
	private LinkedList<Long> latencyValues = new LinkedList<>();
	
	private synchronized void addLatencyValue(long lat) {
		latencyValues.add(lat);
	}
	
	private synchronized float[] getLatencyStats() {
		float[] stats = new float[3];
		stats[1] = Float.MAX_VALUE;
		for (Long l: latencyValues) {
			stats[0] += l;
			if (l < stats[1]) stats[1] = l;
			if (l > stats[2]) stats[2] = l;
		}
		stats[0] = stats[0] / latencyValues.size();
		latencyValues.clear();
		stats[0] = stats[0] / 1000000;
		stats[1] = stats[1] / 1000000;
		stats[2] = stats[2] / 1000000;
		return stats;
	}
	
	private class Worker implements Runnable {
		private FuseOps.Client c;
		int writeSize;
		public Worker(String path, int writeSize) {
			this.writeSize = writeSize;
			String replicaAddr;
			try {
				replicaAddr = rm.getReplicaAddress((byte)1, 1);
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
		}
		@Override
		public void run() {
			String path = "/1/" + Long.toString(r.nextLong());
			FileHandle fh = null;
			try {
				c.mknod(path , 644, 0, 0, 0);
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				fh = c.open(path, UnixConstants.O_WRONLY.getValue());
			} catch (TException e) {
				e.printStackTrace();
			}
			if (fh == null) { return; };
			while (true) {
				List<DBlock> blocks = new ArrayList<>(1);
				blocks.add(new DBlock(ByteBuffer.wrap(UUIDUtils.longToBytes(r.nextLong())), 0, writeSize, Sets.newHashSet(Byte.valueOf((byte)1))));
				long start = System.nanoTime();
				try {
					c.writeBlocks(path, fh, 0, blocks);
					ReplicaBench.opcount.incrementAndGet();
				} catch (TException e) {
					System.err.println("timeout");
					//e.printStackTrace();
				}
				addLatencyValue(System.nanoTime() - start);
			}
		}
	}

	public ReplicaBench(int numberOfPartitions, int replicaId, String zoohost, String storageCfgPrefix) throws IOException {
		this.numberOfPartitions = numberOfPartitions;
		this.zoohost = zoohost;
		this.storages = new HashMap<>();
		// TODO: figure out a better (more generic) way to configure the system. Right now its pretty static
		for (byte part=1; part<=numberOfPartitions; part++) {
			if (storageCfgPrefix.equals("http://fake")) {
				System.out.println("STORAGE: FAKE " + storageCfgPrefix);
				storages.put(Byte.valueOf(part), new FakeStorage());
			} else if (storageCfgPrefix.startsWith("http://")) { // FIXME: simple hack so that i can test with a single storage without config files
				System.out.println("STORAGE: " + storageCfgPrefix);
				Storage storage = StorageFactory.storageFromUrls(storageCfgPrefix);
				storages.put(Byte.valueOf(part), storage);
			} else {
				storages.put(Byte.valueOf(part), StorageFactory.storageFromConfig(FileSystems.getDefault().getPath(storageCfgPrefix + part)));
			}
		}

		this.storageOracle = new DefaultStorageOracle();
		this.partitionOracle = new DefaultMultiPartitionOracle(this.numberOfPartitions);
		this.replicaId = replicaId;
		this.rm = new ReplicaManager(zoohost);
		this.rm.start();
		
		new Thread(){
			public void run() {
				while (true) {
					float[] stats = getLatencyStats();
					System.out.println("Ops/sec: " + ReplicaBench.opcount + " | " + "Lag avg: " + stats[0] + " min: " + stats[1] + " max: " + stats[2]);
					opcount.set(0);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	private void writeBench(int nThreads, int writeSize, String path) throws InterruptedException {
		Thread[] workers = new Thread[nThreads];
		for (int i = 0; i < nThreads; i++) {
			workers[i] = new Thread(new Worker(path, writeSize));
		}
		for (int i = 0; i < nThreads; i++) {
			workers[i].start();
		}
		for (int i = 0; i < nThreads; i++) {
			workers[i].join();
		}
	}
	
	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException {
		if (args.length != 3) {
			System.out.println("bench <replica> <zoohost> <n_threads>");
			return;
		}
		ReplicaBench b = new ReplicaBench(1, Integer.parseInt(args[0]), args[1], "http://fake");
		b.writeBench(Integer.parseInt(args[2]), 1024, "1");
	}
}
