package ch.inf.paxosfs.replica;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.thrift.TException;

import ch.inf.paxosfs.partitioning.PartitioningOracle;
import ch.inf.paxosfs.replica.commands.AttrCmd;
import ch.inf.paxosfs.replica.commands.Command;
import ch.inf.paxosfs.replica.commands.CommandType;
import ch.inf.paxosfs.rpc.Attr;
import ch.inf.paxosfs.rpc.DBlock;
import ch.inf.paxosfs.rpc.DirEntry;
import ch.inf.paxosfs.rpc.FSError;
import ch.inf.paxosfs.rpc.FileHandle;
import ch.inf.paxosfs.rpc.FileSystemStats;
import ch.inf.paxosfs.rpc.FuseOps;
import ch.inf.paxosfs.rpc.ReadResult;

/**
 * Implementation for the thrift server receiving client requests for fuse operations
 * @author pacheco
 *
 */
public class FuseOpsHandler implements FuseOps.Iface {
	private int partition;
	private PartitioningOracle oracle;
	private FileSystemReplica replica;
	
	public FuseOpsHandler(int partition, PartitioningOracle oracle, FileSystemReplica replica) {
		this.partition = partition;
		this.oracle = oracle;
		this.replica = replica;
	}

	@Override
	public Attr getattr(String path) throws FSError, TException {
		int dest = oracle.partitionOf(path);
		Command cmd = new Command(CommandType.ATTR.getValue(), new Random().nextLong(), (int) (System.currentTimeMillis() / 1000L), Arrays.asList(dest));
		cmd.setAttr(new AttrCmd(path));
		Attr ret = (Attr) replica.submitCommand(cmd);
		return ret;
	}

	@Override
	public String readlink(String path) throws FSError, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DirEntry> getdir(String path) throws FSError, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void mknod(String path, int mode, int rdev) throws FSError, TException {
		// TODO Auto-generated method stub
	
	}

	@Override
	public void mkdir(String path, int mode) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unlink(String path) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rmdir(String path) throws FSError, TException {
		// TODO Auto-generated method stub
	}

	@Override
	public void symlink(String target, String path) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rename(String from, String to) throws FSError, TException {
		
	}

	@Override
	public void chmod(String path, int mode) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void chown(String path, int uid, int gid) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void truncate(String path, long size) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void utime(String path, long atime, long mtime) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileSystemStats statfs() throws FSError, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileHandle open(String path, int flags) throws FSError, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void release(String path, FileHandle fh, int flags) throws FSError, TException {
		// TODO Auto-generated method stub
	}

	@Override
	public ReadResult readBlocks(String path, FileHandle fh, long offset, long bytes) throws FSError, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeBlocks(String path, FileHandle fh, long offset, List<DBlock> blocks) throws FSError, TException {
		// TODO Auto-generated method stub
		
	}
}
