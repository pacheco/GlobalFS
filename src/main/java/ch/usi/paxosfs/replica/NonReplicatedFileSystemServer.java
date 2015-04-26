package ch.usi.paxosfs.replica;

import ch.usi.da.paxos.Util;
import ch.usi.paxosfs.filesystem.DirNode;
import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.filesystem.memory.MemFileSystem;
import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.util.UnixConstants;
import fuse.FuseException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

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
	private Map<Byte, Long> instanceMap = new HashMap<>();

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
		ZookeeperReplicaManager rm = new ZookeeperReplicaManager(this.zkHost, PARTITION, REPLICA_ID, public_ip + ":" + Integer.toString(this.port));
		try {
			rm.start();
		} catch (ReplicaManagerException e) {
			e.printStackTrace();
            throw new RuntimeException(e);
		}
		
		try {
			thriftServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}	

	@Override
	public synchronized Response getattr(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		r.setGetattr(fs.get(path).getAttributes());
		return r;
	}

	@Override
	public synchronized Response readlink(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}

	@Override
	public synchronized Response getdir(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		List<DirEntry> result = new LinkedList<DirEntry>();
		DirNode dir = fs.getDir(path);
		for (String child : dir.getChildren()) {
			result.add(new DirEntry(child, 0, dir.getChild(child).typeMode()));
		}
		Response r = new Response();
		r.setGetdir(result);
		return r;
	}

	@Override
	public synchronized Response mknod(String path, int mode, int rdev, int uid, int gid, Map<Byte, Long> instanceMap)
			throws FSError, TException {
		fs.createFile(path, mode, (int) System.currentTimeMillis() / 1000, uid,
				gid);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response mkdir(String path, int mode, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError,
			TException {
		fs.createDir(path, mode, (int) System.currentTimeMillis() / 1000, uid,
				gid);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response unlink(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		fs.removeFileOrLink(path);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response rmdir(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		fs.removeDir(path);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response symlink(String target, String path, int uid, int gid, Map<Byte, Long> instanceMap)
			throws FSError, TException {
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}

	@Override
	public synchronized Response rename(String fromPath, String toPath, Map<Byte, Long> instanceMap) throws FSError,
			TException {
		Node n = fs.rename(fromPath, toPath);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response chmod(String path, int mode, Map<Byte, Long> instanceMap) throws FSError, TException {
		Node n = fs.get(path);
		n.getAttributes().setMode(mode);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response chown(String path, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError, TException {
		Node n = fs.get(path);
		n.getAttributes().setUid(uid);
		n.getAttributes().setGid(gid);
		n.getAttributes().setCtime((int) System.currentTimeMillis() / 1000);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response truncate(String path, long size, Map<Byte, Long> instanceMap) throws FSError, TException {
		fs.get(path).getAttributes()
				.setCtime((int) System.currentTimeMillis() / 1000);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response utime(String path, long atime, long mtime, Map<Byte, Long> instanceMap) throws FSError,
			TException {
		fs.get(path).getAttributes().setAtime((int) atime);
		fs.get(path).getAttributes().setMtime((int) mtime);
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response statfs(Map<Byte, Long> instanceMap) throws FSError, TException {
		Response r = new Response();
		r.setStatfs(new FileSystemStats(0, 0, 0, 0, 0, 0, 1024));
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response open(String path, int flags, Map<Byte, Long> instanceMap) throws FSError, TException {
		Node n = fs.get(path);
		if (n.isDir()) {
			throw new FSError(FuseException.EISDIR, "Is a directory");
		}

		Long id = nextId++;
		FileHandle fh = new FileHandle(id, flags, (byte) 0);
		openFiles.put(id, (FileNode) n);
		Response r = new Response();
		r.setOpen(fh);
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response readBlocks(String path, FileHandle fh, long offset,
			long bytes, Map<Byte, Long> instanceMap) throws FSError, TException {
		FileNode f = openFiles.get(fh.getId());
		if (f == null) {
			throw new FSError(FuseException.EBADF, "Bad file descriptor");
		}
		ReadResult readResult = f.getBlocks(offset, bytes);
		if (readResult == null) {
			readResult = new ReadResult(new ArrayList<DBlock>());
		}
		Response r = new Response();
		r.setReadBlocks(readResult);
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response writeBlocks(String path, FileHandle fh, long offset,
			List<DBlock> blocks, Map<Byte, Long> instanceMap) throws FSError, TException {
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
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public synchronized Response release(String path, FileHandle fh, int flags, Map<Byte, Long> instanceMap) throws FSError,
			TException {
		FileNode f = openFiles.remove(Long.valueOf(fh.getId()));
		if (f == null) {
			throw new FSError(FuseException.EBADF, "Bad file descriptor");
		}
		Response r = new Response();
		r.setInstanceMap(this.instanceMap);
		return r;
	}

	@Override
	public Response debug(Debug debug) throws FSError, TException {
		throw new TException("Not implemented!");
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
