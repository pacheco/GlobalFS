package ch.usi.paxosfs.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.storage.HttpStorageClient;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.util.UUIDUtils;
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
	private static int MAXBLOCKSIZE = 1024*65;
	private ReplicaManager rm;
	private String zoohost;
	private PartitioningOracle oracle;
	private Storage storage;
	private int numberOfPartitions;
	// Each thread has its own connections to the replicas... Fuse is multithreaded
	private ThreadLocal<FuseOps.Client[]> client = new ThreadLocal<FuseOps.Client[]>(){
		protected FuseOps.Client[] initialValue() {
			FuseOps.Client[] c = new FuseOps.Client[numberOfPartitions];
			for (byte i=1; i<=numberOfPartitions; i++) {
				String replicaAddr;
				try {
					replicaAddr = rm.getReplicaAddress(i, replicaId);
				} catch (KeeperException | InterruptedException e) {
					throw new RuntimeException(e);
				}
				String replicaHost = replicaAddr.split(":")[0];
				int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
				// TODO: should store transport to call transport.close() later
				TTransport transport = new TSocket(replicaHost, replicaPort);
				try {
					transport.open();
				} catch (TTransportException e) {
					throw new RuntimeException(e);
				}
				TProtocol protocol = new TBinaryProtocol(transport);
				log.debug(new StrBuilder().append("Connecting to replica "+ i + "," + replicaId +" at ").append(replicaAddr).toString());
				c[i-1] = new FuseOps.Client(protocol);
			}
			return c;
		};
	};
	
	
	public PaxosFileSystem(int numberOfPartitions, String zoohost, String storageHost, int replicaId) {
		this.numberOfPartitions = numberOfPartitions;
		this.zoohost = zoohost;
		this.storage = new HttpStorageClient(storageHost);
		this.oracle = new DefaultMultiPartitionOracle(numberOfPartitions);
		this.replicaId = replicaId;
	}
	
	/** 
	 * Connect to the replicas
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
		try {
			Attr attr;
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				attr = client.get()[partition].getattr(path);
			}
			attrSetterFill(attr, getattrSetter);
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				link.append(client.get()[partition].readlink(path));
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		try {
			List<DirEntry> entries;
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				entries = client.get()[partition].getdir(path);
			}
			for (DirEntry entry: entries) {
				dirFiller.add(entry.getName(), entry.getInode(), entry.getMode());
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].mknod(path, mode, rdev, callerUid(), callerGid());
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].mkdir(path, mode, callerUid(), callerGid());
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int unlink(String path) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].unlink(path);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].rmdir(path);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(from).iterator().next().intValue() - 1;
				client.get()[partition].symlink(from, to, callerUid(), callerGid());
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int rename(String from, String to) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(from).iterator().next().intValue() - 1;
				client.get()[partition].rename(from, to);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int link(String from, String to) throws FuseException {
		throw new FuseException("Hardlinks not supported").initErrno(FuseException.ENOTSUPP);
	}

	public int chmod(String path, int mode) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].chmod(path, mode);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].chown(path, uid, gid);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int truncate(String path, long size) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].truncate(path, size);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].utime(path, atime, mtime);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		try {
			FileSystemStats s;
			synchronized (this) {
				s = client.get()[0].statfs();
			}
			statfsSetter.set(s.getBlockSize(), s.getBlocks(), s.getBlocksFree(), s.getBlocksAvail(),
					s.getFiles(), s.getFilesFree(), s.getNamelen());
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		try {
			FileHandle h;
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				h = client.get()[partition].open(path, flags);
			}
			openSetter.setFh(h);
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	// TODO: fetch data from the DHT
	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		try {
			ReadResult res;
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				res = client.get()[partition].readBlocks(path, (FileHandle) fh, offset, (long) buf.remaining());
			}
			for (DBlock b: res.getBlocks()) {
				byte[] data;
				if (b.getId().length == 0) {
					data = new byte[(int) b.getId().length];
				} else {
					data = storage.get(b.getId());
				}
				if (data == null) {
					throw new FSError(-1, "Data block not found!");
				}
				//log.debug(data.length + " " + b.getStartOffset() + " " + b.getEndOffset());
				buf.put(data, b.getStartOffset(), b.getEndOffset());
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	// TODO: write data to the DHT
	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		try {
			List<DBlock> blocks = new LinkedList<>();
			byte[] data = new byte[MAXBLOCKSIZE];
			while (buf.remaining() >= MAXBLOCKSIZE) {
				buf.get(data);
				DBlock b = new DBlock(null, 0, MAXBLOCKSIZE);
				b.setId(UUIDUtils.longToBytes(rand.nextLong()));
				if (!storage.put(b.getId(), data)) {
					throw new FSError(-1, "Could not store data block!");
				}
				blocks.add(b);
			}
			if (buf.hasRemaining()) {
				byte[] remainingData = new byte[buf.remaining()];
				buf.get(remainingData);
				DBlock b = new DBlock(null, 0, remainingData.length);
				b.setId(UUIDUtils.longToBytes(rand.nextLong()));
				if (!storage.put(b.getId(), remainingData)){
					throw new FSError(-1, "Could not store data block!");
				}
				blocks.add(b);
			}
			
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].writeBlocks(path, (FileHandle) fh, offset, blocks);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int flush(String path, Object fh) throws FuseException {
		// Right now, flush does not make sense for us
		return 0;
	}

	public int release(String path, Object fh, int flags) throws FuseException {
		try {
			synchronized (this) {
				int partition = this.oracle.partitionsOf(path).iterator().next().intValue() - 1;
				client.get()[partition].release(path, (FileHandle) fh, flags);
			}
		} catch (TException e) {
			throw thriftError(e);
		}
		return 0;
	}

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		// Right now, fsync does not make sense for us
		return 0;
	}
	
	public FuseException thriftError(TException cause) {
		if (cause instanceof FSError) {
			return new FuseException(((FSError) cause).errorMsg).initErrno(((FSError) cause).errorCode);
		}
		return new FuseException("Error communicating with replica", cause);
	}
	
	public void attrSetterFill(Attr attr, FuseGetattrSetter setter) {
		setter.set(attr.getInode(), attr.getMode(), attr.getNlink(), attr.getUid(), attr.getGid(),
				attr.getRdev(), attr.getSize(), attr.getBlocks(), attr.getAtime(), attr.getMtime(), attr.getCtime());
	}

    private int callerUid() {
    	return FuseContext.get().uid;
    }
    
    private int callerGid() {
    	return FuseContext.get().gid;
    }
    
    public static void main(String[] args) throws MalformedURLException {
        // small sanity check to avoid problems later (fuse hangs on exceptions sometimes)
    	if (args.length < 4) {
    		System.err.println("usage: PaxosFileSystem <n_partitions> <zoohost> <storagehost> <replica_id> <MOUNT PARAMETERS>\n"
    				+ "\treplica_id -> in each partition, connect to the replica with this id");
    		return;
    	}
    	try {
    		new URL(args[2]);
    	} catch (MalformedURLException e) {
    		System.err.println("usage: PaxosFileSystem <n_partitions> <zoohost> <storagehost> <replica_id> <MOUNT PARAMETERS>\n"
    				+ "\treplica_id -> in each partition, connect to the replica with this id\n"
    				+ "\tstoragehost -> this has to be an http url: http://host:port");
    		return;
    	}
    	
    	System.out.println(Arrays.toString(LogFactory.getFactory().getAttributeNames()));
        
        PaxosFileSystem fs = new PaxosFileSystem(Integer.parseInt(args[0]), args[1], args[2], Integer.parseInt(args[3]));
        try {
        	fs.start();
        	String[] mountArgs = Arrays.copyOfRange(args, 4, args.length);
        	log.info(Arrays.toString(mountArgs));
        	FuseMount.mount(mountArgs, fs, log);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	log.debug("Exiting...");
        }
    }	
}
