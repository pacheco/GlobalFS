package ch.usi.paxosfs.client;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import fuse.Filesystem3;
import fuse.FuseContext;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseStatfsSetter;

public class PaxosFileSystem implements Filesystem3 {
    private static Log log = LogFactory.getLog(PaxosFileSystem.class);
	private static int MAXBLOCKSIZE = 1024;
	
	private String replicaHost;
	private int replicaPort;
	private TTransport transport;
	private FuseOps.Client client;
	
	public PaxosFileSystem(String replicaHost, int replicaPort) {
		this.replicaHost = replicaHost;
		this.replicaPort = replicaPort;
	}
	
	/** 
	 * Connect to the replica
	 * @throws TTransportException
	 */
	public void start() throws TTransportException {
		transport = new TSocket(replicaHost, replicaPort);
		transport.open();
		TProtocol protocol = new TBinaryProtocol(transport);
		client = new FuseOps.Client(protocol);
	}
	
	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		try {
			Attr attr;
			synchronized (this) {
				attr = client.getattr(path);
			}
			attrSetterFill(attr, getattrSetter);
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		try {
			synchronized (this) {
				link.append(client.readlink(path));
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		try {
			List<DirEntry> entries;
			synchronized (this) {
				entries = client.getdir(path);
			}
			for (DirEntry entry: entries) {
				dirFiller.add(entry.getName(), entry.getInode(), entry.getMode());
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		try {
			synchronized (this) {
				client.mknod(path, mode, rdev, callerUid(), callerGid());
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		try {
			synchronized (this) {
				client.mkdir(path, mode, callerUid(), callerGid());
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int unlink(String path) throws FuseException {
		try {
			synchronized (this) {
				client.unlink(path);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		try {
			synchronized (this) {
				client.rmdir(path);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		try {
			synchronized (this) {
				client.symlink(from, to, callerUid(), callerGid());
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int rename(String from, String to) throws FuseException {
		try {
			synchronized (this) {
				client.rename(from, to);
			}
		} catch (TException e) {
			e.printStackTrace();
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
				client.chmod(path, mode);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		try {
			synchronized (this) {
				client.chown(path, uid, gid);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int truncate(String path, long size) throws FuseException {
		try {
			synchronized (this) {
				client.truncate(path, size);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		try {
			synchronized (this) {
				client.utime(path, atime, mtime);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		try {
			FileSystemStats s;
			synchronized (this) {
				s = client.statfs();
			}
			statfsSetter.set(s.getBlockSize(), s.getBlocks(), s.getBlocksFree(), s.getBlocksAvail(),
					s.getFiles(), s.getFilesFree(), s.getNamelen());
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		try {
			FileHandle h;
			synchronized (this) {
				h = client.open(path, flags);
			}
			openSetter.setFh(h);
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	// TODO: fetch data from the DHT
	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		try {
			ReadResult res;
			synchronized (this) {
				res = client.readBlocks(path, (FileHandle) fh, offset, (long) buf.remaining());
			}
			// TODO: fetch data from dht here
			// write data to client using buf.put()
			buf.put("Placeholder for content".getBytes());
		} catch (TException e) {
			e.printStackTrace();
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
				DBlock b = new DBlock();
				// TODO: put data into DHT here. Set DBlock id
				b.setStartOffset(0);
				b.setEndOffset(MAXBLOCKSIZE);
				blocks.add(b);
			}
			if (buf.hasRemaining()) {
				byte[] remainingData = new byte[buf.remaining()];
				buf.get(remainingData);
				DBlock b = new DBlock();
				// TODO: put data into DHT here. Set DBlock id
				b.setStartOffset(0);
				b.setEndOffset(remainingData.length);
				blocks.add(b);
			}
			
			synchronized (this) {
				client.writeBlocks(path, (FileHandle) fh, offset, blocks);
			}
		} catch (TException e) {
			e.printStackTrace();
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
				client.release(path, (FileHandle) fh, flags);
			}
		} catch (TException e) {
			e.printStackTrace();
			throw thriftError(e);
		}
		return 0;
	}

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		// Right now, fsync does not make sense for us
		return 0;
	}
	
	public FuseException thriftError(TException cause) {
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
    
    public static void main(String[] args) {
        System.out.println("entering");

        PaxosFileSystem fs = new PaxosFileSystem(args[0], Integer.parseInt(args[1]));
        try {
        	fs.start();
        	FuseMount.mount(Arrays.copyOfRange(args, 2, args.length), fs, log);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("exiting");
        }
    }	
}
