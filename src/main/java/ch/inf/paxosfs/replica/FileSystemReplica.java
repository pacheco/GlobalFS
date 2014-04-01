package ch.inf.paxosfs.replica;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.inf.paxosfs.filesystem.FileNode;
import ch.inf.paxosfs.filesystem.FileSystem;
import ch.inf.paxosfs.filesystem.Node;
import ch.inf.paxosfs.filesystem.memory.MemFileSystem;
import ch.inf.paxosfs.partitioning.PartitioningOracle;
import ch.inf.paxosfs.replica.commands.Command;
import ch.inf.paxosfs.replica.commands.CommandType;
import ch.inf.paxosfs.replica.commands.Signal;
import ch.inf.paxosfs.rpc.Attr;
import ch.inf.paxosfs.rpc.DBlock;
import ch.inf.paxosfs.rpc.FSError;
import ch.inf.paxosfs.rpc.FileHandle;
import ch.inf.paxosfs.util.Paths;
import fuse.FuseException;

public class FileSystemReplica implements Runnable {
	private CommunicationService comm;
	private ConcurrentHashMap<Long, CommandResult> pendingCommands;
	private List<Command> signalsReceived; // to keep track of signals received in advance
	private int localPartition;
	private FileSystem fs;
	private PartitioningOracle oracle;

	public FileSystemReplica(CommunicationService comm, int partition, PartitioningOracle oracle) {
		this.comm = comm;
		this.pendingCommands = new ConcurrentHashMap<Long, CommandResult>();
		this.signalsReceived = new LinkedList<Command>();
		this.localPartition = partition;
		this.oracle = oracle;
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
			// creating a dummy command so we dont have to check for null all the time
			res = new CommandResult();
		}

		try {
			// handle each command type
			Node n;
			FileNode file;
			switch (CommandType.findByValue(c.getType())) {
			case ATTR:
				System.out.println("attr");
				if (!oracle.partitionHasPath(c.getAttr().getPath(), localPartition)) {
					break;
				}
				n = fs.get(c.getAttr().getPath());
				res.setSuccess(true);
				res.setResponse(new Attr(n.getAttributes()));
				break;
			case MKNOD:
				System.out.println("mknod");
				if (!oracle.partitionHasPath(Paths.dirname(c.getMknod().getPath()), localPartition) 
						&& !oracle.partitionHasPath(c.getMknod().getPath(), localPartition)) {
					break;
				}
				fs.createFile(c.getMknod().getPath(), 
						c.getMknod().getMode(), 
						c.getReqTime(), 
						c.getMknod().getUid(), 
						c.getMknod().getGid());
				
				// send signal
				comm.signal(c.getReqId(), new Signal(localPartition, true));
				// wait signal from other partitions
				for (int otherPartition: oracle.partitionsOf(Paths.dirname(c.getAttr().getPath()))) {
					if (otherPartition != localPartition) {
						Signal sig = this.waitForSignal(c.getReqId(), otherPartition);
					}
				}
				
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case MKDIR:
				System.out.println("mkdir");
				fs.createDir(c.getMkdir().getPath(), 
						c.getMkdir().getMode(), 
						c.getReqTime(), 
						c.getMkdir().getUid(), 
						c.getMkdir().getGid());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case UNLINK:
				System.out.println("unlink");
				fs.removeFileOrLink(c.getUnlink().getPath());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case RMDIR:
				System.out.println("rmdir");
				fs.removeDir(c.getRmdir().getPath());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case SYMLINK:
				System.out.println("symlink");
				fs.createLink(c.getSymlink().getPath(), c.getSymlink().getTarget(), 
						c.getReqTime(), c.getSymlink().getUid(), c.getSymlink().getGid());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case RENAME:
				System.out.println("rename");
				// TODO
				res.setSuccess(true);
				break;
			case CHMOD:
				System.out.println("chmod");
				// TODO
				res.setSuccess(true);
				break;
			case CHOWN:
				System.out.println("chown");
				// TODO
				res.setSuccess(true);
				break;
			case TRUNCATE:
				System.out.println("truncate");
				n = fs.get(c.getTruncate().getPath());
				if (!n.isFile()) {
					throw new FSError(FuseException.EINVAL, "Not a file");
				}
				file = (FileNode) n;
				file.truncate(c.getTruncate().getSize());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case UTIME:
				System.out.println("utime");
				n = fs.get(c.getUtime().getPath());
				n.getAttributes().setAtime(c.getUtime().getAtime());
				n.getAttributes().setMtime(c.getUtime().getMtime());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case OPEN:
				System.out.println("open");
				// TODO: implement filehandle map and generate unique id here. Could be used to implement writing to a file after it has been renamed
				FileHandle fh = new FileHandle(0);
				n = fs.get(c.getOpen().getPath());
				res.setSuccess(true);
				res.setResponse(fh);
				break;
			case READ_BLOCKS:
				System.out.println("readblocks");
				n = fs.get(c.getRead().getPath());
				if (!n.isFile()) {
					throw new FSError(FuseException.EINVAL, "Not a file");
				}
				file = (FileNode) n;
				List<DBlock> blocks = file.getBlocks(c.getRead().getOffset(), c.getRead().getBytes());
				res.setSuccess(true);
				res.setResponse(blocks);
				break;
			case WRITE_BLOCKS:
				System.out.println("writeblocks");
				n = fs.get(c.getRead().getPath());
				if (!n.isFile()) {
					throw new FSError(FuseException.EINVAL, "Not a file");
				}
				file = (FileNode) n;
				file.updateData(c.getWrite().getBlocks(), c.getWrite().getOffset());
				res.setSuccess(true);
				res.setResponse(null);
				break;
			case RELEASE:
				System.out.println("release");
				// TODO: right now, this is not relevant for us. Here we would release the file handle mapping
				res.setSuccess(true);
				res.setResponse(null);
				break;
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
		pendingCommands.put(c.getReqId(), res);

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
