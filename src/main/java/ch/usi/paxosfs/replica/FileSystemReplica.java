package ch.usi.paxosfs.replica;

import ch.usi.paxosfs.filesystem.*;
import ch.usi.paxosfs.filesystem.memory.MemDir;
import ch.usi.paxosfs.filesystem.memory.MemFile;
import ch.usi.paxosfs.filesystem.memory.MemFileSystem;
import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.replica.CommunicationService.CommandDecision;
import ch.usi.paxosfs.replica.commands.*;
import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.util.Paths;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.UnixConstants;
import com.google.common.collect.Sets;
import fuse.Errno;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileSystemReplica implements Runnable {
	private Logger log = Logger.getLogger(FileSystemReplica.class);
	public int WORKER_THREADS = 4096;
	private CommunicationService comm;
	private ConcurrentHashMap<Long, CommandResult> pendingCommands;
	private List<Command> signalsReceived; // to keep track of signals received
											// in advance
	private int nPartitions;
	private int id;
	private Byte localPartition;
	private FileSystem fs = null;
	private Map<Long, FileNode> openFiles; // map file handles to files
	private ZookeeperReplicaManager manager;
	private String zoohost;
	private Thread thriftServer;
	private FuseOpsHandler thriftHandler;
	private String host;
	private int port;
	private Map<Byte, Long> instanceMap;
	private long timeoutValue = 10; // timeout for operations in seconds

	public Map<Byte, Long> getInstanceMap(){
		return instanceMap;
	}

	public FileSystemReplica(int nPartitions, int id, byte partition, CommunicationService comm, String host, int port, String zoohost, FileSystem initialFS) {
		this.nPartitions = nPartitions;
		this.comm = comm;
		this.pendingCommands = new ConcurrentHashMap<Long, CommandResult>();
		this.signalsReceived = new LinkedList<Command>();
		this.id = id;
		this.localPartition = Byte.valueOf(partition);
		this.openFiles = new HashMap<Long, FileNode>();
		this.zoohost = zoohost;
		this.host = host;
		this.port = port;
		this.instanceMap = new ConcurrentHashMap<>();
		this.fs = initialFS;
		// keep track of local partition and global partition
		instanceMap.put(Byte.valueOf((byte) 0), -1L);
		instanceMap.put(Byte.valueOf(localPartition), -1L);
		log.setLevel(Level.INFO);
	}

	public FileSystemReplica(int nPartitions, int id, byte partition, CommunicationService comm, String host, int port, String zoohost) {
		this(nPartitions, id, partition, comm, host, port, zoohost, null);
	}

	/**
	 * The Replica is constantly receiving and applying new commands.
	 */
	@Override
	public void run() {
		// start thrift server
		this.thriftHandler = new FuseOpsHandler(id, localPartition.byteValue(), this, new DefaultMultiPartitionOracle(nPartitions));
		TProcessor fuseProcessor = new FuseOps.Processor<FuseOpsHandler>(this.thriftHandler);
		TServerTransport serverTransport;
		try {
			serverTransport = new TServerSocket(port);
		} catch (TTransportException e1) {
			e1.printStackTrace();
			return;
		}

		TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
		args.maxWorkerThreads(this.WORKER_THREADS);
		args.minWorkerThreads(this.WORKER_THREADS / 2);
		args.processor(fuseProcessor);
		final TThreadPoolServer server = new TThreadPoolServer(args);

		this.thriftServer = new Thread() {
			@Override
			public void run() {
				server.serve();
			};
		};
		this.thriftServer.start();

		// start the replica
		if (fs == null) {
			fs = new MemFileSystem((int) (System.currentTimeMillis() / 1000), 0, 0);
		}

		// FIXME: EC2 HACK
		String public_ip = System.getenv("EC2");
		if (public_ip == null) {
			public_ip = this.host;
		}
		this.manager = new ZookeeperReplicaManager(this.zoohost, this.localPartition.byteValue(), this.id, public_ip + ":" + Integer.toString(this.port));
		try {
			this.manager.start();
		} catch (ReplicaManagerException e) {
            log.error("Error starting replica manager", e);
			throw new RuntimeException(e);
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				CommandDecision c = comm.getCommands().take();
				// command through paxos, not readonly
				this.applyCommand(c, false);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean checkRequiredState(Map<Byte, Long> required) {
		for (Byte partition: instanceMap.keySet()){
			Long requiredVersion = required.get(partition);
			if (requiredVersion != null && requiredVersion > instanceMap.get(partition)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Apply a new command to the FileSystem state.
	 * FIXME: Check it's ok to return upon error without waiting for signals.
	 * 
	 * @param decision
	 *            the command to be applied
	 * @param isReadonly
	 *            indicates if the command is readonly or not
	 */
	private void applyCommand(CommandDecision decision, boolean isReadonly) {
		// FIXME: This loop handles updates and reads. In theory reads don't need to be serialized.
		Command c = decision.command;
		synchronized (fs) {
			CommandResult res = pendingCommands.remove(Long.valueOf(c.getReqId()));
			if (res == null) {
				log.debug("No pending command requests");
				// creating a dummy command so we don't have to check for null
				// all the time
				res = new CommandResult();
			}

			try {
				if (isReadonly) {
					// validate current replica state against the requirement of the read (to guarantee sequential consistency)
					if (!checkRequiredState(c.instanceMap)) {
						throw new FSError(Errno.EAGAIN, "Replica not uptodate");
					}
				}
				// handle each command type
				switch (CommandType.findByValue(c.getType())) {
				case DEBUG:
					applyDebug(c, res);
					break;
				case ATTR:
					applyAttr(c, res);
					break;
				case MKNOD:
					applyMknod(c, res);
					break;
				case GETDIR:
					applyGetdir(c, res);
					break;
				case MKDIR:
					applyMkdir(c, res);
					break;
				case UNLINK:
					applyUnlink(c, res);
					break;
				case RMDIR:
					applyRmdir(c, res);
					break;
				case RENAME:
					applyRename(c, res);
					break;
				case SYMLINK:
					applySymlink(c, res);
					break;
				case CHMOD:
					applyChmod(c, res);
					break;
				case CHOWN:
					applyChown(c, res);
					break;
				case TRUNCATE:
					applyTruncate(c, res);
					break;
				case UTIME:
					applyUtime(c, res);
					break;
				case OPEN:
					applyOpen(c, res);
					break;
				case READ_BLOCKS:
					applyReadBlocks(c, res);
					break;
				case WRITE_BLOCKS:
					applyWriteBlocks(c, res);
					break;
				case RELEASE:
					applyRelease(c, res);
					break;
                case READLINK:
                    applyReadlink(c, res);
                    break;
				default:
					log.error(new StrBuilder().append("Invalid command").toString());
					res.setSuccess(false);
					res.setError(new FSError(Errno.EOPNOTSUPP, "Invalid command"));
					break;
				}
			} catch (FSError e) {
				res.setSuccess(false);
				res.setError(e);
			}
			// udpate the instance map
			if (!isReadonly) {
				this.instanceMap.put(decision.partition, decision.instance);
			}
			// signal the waiting client
			res.countDown();
		}
	}

	private void applyRelease(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("release ").append(c.getRelease().getPath()).toString());
		ReleaseCmd rel = c.getRelease();
		FileNode f = openFiles.remove(Long.valueOf(rel.getFileHandle().getId()));
		if (f == null) {
            throw new FSError(FuseException.EBADF, "Bad file descriptor");
        }

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyWriteBlocks(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("write ").append(c.getWrite().getPath()).toString());
		WriteBlocksCmd write = c.getWrite();
		FileNode f = openFiles.get(Long.valueOf(write.getFileHandle().getId()));
		if (f == null) {
            throw new FSError(FuseException.EBADF, "Bad file descriptor");
        }
		if ((write.getFileHandle().getFlags() & UnixConstants.O_ACCMODE) == UnixConstants.O_RDONLY) {
            throw new FSError(FuseException.EBADF, "File not open for writing");
        }
		// FIXME: check for negative offset?
		if ((write.getFileHandle().getFlags() & UnixConstants.O_APPEND) != 0) {
            f.appendData(write.getBlocks());
        } else {
            f.updateData(write.getBlocks(), write.getOffset());
        }

		f.getAttributes().setCtime(c.getReqTime());
		f.getAttributes().setMtime(c.getReqTime());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyReadBlocks(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("read ").append(c.getRead().getPath()).toString());
		ReadBlocksCmd read = c.getRead();
		FileNode f = openFiles.get(Long.valueOf(read.getFileHandle().getId()));
		if (f == null) {
            throw new FSError(FuseException.EBADF, "Bad file descriptor");
        }
		if ((read.getFileHandle().getFlags() & UnixConstants.O_ACCMODE) == UnixConstants.O_WRONLY) {
            throw new FSError(FuseException.EBADF, "File not open for reading");
        }
		// FIXME: check for negative offset?
		ReadResult rr = f.getBlocks(read.getOffset(), read.getBytes());
		if (rr == null) {
            rr = new ReadResult(new ArrayList<DBlock>());
        }

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(rr);
	}

	private void applyOpen(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("open ").append(c.getOpen().getPath()).toString());
		OpenCmd open = c.getOpen();
		Node n = fs.get(open.getPath());
		if (n.isDir()) {
            throw new FSError(FuseException.EISDIR, "Is a directory");
        }
        /*
         * Flags from open(2) - already removed flags the Fuse docs
		 * say are not passed on
		 *
		 * O_RDONLY open for reading only
		 * O_WRONLY open for writing only
		 * O_RDWR open for reading and writing
		 * O_NONBLOCK do not block on open or for data to become available
		 * O_APPEND append on each write
		 * O_TRUNC truncate size to 0
		 * O_SHLOCK atomically obtain a shared lock
		 * O_EXLOCK atomically obtain an exclusive lock
		 * O_NOFOLLOW do not follow symlinks
		 * O_SYMLINK allow open of symlinks
		 * O_EVTONLY descriptor requested for event notifications only
		 * O_CLOEXEC mark as close-on-exec
		 */
        log.debug("Flags " + Integer.toHexString(open.getFlags()));
        FileHandle fh = new FileHandle(c.getReqId(), open.getFlags(), localPartition.byteValue());
		this.openFiles.put(Long.valueOf(fh.getId()), (FileNode) n);

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(fh);
	}

	private void applyUtime(Command c, CommandResult res) {
		// TODO: not implemented
		log.debug(new StrBuilder().append("utime ").append(c.getUtime().getPath()).toString());
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyTruncate(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("truncate ").append(c.getTruncate().getPath()).toString());
		TruncateCmd t = c.getTruncate();
		Node f = fs.get(t.getPath());
		if (f == null) {
            throw new FSError(FuseException.ENOENT, "File not found");
        } else if (!f.isFile()) {
            throw new FSError(FuseException.EINVAL, "Not a file");
        }
		((FileNode) f).truncate(t.getSize());
		f.getAttributes().setCtime(c.getReqTime());
		f.getAttributes().setMtime(c.getReqTime());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyChown(Command c, CommandResult res) {
		log.debug(new StrBuilder().append("chown ").append(c.getChown().getPath()).toString());
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyChmod(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("chmod ").append(c.getChmod().getPath()).toString());
		ChmodCmd chmod = c.getChmod();
		Node f = fs.get(chmod.getPath());
		if (f == null) {
            throw new FSError(FuseException.ENOENT, "File not found");
        }
		f.getAttributes().setMode(chmod.getMode());
		f.getAttributes().setCtime(c.getReqTime());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applySymlink(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("symlink ").append(c.getSymlink().getPath()).append(" ").append(c.getSymlink().getTarget()).toString());
        SymlinkCmd s = c.getSymlink();
        fs.createLink(s.getPath(), s.getTarget());
		res.setSuccess(true);
		res.setResponse(null);
	}

    private void applyReadlink(Command c, CommandResult res) throws FSError {
        log.debug(new StrBuilder().append("readlink ").append(c.getReadlink().getPath()).toString());

        Node n = fs.get(c.getReadlink().getPath());
        if (!n.isLink()) {
            throw new FSError(FuseException.ENOLINK, "Not a link");
        }
        String response = ((LinkNode) n).getTarget();

        if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
        res.setSuccess(true);
        res.setResponse(response);
    }

    private void applyRename(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("rename ").append(c.getRename().getFrom()).append(" ").append(c.getRename().getTo()).toString());
		RenameCmd r = c.getRename();
		// some partition in partitionTo is not in partitionFrom ->
		// it will need data
		boolean toNeedsData = !Sets.difference(r.getPartitionTo(), r.getPartitionFrom()).isEmpty();
		// some partition in parentPartitionTo is not in
		// partitionFrom -> it will need data
		boolean parentToNeedsData = !Sets.difference(r.getParentPartitionTo(), r.getPartitionFrom()).isEmpty();
		boolean signalWithData = toNeedsData || parentToNeedsData;

		if (c.getInvolvedPartitions().size() == 1) {
            // single partition. just move the file
            Node n = fs.rename(r.getFrom(), r.getTo());
            n.getAttributes().setCtime(c.getReqTime());
        } else {
            /*
             * partitions in partitionFrom send the first signal
             * (checks origin exists) and file data
             */
            if (r.getPartitionFrom().contains(localPartition)) {
                Signal s = new Signal();
                s.setFromPartition(localPartition.byteValue());
                try {
                    Node n = fs.get(r.getFrom());
                    if (signalWithData) {
                        if (n.isDir() && !((DirNode) n).isEmpty()) {
                            // TODO: moving non-empty directories across partitions not implemented
                            throw new FSError(FuseException.ENOTEMPTY, "Moving non-empty directory accross partitions");
                        }
                        s.setRenameData(this.renameDataFromNode(n));
                    }
                    s.setSuccess(true);
                    // partitionFrom signals if file exists
                    // (possibly with data)
                    comm.signal(c.getReqId(), s, c.getInvolvedPartitions());
                } catch (FSError e) {
                    // origin does not exist.
                    // FIXME: Check it's ok to return upon error without waiting for signals.
                    s.setSuccess(false);
                    s.setError(e);
                    comm.signal(c.getReqId(), s, c.getInvolvedPartitions());
                    throw e;
                }
            }

            /*
             * wait for signals
             */
            boolean allSuccess = true;
            RenameData data = null;
            FSError error = null;
            if (c.getInvolvedPartitions().size() > 1) {
                // wait for other signals
                for (Byte part : c.getInvolvedPartitions()) {
                    if (part == localPartition)
                        continue;
                    Signal s = this.waitForSignal(c.getReqId(), part.byteValue());
                    if (!s.isSuccess()) {
                        allSuccess = false;
                        error = s.getError();
                    } else if (s.isSetRenameData()) {
                        data = s.getRenameData();
                    }
                }
            }

            if (allSuccess) {
                /*
                 * partitions in To check if operation fails on its
                 * side and signals the others
                 */
                if (r.getPartitionTo().contains(localPartition)) {
                    try {
                        DirNode d = fs.getDir(Paths.dirname(r.getTo()));
                        Node n = d.getChild(Paths.basename(r.getTo()));
                        // check if the rename can proceed. It fails
                        // when:
                        // - origin and destination differ in type
                        // - destination is directory and is not
                        // empty
                        if (n != null) {
                            boolean originIsDir = (signalWithData && renameDataOriginIsDir(data))
                                    || (!signalWithData && fs.get(r.getFrom()).isDir());
                            if (n.isDir()) {
                                if (!originIsDir) {
                                    throw new FSError(FuseException.ENOTDIR, "Not a directory");
                                } else if (!((DirNode) n).isEmpty()) {
                                    throw new FSError(FuseException.ENOTEMPTY, "Directory not empty");
                                }
                            } else if (!n.isDir() && originIsDir) {
                                throw new FSError(FuseException.EISDIR, "Is a directory");
                            }
                        }
                        // signal that operation can succeed
                        comm.signal(c.getReqId(), new Signal(localPartition.byteValue(), true), c.getInvolvedPartitions());
                    } catch (FSError e) {
                        Signal s = new Signal(localPartition.byteValue(), false);
                        s.setError(e);
                        comm.signal(c.getReqId(), s, c.getInvolvedPartitions());
                        // FIXME: Check it's ok to return upon error without waiting for signals.
                        throw e;
                    }
                }

                /*
                 * Perform the rename
                 */
                if (signalWithData) {
                    // this is used because a partition does not
                    // receive a signal from itself (so it can't use
                    // signal data)
                    Node removedNode = null;
                    if (r.getPartitionFrom().contains(localPartition) || r.getParentPartitionFrom().contains(localPartition)) {
                        // remove node
                        DirNode d = fs.getDir(Paths.dirname(r.getFrom()));
                        removedNode = d.removeChild(Paths.basename(r.getFrom()));
                    }
                    // TODO: parentTo does not need "full" file
                    if (r.getPartitionTo().contains(localPartition) || r.getParentPartitionTo().contains(localPartition)) {
                        Node n = (data == null) ? removedNode : renameDataNewNode(data);
                        DirNode d = fs.getDir(Paths.dirname(r.getTo()));
                        n.getAttributes().setCtime(c.getReqTime());
                        d.addChild(Paths.basename(r.getTo()), n);
                    }
                } else { // no need of the data from signal
                    if (r.getPartitionTo().contains(localPartition) || r.getParentPartitionTo().contains(localPartition)) {
                        // to and parentTo have the origin. Just
                        // rename
                        Node n = fs.rename(r.getFrom(), r.getTo());
                        n.getAttributes().setCtime(c.getReqTime());
                    } else {
                        // remove node
                        DirNode d = fs.getDir(Paths.dirname(r.getFrom()));
                        d.removeChild(Paths.basename(r.getFrom()));
                    }
                }
            } else { // some signal received was NOT success
                if (r.getPartitionTo().contains(localPartition)) {
                    // partitionTo still needs to send its signal
                    comm.signal(c.getReqId(), new Signal(localPartition.byteValue(), false), c.getInvolvedPartitions());
                }
                throw error;
            }
        }

		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyRmdir(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("rmdir ").append(c.getRmdir().getPath()).toString());

		fs.removeDir(c.getRmdir().getPath());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyUnlink(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("unlink ").append(c.getUnlink().getPath()).toString());

		fs.removeFileOrLink(c.getUnlink().getPath());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyMkdir(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("mkdir ").append(c.getMkdir().getPath()).toString());

		fs.createDir(c.getMkdir().getPath(), c.getMkdir().getMode(), c.getReqTime(), c.getMkdir().getUid(), c.getMkdir().getGid());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyGetdir(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("getdir ").append(c.getGetdir().getPath()).toString());

		Node n = fs.get(c.getGetdir().getPath());
		if (!n.isDir()) {
            throw new FSError(FuseException.ENOTDIR, "Not a directory");
        }
		DirNode dir = (DirNode) n;

		List<DirEntry> entries = new LinkedList<DirEntry>();
		for (String child : dir.getChildren()) {
            entries.add(new DirEntry(child, 0, dir.getChild(child).typeMode()));
        }

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		res.setSuccess(true);
		res.setResponse(entries);
	}

	private void applyMknod(Command c, CommandResult res) throws FSError {
		log.debug(new StrBuilder().append("mknod ").append(c.getMknod().getPath()).toString());
		// if the create fails here, there is no need for signals,
		// the other partitions also fail
		fs.createFile(c.getMknod().getPath(), c.getMknod().getMode(), c.getReqTime(), c.getMknod().getUid(), c.getMknod().getGid());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }
		res.setSuccess(true);
		res.setResponse(null);
	}

	private void applyAttr(Command c, CommandResult res) throws FSError {
		log.debug(new StringBuilder().append("attr ").append(c.getAttr().getPath()).toString());
		Node n = fs.get(c.getAttr().getPath());
		res.setSuccess(true);
		Attr response = new Attr(n.getAttributes());

		if (c.getInvolvedPartitions().size() > 1) {
            // wait for other signals
            for (Byte part : c.getInvolvedPartitions()) {
                if (part == localPartition)
                    continue;
                //this.waitForSignal(c.getReqId(), part.byteValue());
            }
        }

		response.setMode(response.getMode() | n.typeMode());
		res.setResponse(response);
	}

	private void applyDebug(Command c, CommandResult res) throws FSError {
		log.debug(new StringBuilder().append("debugcmd ").toString());
		Debug debug = c.getDebug();
		if (debug.getType() == DebugCommands.POPULATE_FILE.getId()) {
            Random rand = new Random();
            String filename = debug.getData().get("name");
            Integer nBlocks = Integer.valueOf(debug.getData().get("nBlocks"));
            Integer blockSize = Integer.valueOf(debug.getData().get("blockSize"));
            List<DBlock> blocks = new ArrayList<>(nBlocks);
            for (int i = 0; i < nBlocks; i++) {
                DBlock b = new DBlock(null, 0, blockSize, new HashSet<Byte>());
                b.setId(UUIDUtils.longToBytes(rand.nextLong()));
                blocks.add(b);
            }
            fs.setFileData(filename, blocks);
        } else if (debug.getType() == DebugCommands.NULL.getId()) {
            // Null command does nothing
        }
		res.setSuccess(true);
	}

	private boolean renameDataOriginIsDir(RenameData r) {
		return (r.getMode() & FuseFtypeConstants.TYPE_DIR) != 0;
	}

	private Node renameDataNewNode(RenameData r) {
		Node n;
		if ((r.getMode() & FuseFtypeConstants.TYPE_DIR) != 0) {
			n = new MemDir(r.getMode(), r.getCtime(), r.getUid(), r.getGid());
			n.getAttributes().setAtime(r.getAtime()).setCtime(r.getCtime()).setMtime(r.getMtime());
		} else if ((r.getMode() & FuseFtypeConstants.TYPE_FILE) != 0) {
			n = new MemFile(r.getMode(), r.getCtime(), r.getUid(), r.getGid());
			n.getAttributes().setAtime(r.getAtime()).setCtime(r.getCtime()).setMtime(r.getMtime()).setBlocks(0);
			((MemFile) n).setData(r.getBlocks());
		} else {
			throw new RuntimeException("symlinks not supported!!!!");
		}
		return n;
	}

	private RenameData renameDataFromNode(Node n) {
		Attr a = n.getAttributes();
		RenameData r = new RenameData(a.getMode() | n.typeMode(), a.getRdev(), a.getUid(), a.getGid(), a.getSize(), null, a.getAtime(), a.getMtime(),
				a.getCtime());
		if (n.isFile()) {
			r.setBlocks(((FileNode) n).getBlocks());
		} else {
			r.setBlocksIsSet(false);
		}
		return r;
	}

	/**
	 * Will return only after the signal arrives from the other partition
	 * 
	 * @param reqId
	 * @param fromPartition
	 * @return
	 */
	private Signal waitForSignal(long reqId, byte fromPartition) {
		Command c;

		// check already received signals
		Iterator<Command> iter = signalsReceived.iterator();
		while (iter.hasNext()) {
			c = iter.next();
			if (c.getSignal().getFromPartition() == fromPartition && c.getReqId() == reqId) {
				iter.remove();
				return c.getSignal();
			}
		}
		// wait for signal to arrive
		while (true) {
			try {
				c = comm.signals.take();
				if (c.getSignal().getFromPartition() == fromPartition && c.getReqId() == reqId) {
					return c.getSignal();
				} else if (c.getSignal().getFromPartition() == this.localPartition.byteValue()) {
					// ignore signals from our own partition
				} else {
					// some other unrelated signal arrived. Store it
					signalsReceived.add(c);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Process a command requested by some client. Only returns after the
	 * command has been delivered and applied (or some error occurred).
	 * 
	 * @param c
	 *            The command to be processed
	 * @return Result of the request. If the operation produces no result (e.g.
	 *         renaming a file), it returns null.
	 * @throws FSError
	 *             If there was any error processing the request, FSError will
	 *             be raised with the error code and message.
	 */
	public Object submitCommand(Command c) throws FSError {
		// We will wait later on the command result
		CommandResult res = new CommandResult();
		pendingCommands.put(Long.valueOf(c.getReqId()), res);

		// submit the command
		if (isReadOnly(c)) {
			// we need to create a dummy CommandDecision to satisfy the applyCommand interface... the only thing used is the Command though
			this.applyCommand(new CommandDecision(c, (byte)-1, -1L), true);
		} else {
			comm.amcast(c);
		}

		// Wait for the command to be applied and for the result
		boolean timeout = false;
		try {
			timeout = !res.await(timeoutValue, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			throw new FSError(Errno.EINTR, "Error waiting for command result");
		}

		// throw exception if the command was not successful
		if (timeout) {
			pendingCommands.remove(Long.valueOf(c.getReqId()));
			throw new FSError(Errno.ETIMEDOUT, "Command timeout");
		}
		if (!res.isSuccess()) {
			throw res.getError();
		}

		return res.getResponse();
	}
	
	private boolean isReadOnly(Command c) {
		switch (CommandType.findByValue(c.getType())) {
		case ATTR:
		case GETDIR:
		case READ_BLOCKS:
        case READLINK:
			return true;
        case OPEN:
            // check for the O_RDONLY flag
            return ((c.getOpen().getFlags() & UnixConstants.O_ACCMODE) == UnixConstants.O_RDONLY);
		default:
			return false;
		}
	}

	private class CommandResult extends CountDownLatch {
		private boolean success = false;
		private Object response = null;
		private FSError error = null;

		public CommandResult() {
			super(1);
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public FSError getError() {
			return error;
		}

		public void setError(FSError error) {
			this.error = error;
		}

		public Object getResponse() {
			return response;
		}

		public void setResponse(Object response) {
			this.response = response;
		}
	}
}
