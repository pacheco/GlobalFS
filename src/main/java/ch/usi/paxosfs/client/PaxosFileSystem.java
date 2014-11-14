package ch.usi.paxosfs.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.partitioning.DefaultStorageOracle;
import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.partitioning.StorageOracle;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.rpc.Response;
import ch.usi.paxosfs.storage.FakeStorage;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.storage.StorageFactory;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.Utils;
import fuse.Errno;
import fuse.Filesystem3;
import fuse.FuseContext;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseStatfsSetter;

public class PaxosFileSystem implements Filesystem3 {
	private Random rand = new Random();
	private int replicaId;
	private static Log log = LogFactory.getLog(PaxosFileSystem.class);
	private static int MAXBLOCKSIZE = 1024 * 300;
	private ReplicaManager rm;
	private String zoohost;
	private PartitioningOracle partitionOracle;
	private StorageOracle storageOracle;
	private Map<Byte, Storage> storages;
	private int numberOfPartitions;
	private ConcurrentLinkedQueue<FuseOps.Client>[] clients;
	// FIXME: just using a concurrent hash map is probably not correct. The fuse client is multithreaded, so each thread should have its own instanceMap.
	// I don't remember exactly how Fuse4j uses threads so I'm not gonna worry about this now. 
	// For benchmarking purposes, this is good enough.
	private Map<Byte, Long> instanceMap = new ConcurrentHashMap<>();

	/**
	 * client connection pool return
	 * 
	 * @param partition
	 * @return
	 */
	private FuseOps.Client getClient(byte partition) {
		FuseOps.Client c = clients[partition - 1].poll();
		if (c == null) {
			String replicaAddr;
			try {
				replicaAddr = rm.getReplicaAddress(partition, replicaId);
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
			log.debug(new StrBuilder().append("Connecting to replica " + partition + "," + replicaId + " at ").append(replicaAddr)
					.toString());
			c = new FuseOps.Client(protocol);
		}
		return c;
	}

	/**
	 * client connection pool return
	 * 
	 * @param client
	 * @param partition
	 */
	private void returnClient(FuseOps.Client client, int partition) {
		clients[partition - 1].add(client);
	}

	@SuppressWarnings("unchecked")
	public PaxosFileSystem(int numberOfPartitions, String zoohost, String storageCfgPrefix, int replicaId) throws FileNotFoundException {
		this.numberOfPartitions = numberOfPartitions;
		this.zoohost = zoohost;
		this.storages = new ConcurrentHashMap<>();
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
		this.partitionOracle = new DefaultMultiPartitionOracle(numberOfPartitions);
		this.replicaId = replicaId;
		
		// haxxor around generics
		clients = (ConcurrentLinkedQueue<FuseOps.Client>[]) new ConcurrentLinkedQueue<?>[this.numberOfPartitions];
		for (byte i = 0; i < this.numberOfPartitions; i++) {
			clients[i] = new ConcurrentLinkedQueue<>();
		}
	}

	/**
	 * Connect to the replicas
	 * 
	 * @throws TTransportException
	 * @throws InterruptedException
	 * @throws KeeperException
	 * @throws IOException
	 */
	public void start() throws TTransportException, KeeperException, InterruptedException, IOException {
		rm = new ReplicaManager(zoohost);
		rm.start();

	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Attr attr;
			Response r = client.getattr(path, instanceMap);
			instanceMap.putAll(r.instanceMap);
			attr = r.getattr;
			attrSetterFill(attr, getattrSetter);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.readlink(path, instanceMap);
			instanceMap.putAll(r.instanceMap);
			link.append(r.readlink);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.getdir(path, instanceMap); 
			instanceMap.putAll(r.instanceMap);
			List<DirEntry> entries;
			entries = r.getdir;
			for (DirEntry entry : entries) {
				dirFiller.add(entry.getName(), entry.getInode(), entry.getMode());
			}
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.mknod(path, mode, rdev, callerUid(), callerGid(), instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.mkdir(path, mode, callerUid(), callerGid(), instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int unlink(String path) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.unlink(path, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.rmdir(path, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		int partition = this.partitionOracle.partitionsOf(from).iterator().next().intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.symlink(from, to, callerUid(), callerGid(), instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int rename(String from, String to) throws FuseException {
		int partition = this.partitionOracle.partitionsOf(from).iterator().next().intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.rename(from, to, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int link(String from, String to) throws FuseException {
		throw new FuseException("Hardlinks not supported").initErrno(FuseException.EOPNOTSUPP);
	}

	public int chmod(String path, int mode) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.chmod(path, mode, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.chown(path, uid, gid, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int truncate(String path, long size) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.truncate(path, size, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.utime(path, atime, mtime, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		FuseOps.Client client = getClient((byte) 1);
		try {
			FileSystemStats s;
			Response r = client.statfs(instanceMap);
			instanceMap.putAll(r.instanceMap);
			s = r.statfs;
			statfsSetter.set(s.getBlockSize(), s.getBlocks(), s.getBlocksFree(), s.getBlocksAvail(), s.getFiles(), s.getFilesFree(),
					s.getNamelen());
			returnClient(client, (byte) 1);
		} catch (FSError e) {
            returnClient(client, (byte) 1);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			FileHandle h;
			Response r = client.open(path, flags, instanceMap);
			instanceMap.putAll(r.instanceMap);
			h = r.open;
			openSetter.setFh(h);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		FileHandle handle = (FileHandle) fh;
		Set<Byte> allPartitions = this.partitionOracle.partitionsOf(path);
		int partition = Utils.randomElem(rand, allPartitions).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			ReadResult res;
			Response r = client.readBlocks(path, handle, offset, (long) buf.remaining(), instanceMap);
			instanceMap.putAll(r.instanceMap);
			res = r.readBlocks;
			List<Future<byte[]>> futureValues = new LinkedList<>();
			// dispatch the requests
			for (DBlock b : res.getBlocks()) {
				if (b.getId().length != 0) {
					// TODO: reading from a random datacenter replicating the file. Implement locality?
					Byte storageId = Utils.randomElem(rand, b.getStorage());
					futureValues.add(storages.get(storageId).get(b.getId()));
				}
			}
			// wait for completion
			List<byte[]> values = new ArrayList<byte[]>(futureValues.size());
			for (Future<byte[]> f: futureValues) {
				try {
					values.add(f.get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					throw new FSError(Errno.EREMOTEIO, "Error fetching data block!");
				}
			}
			
			// pass the values to fuse - also checking for and creating zeroed blocks (null id)
			Iterator<byte[]> valuesIter = values.iterator();
			for (DBlock b : res.getBlocks()) {
				if (b.getId().length == 0){ 
					// zero block
					int size = b.getEndOffset() - b.getStartOffset();
					buf.put(new byte[size], 0, size);
				} else { 
					// block fetched from the storage
					byte[] data = valuesIter.next();
					if (data == null) {
						throw new FSError(Errno.EREMOTEIO, "Error fetching data block!");
					}
					buf.put(data, b.getStartOffset(), b.getEndOffset() - b.getStartOffset());
				}
			}
			
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		FileHandle handle = (FileHandle) fh;
		Set<Byte> allPartitions = this.partitionOracle.partitionsOf(path);
		Set<Byte> storageIds = storageOracle.storageOf(allPartitions);
		int partition = Utils.randomElem(rand, allPartitions).intValue(); // partition to send the request
		FuseOps.Client client = getClient((byte) partition);
		try {
			List<DBlock> blocks = new LinkedList<>();
			List<Future<Boolean>> putFutures = new LinkedList<>();

			while (buf.remaining() >= MAXBLOCKSIZE) {
				byte[] data = new byte[MAXBLOCKSIZE];
				buf.get(data);
				DBlock b = new DBlock(null, 0, MAXBLOCKSIZE, storageIds);
				b.setId(UUIDUtils.longToBytes(rand.nextLong()));
				// store the block in all partitions
				for (Byte id: storageIds) {
					putFutures.add(storages.get(id).put(b.getId(), data));
				}
				blocks.add(b);
			}
			if (buf.hasRemaining()) {
				byte[] remainingData = new byte[buf.remaining()];
				buf.get(remainingData);
				DBlock b = new DBlock(null, 0, remainingData.length, allPartitions);
				b.setId(UUIDUtils.longToBytes(rand.nextLong()));
				// store the blocks in all partitions
				for (Byte id: storageIds) {
					putFutures.add(storages.get(id).put(b.getId(), remainingData));
				}
				blocks.add(b);
			}
			
			// Check all puts were successful
			for (Future<Boolean> put : putFutures) {
				try {
					if (!put.get()) {
						throw new FSError(Errno.EREMOTEIO, "Error storing data block!");
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					throw new FSError(Errno.EREMOTEIO, "Error storing data block!");
				}
			}
			Response r = client.writeBlocks(path, handle, offset, blocks, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int flush(String path, Object fh) throws FuseException {
		// All operations go through paxos. Flush does not make sense for us
		return 0;
	}

	public int release(String path, Object fh, int flags) throws FuseException {
		FileHandle handle = (FileHandle) fh;
		//int partition = (int) handle.getPartition();
		int partition = Utils.randomElem(rand, this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.release(path, handle, flags, instanceMap);
			instanceMap.putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		// Since all reads/writes go through paxos, no need for fsync
		return 0;
	}

	public FuseException thriftError(TException cause) {
		if (cause instanceof FSError) {
			return new FuseException(((FSError) cause).errorMsg).initErrno(((FSError) cause).errorCode);
		}
		return new FuseException("Error communicating with replica", cause);
	}

	public void attrSetterFill(Attr attr, FuseGetattrSetter setter) {
		setter.set(attr.getInode(), attr.getMode(), attr.getNlink(), attr.getUid(), attr.getGid(), attr.getRdev(), attr.getSize(),
				attr.getBlocks(), attr.getAtime(), attr.getMtime(), attr.getCtime());
	}

	private int callerUid() {
		return FuseContext.get().uid;
	}

	private int callerGid() {
		return FuseContext.get().gid;
	}
	
	public static void main(String[] args) throws MalformedURLException, NumberFormatException, FileNotFoundException {
		// small sanity check to avoid problems later (fuse hangs on exceptions
		// sometimes)
		if (args.length < 4) {
			System.err.println("usage: PaxosFileSystem <n_partitions> <zoohost> <storage> <replica_id> <MOUNT PARAMETERS>\n"
					+ "\treplica_id -> in each partition, connect to the replica with this id\n"
					+ "\tstorage -> cfg prefix path | http://host:port | http://fake");
			return;
		}

		System.out.println(Arrays.toString(LogFactory.getFactory().getAttributeNames()));

		PaxosFileSystem fs = new PaxosFileSystem(Integer.parseInt(args[0]), args[1], args[2], Integer.parseInt(args[3]));
		try {
			fs.start();
			String[] mountArgs = Arrays.copyOfRange(args, 4, args.length);
			log.info(Arrays.toString(mountArgs));
			FuseMount.mount(mountArgs, fs, log);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			log.debug("Exiting...");
		}
	}
}
