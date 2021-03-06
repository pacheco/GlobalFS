package ch.usi.paxosfs.client;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.ReplicaManagerException;
import ch.usi.paxosfs.replica.ZookeeperReplicaManager;
import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.storage.StorageFactory;
import ch.usi.paxosfs.storage.StorageFuture;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.Utils;
import com.google.common.net.HostAndPort;
import fuse.*;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class PaxosFileSystem implements Filesystem3 {
    private static AtomicInteger readCount = new AtomicInteger(0);
    private static AtomicInteger writeCount = new AtomicInteger(0);
    private static AtomicInteger statCount = new AtomicInteger(0);
    private static AtomicInteger opCount = new AtomicInteger(0);
    private static int MAXBLOCKSIZE = 1024 * 300;
    private static int KEYSIZE = 16;
	private Random rand = new Random();
	private int replicaId;
	private static Log log = LogFactory.getLog(PaxosFileSystem.class);
	private ZookeeperReplicaManager rm;
	private String zoohost;
	private PartitioningOracle partitionOracle;
	private Storage storage;
	private int numberOfPartitions;
	private ConcurrentLinkedQueue<FuseOps.Client>[] clients;
    private Byte closestPartition; // TODO: use an array of the partitions in order of proximity?

	// TODO: The correctness of this scheme depends on thread behaviour inside Fuse4J. Good enough for benchmarking.
	private ThreadLocal<Map<Byte, Long>> instanceMap = new ThreadLocal<Map<Byte, Long>>(){
        @Override protected Map<Byte, Long> initialValue() {
            return new ConcurrentHashMap<Byte, Long>();
        }
    };

    /**
     * Print op statistics
     */
    private static Thread statsPrinter = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (Thread.interrupted()) {
                    return;
                }
                long start = System.currentTimeMillis();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                long elapsed = System.currentTimeMillis() - start;
                double readSec = readCount.get()*1000.0 / elapsed;
                double writeSec = writeCount.get()*1000.0 / elapsed;
                double statSec = statCount.get()*1000.0 / elapsed;
                double opSec = opCount.get()*1000.0 / elapsed;
                readCount.set(0);
                writeCount.set(0);
                statCount.set(0);
                opCount.set(0);
                log.info(String.format("op/s: %f\tread/s: %f\twrite/s: %f\tstat/s: %f\t", opSec, readSec, writeSec, statSec));
            }
        }
    });

	/**
	 * client connection pool return
	 * 
	 * @param partition
	 * @return
	 */
	private FuseOps.Client getClient(byte partition) {
		FuseOps.Client c = clients[partition - 1].poll();
		if (c == null) {
			HostAndPort replicaAddr;
			try {
				replicaAddr = rm.getReplicaAddress(partition, replicaId);
			} catch (ReplicaManagerException e) {
				throw new RuntimeException(e);
			}
			TTransport transport = new TSocket(replicaAddr.getHostText(), replicaAddr.getPort());
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
	public PaxosFileSystem(int numberOfPartitions, String zoohost, String storageCfg, int replicaId, Byte closestPartition) throws Exception {
		this.numberOfPartitions = numberOfPartitions;
		this.zoohost = zoohost;
		this.storage = StorageFactory.storageFromConfig(FileSystems.getDefault().getPath(storageCfg));
		this.partitionOracle = new DefaultMultiPartitionOracle(numberOfPartitions);
		this.replicaId = replicaId;
        this.closestPartition = closestPartition;
		
		// hack around generics
		clients = (ConcurrentLinkedQueue<FuseOps.Client>[]) new ConcurrentLinkedQueue<?>[this.numberOfPartitions];
		for (byte i = 0; i < this.numberOfPartitions; i++) {
			clients[i] = new ConcurrentLinkedQueue<>();
		}
	}

    /**
     * Pick the closest partition
     * @param partitions
     * @return
     */
    private Byte choosePartition(Set<Byte> partitions) {
        if (partitions.contains(Byte.valueOf(closestPartition))) {
            return closestPartition;
        } else {
            return Utils.randomElem(rand, partitions);
        }
    }

    /**
     * Get replica addresss
     */
	public void start() throws ReplicaManagerException {
		rm = new ZookeeperReplicaManager(zoohost);
		rm.start();

	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
        statCount.incrementAndGet();
        opCount.incrementAndGet();
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Attr attr;
			Response r = client.getattr(path, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			attr = r.getattr;
			attrSetterFill(attr, getattrSetter);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.readlink(path, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			link.append(r.readlink);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.getdir(path, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			List<DirEntry> entries;
			entries = r.getdir;
			for (DirEntry entry : entries) {
				dirFiller.add(entry.getName(), entry.getInode(), entry.getMode());
			}
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.mknod(path, mode, rdev, callerUid(), callerGid(), instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.mkdir(path, mode, callerUid(), callerGid(), instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int unlink(String path) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.unlink(path, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.rmdir(path, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
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
			Response r = client.symlink(from, to, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
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
			Response r = client.rename(from, to, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
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
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.chmod(path, mode, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.chown(path, uid, gid, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int truncate(String path, long size) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.truncate(path, size, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.utime(path, atime, mtime, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
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
			Response r = client.statfs(instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			s = r.statfs;
			statfsSetter.set(s.getBlockSize(), s.getBlocks(), s.getBlocksFree(), s.getBlocksAvail(), s.getFiles(), s.getFilesFree(),
					s.getNamelen());
			returnClient(client, (byte) 1);
		} catch (FSError e) {
            returnClient(client, (byte) 1);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		}
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			FileHandle h;
			Response r = client.open(path, flags, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			h = r.open;
			openSetter.setFh(h);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		} catch (NoSuchElementException e) {
            e.printStackTrace();
            throw e;
        }
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
        readCount.incrementAndGet();
        opCount.incrementAndGet();
		FileHandle handle = (FileHandle) fh;
		Set<Byte> allPartitions = this.partitionOracle.partitionsOf(path);
		int partition = choosePartition(allPartitions).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			ReadResult res;
			Response r = client.readBlocks(path, handle, offset, (long) buf.remaining(), instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			res = r.readBlocks;
			List<StorageFuture<byte[]>> futureValues = new LinkedList<>();
			// dispatch the requests
			for (DBlock b : res.getBlocks()) {
				if (b.getId().length != 0) {
					Byte storageId = choosePartition(b.getStorage());
					futureValues.add(storage.get(storageId, b.getId()));
				}
			}
			// wait for completion
			List<byte[]> values = new ArrayList<>(futureValues.size());
			for (StorageFuture<byte[]> f: futureValues) {
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
					// zeroed block
					int size = b.getEndOffset() - b.getStartOffset();
					buf.put(new byte[size], 0, size);
				} else { 
					// block fetched from the storage
					byte[] data = valuesIter.next();
					if (data == null) {
                        log.error("Error fetching data block " + new String(b.getId()));
						throw new FSError(Errno.EREMOTEIO, "Error fetching data block!");
					}
                    try {
                        buf.put(data, b.getStartOffset(), b.getEndOffset() - b.getStartOffset());
                    } catch (IndexOutOfBoundsException e) {
                        log.error("Block offsets out of bounds of the data block:"
                                    + " block_id " + UUIDUtils.bytesToHex(b.getId())
                                    + " data_len " + data.length
                                    + " start_offset " + b.getStartOffset()
                                    + " end_offset " + b.getEndOffset());
                        e.printStackTrace();
                        throw e;
                    }
				}
			}
			
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
            throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		} catch (NoSuchElementException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
		return 0;
	}

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
        writeCount.incrementAndGet();
        opCount.incrementAndGet();
		FileHandle handle = (FileHandle) fh;
		Set<Byte> allPartitions = this.partitionOracle.partitionsOf(path); // partitions to store the data block
		int partition = choosePartition(allPartitions).intValue(); // partition to send the request
		FuseOps.Client client = getClient((byte) partition);
		try {
			List<DBlock> blocks = new LinkedList<>();
			List<StorageFuture<Boolean>> putFutures = new LinkedList<>();

			while (buf.remaining() >= MAXBLOCKSIZE) {
				byte[] data = new byte[MAXBLOCKSIZE];
				buf.get(data);
                Set<Byte> st = new HashSet<>();
                st.addAll(allPartitions);
				DBlock b = new DBlock(null, 0, MAXBLOCKSIZE, st);
				b.setId(UUIDUtils.randomBytes(rand, KEYSIZE));
				// store the block in all partitions
				for (Byte p: allPartitions) {
					putFutures.add(storage.put(p, b.getId(), data));
				}
				blocks.add(b);
			}
			if (buf.hasRemaining()) {
				byte[] remainingData = new byte[buf.remaining()];
				buf.get(remainingData);
                Set<Byte> st = new HashSet<>();
                st.addAll(allPartitions);
				DBlock b = new DBlock(null, 0, remainingData.length, st);
				b.setId(UUIDUtils.randomBytes(rand, KEYSIZE));
				// store the blocks in all partitions
				for (Byte p: allPartitions) {
					putFutures.add(storage.put(p, b.getId(), remainingData));
				}
				blocks.add(b);
			}


            // Check that puts were successful
            Iterator<StorageFuture<Boolean>> futuresIter = putFutures.iterator();
            for (DBlock b : blocks) {
                // check each partition write
                for (Byte p : allPartitions) {
                    StorageFuture<Boolean> putFuture = futuresIter.next();
                    boolean ok = false;
                    try {
                        ok = putFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    } finally {
                        if (!ok) {
                            b.getStorage().remove(Byte.valueOf(p));
                            log.error("Error writing data block " + UUIDUtils.bytesToHex(b.getId()) + " to storage " + p);
                        }
                    }
                }

                if (allPartitions.size() == 1 && b.getStorageSize() == 0) {
                    throw new FSError(Errno.EREMOTEIO, "Error storing single-partition data block: Could not write to storage " + allPartitions.iterator().next());
                } else if (allPartitions.size() >= 2 && b.getStorageSize() < 2) {
					// FIXME: hard-coded: writing to 2 partitions is enough to support 1 datacenter failure
                    throw new FSError(Errno.EREMOTEIO, "Error storing replicated data block: Required replication not achieved");
                }
            }

			Response r = client.writeBlocks(path, handle, offset, blocks, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
			throw thriftError(e);
        } catch (TException e) {
			client.getOutputProtocol().getTransport().close();
			throw thriftError(e);
		} catch (NoSuchElementException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
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
		int partition = choosePartition(this.partitionOracle.partitionsOf(path)).intValue();
		FuseOps.Client client = getClient((byte) partition);
		try {
			Response r = client.release(path, handle, flags, instanceMap.get());
			instanceMap.get().putAll(r.instanceMap);
			returnClient(client, (byte) partition);
		} catch (FSError e) {
            returnClient(client, (byte) partition);
            if (e.getErrorCode() == Errno.EAGAIN) {
                System.out.println(e.getErrorMsg());
            }
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
	
	public static void main(String[] args) throws Exception {
		// small sanity check to avoid problems later (fuse hangs on exceptions
		// sometimes)
		if (args.length < 5) {
			System.err.println("usage: PaxosFileSystem <n_partitions> <zoohost> <storage> <replica_id> <closest_partition> <MOUNT PARAMETERS>\n"
					+ "\treplica_id -> in each partition, connect to the replica with this id\n"
					+ "\tstorage -> storage config file \n"
                    + "\tclosest_partition -> id of the partition geographically closest to the client machine");
			return;
		}

		System.out.println(Arrays.toString(LogFactory.getFactory().getAttributeNames()));

       // statsPrinter.start();

        log.info(Arrays.toString(args));
		PaxosFileSystem fs = new PaxosFileSystem(Integer.parseInt(args[0]), args[1], args[2], Integer.parseInt(args[3]), Byte.parseByte(args[4]));
		try {
			fs.start();
			String[] mountArgs = Arrays.copyOfRange(args, 5, args.length);
//			log.info(Arrays.toString(mountArgs));
			FuseMount.mount(mountArgs, fs, log);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			log.debug("Exiting...");
            // statsPrinter.interrupt();
		}
	}
}
