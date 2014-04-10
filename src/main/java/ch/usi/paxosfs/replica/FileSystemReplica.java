package ch.usi.paxosfs.replica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import ch.usi.paxosfs.filesystem.DirNode;
import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.filesystem.FileSystem;
import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.filesystem.memory.MemDir;
import ch.usi.paxosfs.filesystem.memory.MemFile;
import ch.usi.paxosfs.filesystem.memory.MemFileSystem;
import ch.usi.paxosfs.partitioning.TwoPartitionOracle;
import ch.usi.paxosfs.replica.commands.Command;
import ch.usi.paxosfs.replica.commands.CommandType;
import ch.usi.paxosfs.replica.commands.OpenCmd;
import ch.usi.paxosfs.replica.commands.ReadBlocksCmd;
import ch.usi.paxosfs.replica.commands.ReleaseCmd;
import ch.usi.paxosfs.replica.commands.RenameCmd;
import ch.usi.paxosfs.replica.commands.RenameData;
import ch.usi.paxosfs.replica.commands.Signal;
import ch.usi.paxosfs.replica.commands.WriteBlocksCmd;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.util.Paths;
import ch.usi.paxosfs.util.UnixConstants;

import com.google.common.collect.Sets;

import fuse.FuseException;
import fuse.FuseFtypeConstants;

public class FileSystemReplica implements Runnable {
	public int WORKER_THREADS = 20;
	private CommunicationService comm;
	private ConcurrentHashMap<Long, CommandResult> pendingCommands;
	private List<Command> signalsReceived; // to keep track of signals received in advance
	private int id;
	private Byte localPartition;
	private FileSystem fs;
	private Map<Long, FileNode> openFiles; // map file handles to files
	private ReplicaManager manager;
	private String zoohost;
	private Thread thriftServer;

	private FuseOpsHandler thriftHandler;
	private String host;
	private int port;

	public FileSystemReplica(int id, byte partition, CommunicationService comm, String host, int port, String zoohost) {
		this.comm = comm;
		this.pendingCommands = new ConcurrentHashMap<Long, CommandResult>();
		this.signalsReceived = new LinkedList<Command>();
		this.id = id;
		this.localPartition = Byte.valueOf(partition);
		this.openFiles = new HashMap<Long, FileNode>();
		this.zoohost = zoohost;
		this.host = host;
		this.port = port;
	}
	
	@SafeVarargs
	private static Set<Byte> unionOf(Set<Byte>... sets) {
		Set<Byte> union = new HashSet<>();
		for (Set<Byte> parts: sets){
			union.addAll(parts);
		}
		return union;
	}

	/**
	 * The Replica is constantly receiving and applying new commands.
	 */
	@Override
	public void run() {
		// start thrift server
		this.thriftHandler = new FuseOpsHandler(id, localPartition.byteValue(), this, new TwoPartitionOracle("/a", "/b"));
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
		args.minWorkerThreads(this.WORKER_THREADS);
		final TThreadPoolServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(fuseProcessor));
		
		this.thriftServer = new Thread() {
			@Override
			public void run() {
				server.serve();
			};
		};
		this.thriftServer.start();

		// start the replica
		fs = new MemFileSystem((int) (System.currentTimeMillis() / 1000), 0, 0);
		this.manager = new ReplicaManager(this.zoohost);
		try {
			this.manager.start();
			this.manager.registerReplica(this.localPartition.byteValue(), this.id, this.host + ":" + Integer.toString(this.port));
		} catch (KeeperException | InterruptedException | IOException e) {
			e.printStackTrace();
			return;
		}
		
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Command c = comm.getCommands().take();
				this.applyCommand(c);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	/**
	 * Apply a new command to the FileSystem state.
	 * FIXME: Assuming it's correct to return upon error without waiting for signals. Check if this is true!
	 * 
	 * @param c
	 *            the command to be applied
	 */
	private void applyCommand(Command c) {
		CommandResult res = pendingCommands.remove(Long.valueOf(c.getReqId()));
		if (res == null) {
			System.out.println("No pending command for " + c.getReqId());
			// creating a dummy command so we don't have to check for null all the time
			res = new CommandResult();
		}

		try {
			// handle each command type
			switch (CommandType.findByValue(c.getType())) {
			case ATTR: {
				System.out.println("attr " + c.getAttr().getPath());
				Node n = fs.get(c.getAttr().getPath());
				res.setSuccess(true);
				Attr response = new Attr(n.getAttributes());
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				
				response.setMode(response.getMode() | n.typeMode());
				res.setResponse(response);
				break;
			}
			/* -------------------------------- */
			case MKNOD: {
				System.out.println("mknod");
				
				// if the create fails here, there is no need for signals, the other partitions also fail
				fs.createFile(c.getMknod().getPath(), 
					c.getMknod().getMode(), 
					c.getReqTime(), 
					c.getMknod().getUid(), 
					c.getMknod().getGid());
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				res.setSuccess(true);
				res.setResponse(null);

				break;
			}
			/* -------------------------------- */
			case GETDIR: {
				System.out.println("getdir " + c.getGetdir().getPath());

				Node n = fs.get(c.getGetdir().getPath());
				if (!n.isDir()) {
					throw new FSError(FuseException.ENOTDIR, "Not a directory");
				}
				DirNode dir = (DirNode) n;
				
				List<DirEntry> entries = new LinkedList<DirEntry>();
				for (String child: dir.getChildren()) {
					entries.add(new DirEntry(child, 0, dir.getChild(child).typeMode()));
				}
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}

				res.setSuccess(true);
				res.setResponse(entries);
				break;
			}
			/* -------------------------------- */
			case MKDIR: {
				System.out.println("mkdir");

				fs.createDir(c.getMkdir().getPath(), 
					c.getMkdir().getMode(), 
					c.getReqTime(), 
					c.getMkdir().getUid(), 
					c.getMkdir().getGid());
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				res.setSuccess(true);
				res.setResponse(null);
				break;
			}
			/* -------------------------------- */
			case UNLINK: {
				System.out.println("unlink");

				fs.removeFileOrLink(c.getUnlink().getPath());;
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				res.setSuccess(true);
				res.setResponse(null);
				break;
			}
			/* -------------------------------- */
			case RMDIR: {
				System.out.println("rmdir");
				
				fs.removeDir(c.getRmdir().getPath());
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				res.setSuccess(true);
				res.setResponse(null);
				break;
			}
			/* -------------------------------- */
			case RENAME: {
				System.out.println("rename " + c.getRename().getFrom() + " " + c.getRename().getTo());
				RenameCmd r = c.getRename();
				// some partition in partitionTo is not in partitionFrom -> it will need data
				boolean toNeedsData = !Sets.difference(r.getPartitionTo(), r.getPartitionFrom()).isEmpty();
				// some partition in parentPartitionTo is not in partitionFrom -> it will need data
				boolean parentToNeedsData = !Sets.difference(r.getParentPartitionTo(), r.getPartitionFrom()).isEmpty();
				boolean signalWithData = toNeedsData || parentToNeedsData; 

				if (c.getInvolvedPartitions().size() == 1) {
					// single partition. just move the file
					Node n = fs.rename(r.getFrom(), r.getTo());
					n.getAttributes().setCtime(c.getReqTime());
				} else {
					/* 
					 * partitions in partitionFrom send the first signal (checks origin exists) and file data 
					 */					
					if (r.getPartitionFrom().contains(localPartition)) {
						Signal s = new Signal();
						s.setFromPartition(localPartition.byteValue());
						try {
							Node n = fs.get(r.getFrom());
							if (signalWithData) {
								if (n.isDir() && !((DirNode) n).isEmpty()) {
									// TODO: we don't support moving non-empty directories accross partitions
									throw new FSError(FuseException.ENOTEMPTY, "Moving non-empty directory accross partitions");
								}
								s.setRenameData(this.renameDataFromNode(n));
							}
							s.setSuccess(true);
							// partitionFrom signals if file exists (possibly with data)
							comm.signal(c.getReqId(), s, c.getInvolvedPartitions());
						} catch (FSError e) {
							// origin does not exist.
							// FIXME: we fail early by throwing e. Is it ok (linearizable) to not wait for signals in this case?
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
						for (Byte part: c.getInvolvedPartitions()) {
							if (part == localPartition) continue;
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
						 * partitions in To check if operation fails on its side and signals the others
						 */
						if (r.getPartitionTo().contains(localPartition)) {
							try {
								DirNode d = fs.getDir(Paths.dirname(r.getTo()));
								Node n = d.getChild(Paths.basename(r.getTo()));
								// check if the rename can proceed. It fails when:
								// - origin and destination differ in type
								// - destination is directory and is not empty
								if (n != null) {
									boolean originIsDir = (signalWithData && renameDataOriginIsDir(data)) || 
											(!signalWithData && fs.get(r.getFrom()).isDir()); 
									if (n.isDir()) {
										if (!originIsDir) {
											throw new FSError(FuseException.ENOTDIR, "Not a directory");
										} else if (!((DirNode)n).isEmpty()) {
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
								// FIXME: we fail early by throwing e. Is it ok (linearizable) to not wait for signals in this case?
								throw e;
							}
						}
						
						/*
						 * Perform the rename
						 */
						if (signalWithData) {
							Node removedNode = null; // this is used because a partition does not receive a signal from itself (so it can't use signal data)
							if (r.getPartitionFrom().contains(localPartition) || r.getParentPartitionFrom().contains(localPartition)) {
								// remove node
								DirNode d = fs.getDir(Paths.dirname(r.getFrom()));
								removedNode = d.removeChild(Paths.basename(r.getFrom()));
							}
							if (r.getPartitionTo().contains(localPartition)
									|| r.getParentPartitionTo().contains(localPartition)) { // TODO: parentTo does not need "full" file
								Node n = (data == null) ? removedNode : renameDataNewNode(data);
								DirNode d = fs.getDir(Paths.dirname(r.getTo()));
								n.getAttributes().setCtime(c.getReqTime());
								d.addChild(Paths.basename(r.getTo()), n);
							}
						} else { // no need of the data from signal
							if (r.getPartitionTo().contains(localPartition) 
									|| r.getParentPartitionTo().contains(localPartition)) {
								// to and parentTo have the origin. Just rename
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
				break;
			}
			/* -------------------------------- */
			case SYMLINK:
				System.out.println("symlink");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case CHMOD:
				System.out.println("chmod");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case CHOWN:
				System.out.println("chown");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case TRUNCATE:
				System.out.println("truncate");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case UTIME:
				System.out.println("utime");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case OPEN: {
				System.out.println("open " + c.getOpen().getPath());
				OpenCmd open = c.getOpen();
				Node n = fs.get(open.getPath());
				if (n.isDir()) {
					throw new FSError(FuseException.EISDIR, "Is a directory");
				}
				/*
				 * Flags from open(2) - already removed flags the Fuse docs say are not passed on
				 * 
				 * O_RDONLY        open for reading only
                 * O_WRONLY        open for writing only
                 * O_RDWR          open for reading and writing
                 * O_NONBLOCK      do not block on open or for data to become available
                 * O_APPEND        append on each write
                 * O_TRUNC         truncate size to 0
                 * O_SHLOCK        atomically obtain a shared lock
                 * O_EXLOCK        atomically obtain an exclusive lock
                 * O_NOFOLLOW      do not follow symlinks
                 * O_SYMLINK       allow open of symlinks
                 * O_EVTONLY       descriptor requested for event notifications only
                 * O_CLOEXEC       mark as close-on-exec
				 */
				FileHandle fh = new FileHandle(c.getReqId(), open.getFlags());
				this.openFiles.put(Long.valueOf(fh.getId()), (FileNode) n);
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				
				res.setSuccess(true);
				res.setResponse(fh);
				break;
			}
			/* -------------------------------- */
			case READ_BLOCKS: {
				System.out.println("readblocks");
				ReadBlocksCmd read = c.getRead();
				FileNode f = openFiles.get(Long.valueOf(read.getFileHandle().getId()));
				if (f == null) {
					throw new FSError(FuseException.EBADF, "Bad file descriptor");
				}
				if ((read.getFileHandle().getFlags() & UnixConstants.O_ACCMODE.getValue()) == UnixConstants.O_WRONLY.getValue()) {
					throw new FSError(FuseException.EBADF, "File not open for reading");
				}
				// FIXME: check for negative offset?
				ReadResult rr = f.getBlocks(read.getOffset(), read.getBytes());
				if (rr == null) {
					rr = new ReadResult(new ArrayList<DBlock>());
				}

				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				
				res.setSuccess(true);
				res.setResponse(rr);
				break;
			}
			/* -------------------------------- */
			case WRITE_BLOCKS: {
				System.out.println("writeblocks");
				WriteBlocksCmd write = c.getWrite();
				FileNode f = openFiles.get(Long.valueOf(write.getFileHandle().getId()));
				if (f == null) {
					throw new FSError(FuseException.EBADF, "Bad file descriptor");
				}
				if ((write.getFileHandle().getFlags() & UnixConstants.O_ACCMODE.getValue()) == UnixConstants.O_RDONLY.getValue()) {
					throw new FSError(FuseException.EBADF, "File not open for writing");
				}
				// FIXME: check for negative offset?
				if ((write.getFileHandle().getFlags() & UnixConstants.O_APPEND.getValue()) != 0) {
					f.appendData(write.getBlocks());
				} else {
					f.updateData(write.getBlocks(), write.getOffset());
				}
				
				f.getAttributes().setCtime(c.getReqTime());
				f.getAttributes().setMtime(c.getReqTime());

				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				
				res.setSuccess(true);
				res.setResponse(null);
				break;
			}
			/* -------------------------------- */
			case RELEASE: {
				System.out.println("release");
				ReleaseCmd rel = c.getRelease();
				FileNode f = openFiles.remove(Long.valueOf(rel.getFileHandle().getId()));
				if (f == null) {
					throw new FSError(FuseException.EBADF, "Bad file descriptor");
				}
				
				if (c.getInvolvedPartitions().size() > 1) {
					// wait for other signals
					for (Byte part: c.getInvolvedPartitions()) {
						if (part == localPartition) continue;
						this.waitForSignal(c.getReqId(), part.byteValue());
					}						
				}
				
				res.setSuccess(true);
				res.setResponse(null);
				break;
			}
			/* -------------------------------- */
			default:
				System.err.println("Replica: Invalid command");
				res.setSuccess(false);
				res.setError(new FSError(-1, "Invalid command"));
				break;
			}
		} catch (FSError e) {
			res.setSuccess(false);
			res.setError(e);
		}
		// signal waiting client, if any
		System.out.println("Replying to client " + c.getReqId());
		res.countDown();
	}
	
	private boolean renameDataOriginIsDir(RenameData r) {
		return (r.getMode() & FuseFtypeConstants.TYPE_DIR) != 0;
	}

	private Node renameDataNewNode(RenameData r) {
		Node n;
		if ((r.getMode() & FuseFtypeConstants.TYPE_DIR) != 0) {
			n = new MemDir(r.getMode(), r.getCtime(), r.getUid(), r.getGid());
			n.getAttributes().setAtime(r.getAtime())
				.setCtime(r.getCtime())
				.setMtime(r.getMtime());
		} else if ((r.getMode() & FuseFtypeConstants.TYPE_FILE) != 0) {
			n = new MemFile(r.getMode(), r.getCtime(), r.getUid(), r.getGid());
			n.getAttributes().setAtime(r.getAtime())
				.setCtime(r.getCtime())
				.setMtime(r.getMtime())
				.setBlocks(0);
			((MemFile) n).setData(r.getBlocks());
		} else {
			throw new RuntimeException("symlinks not supported!!!!");
		}
		return n;
	}
	
	private RenameData renameDataFromNode(Node n) {
		Attr a = n.getAttributes();
		RenameData r = new RenameData(a.getMode() | n.typeMode(), a.getRdev(), a.getUid(), a.getGid(), a.getSize(), null, a.getAtime(), a.getMtime(), a.getCtime());
		if (n.isFile()) {
			r.setBlocks(((FileNode)n).getBlocks());
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
	 * @param partitions
	 *            The partitions this command will be submitted to
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
		comm.amcast(c);

		// Wait for the command to be applied and result
		boolean timeout = false;
		try {
			timeout = !res.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			throw new FSError(-1, "Error waiting for command result");
		}

		// throw exception if the command was not successful
		if (timeout) {
			pendingCommands.remove(Long.valueOf(c.getReqId()));
			throw new FSError(-1, "Command timeout");
		}
		if (!res.isSuccess()) {
			throw res.getError();
		}

		return res.getResponse();
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
