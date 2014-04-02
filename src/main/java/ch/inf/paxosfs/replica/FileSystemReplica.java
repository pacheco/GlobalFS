package ch.inf.paxosfs.replica;

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

import ch.inf.paxosfs.filesystem.DirNode;
import ch.inf.paxosfs.filesystem.FileNode;
import ch.inf.paxosfs.filesystem.FileSystem;
import ch.inf.paxosfs.filesystem.Node;
import ch.inf.paxosfs.filesystem.memory.MemFileSystem;
import ch.inf.paxosfs.replica.commands.Command;
import ch.inf.paxosfs.replica.commands.CommandType;
import ch.inf.paxosfs.replica.commands.Signal;
import ch.inf.paxosfs.rpc.Attr;
import ch.inf.paxosfs.rpc.DirEntry;
import ch.inf.paxosfs.rpc.FSError;
import fuse.FuseException;

public class FileSystemReplica implements Runnable {
	private CommunicationService comm;
	private ConcurrentHashMap<Long, CommandResult> pendingCommands;
	private List<Command> signalsReceived; // to keep track of signals received in advance
	private int id;
	private byte localPartition;
	private FileSystem fs;
	private Map<Long, FileNode> openFiles; // map file handles to files

	public FileSystemReplica(int id, byte partition, CommunicationService comm) {
		this.comm = comm;
		this.pendingCommands = new ConcurrentHashMap<Long, CommandResult>();
		this.signalsReceived = new LinkedList<Command>();
		this.localPartition = partition;
		this.openFiles = new HashMap<Long, FileNode>();
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
		fs = new MemFileSystem((int) (System.currentTimeMillis() / 1000), 0, 0);
		
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
	 * 
	 * @param c
	 *            the command to be applied
	 */
	private void applyCommand(Command c) {
		CommandResult res = pendingCommands.remove(c.getReqId());
		if (res == null) {
			System.out.println("No pending command for " + c.getReqId());
			// creating a dummy command so we don't have to check for null all the time
			res = new CommandResult();
		}

		try {
			// handle each command type
			switch (CommandType.findByValue(c.getType())) {
			case ATTR: {
				System.out.println("attr");
				if (!c.getAttr().getPartition().contains(this.localPartition)) {
					break; // not for us
				}
				Node n = fs.get(c.getAttr().getPath());
				res.setSuccess(true);
				res.setResponse(new Attr(n.getAttributes()));
				break;
			}
			/* -------------------------------- */
			case MKNOD: {
				System.out.println("mknod");
				Set<Byte> involvedPartitions = unionOf(c.getMknod().getParentPartition(), c.getMknod().getPartition());
				if (!involvedPartitions.contains(localPartition)) {
					break; // not for us
				}
				boolean isSinglePartition = involvedPartitions.size() == 1;
				
				// if the create fails here, there is no need for signals, the other partitions also fail
				fs.createFile(c.getMknod().getPath(), 
					c.getMknod().getMode(), 
					c.getReqTime(), 
					c.getMknod().getUid(), 
					c.getMknod().getGid());
				
				if (!isSinglePartition) {
					// send signal
					involvedPartitions.remove(localPartition);
					comm.signal(c.getReqId(), new Signal(localPartition, true), involvedPartitions);
					// wait for other signals
					for (Byte part: involvedPartitions) {
						// not possible for other partitions to fail (if this succeeded), no need to check signal.success
						this.waitForSignal(c.getReqId(), part);
					}						
				}
				res.setSuccess(true);
				res.setResponse(null);

				break;
			}
			/* -------------------------------- */
			case GETDIR:
				System.out.println("getdir");
				if (!c.getGetdir().getPartition().contains(localPartition)) {
					break; // not for us
				}
				Node n = fs.get(c.getGetdir().getPath());
				if (!n.isDir()) {
					throw new FSError(FuseException.ENOTDIR, "Not a directory");
				}
				DirNode dir = (DirNode) n;
				
				List<DirEntry> entries = new LinkedList<DirEntry>();
				for (String child: dir.getChildren()) {
					entries.add(new DirEntry(child, 0, dir.typeMode()));
				}
				res.setSuccess(true);
				res.setResponse(entries);
				
			/* -------------------------------- */
			case MKDIR:
				System.out.println("mkdir");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case UNLINK:
				System.out.println("unlink");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case RMDIR:
				System.out.println("rmdir");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case SYMLINK:
				System.out.println("symlink");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case RENAME:
				System.out.println("rename");
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
			case OPEN:
				System.out.println("open");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case READ_BLOCKS:
				System.out.println("readblocks");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case WRITE_BLOCKS:
				System.out.println("writeblocks");
				res.setSuccess(true);
				res.setResponse(null);
				break;
			/* -------------------------------- */
			case RELEASE:
				System.out.println("release");
				res.setSuccess(true);
				res.setResponse(null);
				break;
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
		res.countDown();
	}

	/**
	 * Will return only after the signal arrives from the other partition
	 * 
	 * @param reqId
	 * @param fromPartition
	 * @return
	 */
	private Signal waitForSignal(long reqId, int fromPartition) {
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
				} else if (c.getSignal().getFromPartition() == this.localPartition) {
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
	public Object submitCommand(Command c, Set<Byte> partitions) throws FSError {
		// We will wait later on the command result
		CommandResult res = new CommandResult();
		pendingCommands.put(c.getReqId(), res);

		// submit the command
		comm.amcast(c, partitions);

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
			pendingCommands.remove(c.getReqId());
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
