package ch.usi.paxosfs.replica;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.thrift.TException;

import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.commands.AttrCmd;
import ch.usi.paxosfs.replica.commands.Command;
import ch.usi.paxosfs.replica.commands.CommandType;
import ch.usi.paxosfs.replica.commands.GetdirCmd;
import ch.usi.paxosfs.replica.commands.MkdirCmd;
import ch.usi.paxosfs.replica.commands.MknodCmd;
import ch.usi.paxosfs.replica.commands.RenameCmd;
import ch.usi.paxosfs.replica.commands.RmdirCmd;
import ch.usi.paxosfs.replica.commands.UnlinkCmd;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.util.Paths;

import com.google.common.collect.Sets;

import fuse.FuseException;

/**
 * Implementation for the thrift server receiving client requests for fuse operations
 * @author pacheco
 *
 */
public class FuseOpsHandler implements FuseOps.Iface {
	private int id;
	private byte partition;
	private PartitioningOracle oracle;
	private FileSystemReplica replica;
	
	public Command newCommand(CommandType type, Set<Byte> involvedPartitions) {
		return new Command(type.getValue(), new Random().nextLong(), (int) (System.currentTimeMillis() / 1000L), involvedPartitions);
	}
	
	public FuseOpsHandler(int id, byte partition, FileSystemReplica replica, PartitioningOracle oracle) {
		this.id = id;
		this.partition = partition;
		this.oracle = oracle;
		this.replica = replica;
	}
	
	@Override
	public Attr getattr(String path) throws FSError, TException {
		// can be sent to ANY partition that replicates the path - we send it to the first returned by the oracle
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.ATTR, parts);
		AttrCmd attr = new AttrCmd(path, Sets.newHashSet(parts.iterator().next()));
		cmd.setAttr(attr);
		Attr result = (Attr) replica.submitCommand(cmd);
		//System.out.println(result);
		return result;
	}

	@Override
	public List<DirEntry> getdir(String path) throws FSError, TException {
		// can be sent to ANY partition that replicates the path - we send it to the first returned by the oracle
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.GETDIR, parts);
		GetdirCmd getdir = new GetdirCmd(path, Sets.newHashSet(parts.iterator().next()));
		cmd.setGetdir(getdir);
		return (List<DirEntry>) replica.submitCommand(cmd);
	}

	@Override
	public void mknod(String path, int mode, int rdev, int uid, int gid) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.MKNOD, involvedPartitions);
		MknodCmd mknod = new MknodCmd(path, mode, uid, gid, parentParts, parts); 
		cmd.setMknod(mknod);
		replica.submitCommand(cmd);
	}

	@Override
	public void mkdir(String path, int mode, int uid, int gid) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.MKDIR, involvedPartitions);
		MkdirCmd mkdir = new MkdirCmd(path, mode, uid, gid, parentParts, parts); 
		cmd.setMkdir(mkdir);
		replica.submitCommand(cmd);
	}

	@Override
	public void symlink(String target, String path, int uid, int gid) throws FSError, TException {
		throw new FSError(FuseException.ENOTSUPP, "symlinks not supported.");
	}
	
	@Override
	public String readlink(String path) throws FSError, TException {
		// TODO Auto-generated method stub
		throw new FSError(FuseException.ENOTSUPP, "symlinks not supported.");
	}

	@Override
	public void unlink(String path) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.UNLINK, involvedPartitions);
		UnlinkCmd unlink = new UnlinkCmd(path, parentParts, parts); 
		cmd.setUnlink(unlink);
		replica.submitCommand(cmd);
	}

	@Override
	public void rmdir(String path) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.RMDIR, involvedPartitions);
		RmdirCmd rmdir = new RmdirCmd(path, parentParts, parts); 
		cmd.setRmdir(rmdir);
		replica.submitCommand(cmd);
	}

	@Override
	public void rename(String from, String to) throws FSError, TException {
		Set<Byte> fromParts = oracle.partitionsOf(from);
		Set<Byte> fromParentParts = oracle.partitionsOf(Paths.dirname(from));
		Set<Byte> toParts = oracle.partitionsOf(to);
		Set<Byte> toParentParts = oracle.partitionsOf(Paths.dirname(to));
		Set<Byte> involvedPartitions = new HashSet<>();
		involvedPartitions.addAll(fromParts);
		involvedPartitions.addAll(fromParentParts);
		involvedPartitions.addAll(toParts);
		involvedPartitions.addAll(toParentParts);

		Command cmd = newCommand(CommandType.RENAME, involvedPartitions);
		RenameCmd rename = new RenameCmd(from, to, fromParentParts, fromParts, toParentParts, toParts);
		cmd.setRename(rename);
		replica.submitCommand(cmd);
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
		// FIXME: implement this if we care about statfs
		return new FileSystemStats(0, 0, 0, 0, 0, 0, 1024);
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
