package ch.usi.paxosfs.replica;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import ch.usi.da.paxos.Util;
import ch.usi.paxosfs.filesystem.DirNode;
import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.filesystem.memory.MemFileSystem;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.util.UnixConstants;
import fuse.FuseException;

/**
 * Non-replicated file system server. Same thrift interface as
 * FileSystemReplica. Registers itself on zookeeper as being replica 0 of
 * partition 1 to make it easily accessible to the replicated version clients.
 * 
 * @author pacheco
 * 
 */
public class NonReplicatedFileSystemServer implements FuseOps.Iface, Runnable {
	private static final int WORKER_THREADS = 4092;
	private static final byte PARTITION = 1;
	private static final int REPLICA_ID = 0;
	private MemFileSystem fs;
	private Map<Long, FileNode> openFiles;
	private Long nextId;
	private int port;
	private String zkHost;

	public NonReplicatedFileSystemServer(int port, String zkHost) {
		this.fs = new MemFileSystem((int) (System.currentTimeMillis() / 1000), 0, 0);
		this.openFiles = new HashMap<>();
		this.nextId = 3L;
		this.port = port;
		this.zkHost = zkHost;
	}
	
	@Override
	public void run() {
		/*
		 *  start thrift server
		 */
		TProcessor fuseProcessor = new FuseOps.Processor<NonReplicatedFileSystemServer>(this);
		TServerTransport serverTransport;
		try {
			serverTransport = new TServerSocket(port);
		} catch (TTransportException e1) {
			e1.printStackTrace();
			return;
		}
		
		TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
		args.maxWorkerThreads(WORKER_THREADS);
		args.minWorkerThreads(WORKER_THREADS/2);
		args.processor(fuseProcessor);
		final TThreadPoolServer server = new TThreadPoolServer(args);
		
		Thread thriftServer = new Thread() {
			@Override
			public void run() {
				server.serve();
			};
		};
		thriftServer.start();

		/*
		 * start zookeeper replica manager
		 */
		// FIXME: EC2 HACK
		String public_ip = System.getenv("EC2");
		if(public_ip == null) {
			InetSocketAddress addr = new InetSocketAddress(Util.getHostAddress(), port);
			public_ip = addr.getHostString();
		} 
		ReplicaManager rm = new ReplicaManager(this.zkHost, PARTITION, REPLICA_ID, public_ip + ":" + Integer.toString(this.port));
		try {
			rm.start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		try {
			thriftServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}	

	@Override
	public synchronized Attr getattr(String path) throws FSError, TException {
		return fs.get(path).getAttributes();
	}

	@Override
	public synchronized String readlink(String path) throws FSError, TException {
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}

	@Override
	public synchronized List<DirEntry> getdir(String path) throws FSError, TException {
		List<DirEntry> result = new LinkedList<DirEntry>();
		DirNode dir = fs.getDir(path);
		for (String child : dir.getChildren()) {
			result.add(new DirEntry(child, 0, dir.getChild(child).typeMode()));
		}
		return result;
	}

	@Override
	public synchronized void mknod(String path, int mode, int rdev, int uid, int gid)
			throws FSError, TException {
		fs.createFile(path, mode, (int) System.currentTimeMillis() / 1000, uid,
				gid);
	}

	@Override
	public synchronized void mkdir(String path, int mode, int uid, int gid) throws FSError,
			TException {
		fs.createDir(path, mode, (int) System.currentTimeMillis() / 1000, uid,
				gid);
	}

	@Override
	public synchronized void unlink(String path) throws FSError, TException {
		fs.removeFileOrLink(path);
	}

	@Override
	public synchronized void rmdir(String path) throws FSError, TException {
		fs.removeDir(path);
	}

	@Override
	public synchronized void symlink(String target, String path, int uid, int gid)
			throws FSError, TException {
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}

	@Override
	public synchronized void rename(String fromPath, String toPath) throws FSError,
			TException {
		Node n = fs.rename(fromPath, toPath);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
	}

	@Override
	public synchronized void chmod(String path, int mode) throws FSError, TException {
		Node n = fs.get(path);
		n.getAttributes().setMode(mode);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
	}

	@Override
	public synchronized void chown(String path, int uid, int gid) throws FSError, TException {
		Node n = fs.get(path);
		n.getAttributes().setUid(uid);
		n.getAttributes().setGid(gid);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
	}

	@Override
	public synchronized void truncate(String path, long size) throws FSError, TException {
		fs.get(path).getAttributes()
				.setCtime((int) System.currentTimeMillis() / 1000);
	}

	@Override
	public synchronized void utime(String path, long atime, long mtime) throws FSError,
			TException {
		fs.get(path).getAttributes().setAtime((int) atime);
		fs.get(path).getAttributes().setMtime((int) mtime);
	}

	@Override
	public synchronized FileSystemStats statfs() throws FSError, TException {
		return new FileSystemStats(0, 0, 0, 0, 0, 0, 1024);
	}

	@Override
	public synchronized FileHandle open(String path, int flags) throws FSError, TException {
		Node n = fs.get(path);
		if (n.isDir()) {
			throw new FSError(FuseException.EISDIR, "Is a directory");
		}

		Long id = nextId++;
		FileHandle fh = new FileHandle(id, flags, (byte) 0);
		openFiles.put(id, (FileNode) n);
		return fh;
	}

	@Override
	public synchronized ReadResult readBlocks(String path, FileHandle fh, long offset,
			long bytes) throws FSError, TException {
		FileNode f = openFiles.get(fh.getId());
		if (f == null) {
			throw new FSError(FuseException.EBADF, "Bad file descriptor");
		}
		ReadResult rr = f.getBlocks(offset, bytes);
		if (rr == null) {
			rr = new ReadResult(new ArrayList<DBlock>());
		}
		return rr;
	}

	@Override
	public synchronized void writeBlocks(String path, FileHandle fh, long offset,
			List<DBlock> blocks) throws FSError, TException {
		FileNode f = openFiles.get(fh.getId());
		if (f == null) {
			throw new FSError(FuseException.EBADF, "Bad file descriptor");
		}
		if ((fh.getFlags() & UnixConstants.O_ACCMODE.getValue()) == UnixConstants.O_RDONLY
				.getValue()) {
			throw new FSError(FuseException.EBADF, "File not open for writing");
		}
		// FIXME: check for negative offset?
		if ((fh.getFlags() & UnixConstants.O_APPEND.getValue()) != 0) {
			f.appendData(blocks);
		} else {
			f.updateData(blocks, offset);
		}

		int time = (int) System.currentTimeMillis() / 1000;
		f.getAttributes().setCtime(time);
		f.getAttributes().setMtime(time);
	}

	@Override
	public synchronized void release(String path, FileHandle fh, int flags) throws FSError,
			TException {
		FileNode f = openFiles.remove(Long.valueOf(fh.getId()));
		if (f == null) {
			throw new FSError(FuseException.EBADF, "Bad file descriptor");
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("server <port> <zookeeperHost>");
			System.exit(1);
		}
		int port = Integer.parseInt(args[0]);
		String zkHost = args[1];
		NonReplicatedFileSystemServer server = new NonReplicatedFileSystemServer(port, zkHost);
		server.run();
	}
}
