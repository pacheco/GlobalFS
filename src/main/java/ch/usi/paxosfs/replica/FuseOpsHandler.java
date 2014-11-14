package ch.usi.paxosfs.replica;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.thrift.TException;

import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.commands.AttrCmd;
import ch.usi.paxosfs.replica.commands.ChmodCmd;
import ch.usi.paxosfs.replica.commands.Command;
import ch.usi.paxosfs.replica.commands.CommandType;
import ch.usi.paxosfs.replica.commands.GetdirCmd;
import ch.usi.paxosfs.replica.commands.MkdirCmd;
import ch.usi.paxosfs.replica.commands.MknodCmd;
import ch.usi.paxosfs.replica.commands.OpenCmd;
import ch.usi.paxosfs.replica.commands.ReadBlocksCmd;
import ch.usi.paxosfs.replica.commands.ReleaseCmd;
import ch.usi.paxosfs.replica.commands.RenameCmd;
import ch.usi.paxosfs.replica.commands.RmdirCmd;
import ch.usi.paxosfs.replica.commands.TruncateCmd;
import ch.usi.paxosfs.replica.commands.UnlinkCmd;
import ch.usi.paxosfs.replica.commands.WriteBlocksCmd;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.DirEntry;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FileSystemStats;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.rpc.Response;
import ch.usi.paxosfs.util.Paths;

import com.google.common.collect.Sets;

import fuse.FuseException;

/**
 * FIXME: The methods here assume that this replica is part of the partitions of
 * a given path. A more "general" way would be to make a remote call to a
 * responsible replica when that is not the case. Clients always send request to
 * a responsible replica so this is not a problem now.
 * 
 * Implementation for the thrift server receiving client requests for fuse
 * operations
 * 
 * @author pacheco
 * 
 */
public class FuseOpsHandler implements FuseOps.Iface {
	private int id;
	private byte partition;
	private PartitioningOracle oracle;
	private FileSystemReplica replica;
	
	public Command newCommand(CommandType type, Set<Byte> involvedPartitions, Map<Byte, Long> instanceMap) {
		Command c = new Command(type.getValue(), new Random().nextLong(), (int) (System.currentTimeMillis() / 1000L), involvedPartitions);
		c.setInstanceMap(instanceMap);
		return c;
	}
	
	public FuseOpsHandler(int id, byte partition, FileSystemReplica replica, PartitioningOracle oracle) {
		this.id = id;
		this.partition = partition;
		this.oracle = oracle;
		this.replica = replica;
	}
	
	@Override
	public Response getattr(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		// can be sent to ANY partition that replicates the path - we send it to the first returned by the oracle
		Set<Byte> parts = Sets.newHashSet(Byte.valueOf(partition));
		Command cmd = newCommand(CommandType.ATTR, parts, instanceMap);
		AttrCmd attr = new AttrCmd(path, parts);
		cmd.setAttr(attr);
		Attr result = (Attr) replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		r.setGetattr(result);
		return r;
	}

	@Override
	public Response getdir(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		// can be sent to ANY partition that replicates the path - we send it to the first returned by the oracle
		Set<Byte> parts = Sets.newHashSet(Byte.valueOf(partition));
		Command cmd = newCommand(CommandType.GETDIR, parts, instanceMap);
		GetdirCmd getdir = new GetdirCmd(path, parts);
		cmd.setGetdir(getdir);
		@SuppressWarnings("unchecked")
		List<DirEntry> entries = (List<DirEntry>) replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		r.setGetdir(entries);
		return r;
	}

	@Override
	public Response mknod(String path, int mode, int rdev, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.MKNOD, involvedPartitions, instanceMap);
		MknodCmd mknod = new MknodCmd(path, mode, uid, gid, parentParts, parts); 
		cmd.setMknod(mknod);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response mkdir(String path, int mode, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.MKDIR, involvedPartitions, instanceMap);
		MkdirCmd mkdir = new MkdirCmd(path, mode, uid, gid, parentParts, parts); 
		cmd.setMkdir(mkdir);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response symlink(String target, String path, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError, TException {
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}
	
	@Override
	public Response readlink(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		// TODO Auto-generated method stub
		throw new FSError(FuseException.EOPNOTSUPP, "symlinks not supported.");
	}

	@Override
	public Response unlink(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.UNLINK, involvedPartitions, instanceMap);
		UnlinkCmd unlink = new UnlinkCmd(path, parentParts, parts); 
		cmd.setUnlink(unlink);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response rmdir(String path, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
		Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
		Command cmd = newCommand(CommandType.RMDIR, involvedPartitions, instanceMap);
		RmdirCmd rmdir = new RmdirCmd(path, parentParts, parts); 
		cmd.setRmdir(rmdir);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response rename(String from, String to, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> fromParts = oracle.partitionsOf(from);
		Set<Byte> fromParentParts = oracle.partitionsOf(Paths.dirname(from));
		Set<Byte> toParts = oracle.partitionsOf(to);
		Set<Byte> toParentParts = oracle.partitionsOf(Paths.dirname(to));
		Set<Byte> involvedPartitions = new HashSet<>();
		involvedPartitions.addAll(fromParts);
		involvedPartitions.addAll(fromParentParts);
		involvedPartitions.addAll(toParts);
		involvedPartitions.addAll(toParentParts);

		Command cmd = newCommand(CommandType.RENAME, involvedPartitions, instanceMap);
		RenameCmd rename = new RenameCmd(from, to, fromParentParts, fromParts, toParentParts, toParts);
		cmd.setRename(rename);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response chmod(String path, int mode, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.CHMOD, parts, instanceMap);
		ChmodCmd chmod = new ChmodCmd(path, mode, parts);
		cmd.setChmod(chmod);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response chown(String path, int uid, int gid, Map<Byte, Long> instanceMap) throws FSError, TException {
		// TODO: implement
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response truncate(String path, long size, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.TRUNCATE, parts, instanceMap);
		TruncateCmd truncate = new TruncateCmd(path, size, parts);
		cmd.setTruncate(truncate);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response utime(String path, long atime, long mtime, Map<Byte, Long> instanceMap) throws FSError, TException {
		// TODO implement
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response statfs(Map<Byte, Long> instanceMap) throws FSError, TException {
		// TODO: implement this if we care about statfs
		Response r = new Response(replica.getInstanceMap());
		r.setStatfs(new FileSystemStats(0, 0, 0, 0, 0, 0, 1024));
		return r;
	}

	@Override
	public Response open(String path, int flags, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.OPEN, parts, instanceMap);
		OpenCmd open = new OpenCmd(path, flags, parts);
		cmd.setOpen(open);
		FileHandle fh = (FileHandle) replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		r.setOpen(fh);
		return r;
	}

	@Override
	public Response release(String path, FileHandle fh, int flags, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.RELEASE, parts, instanceMap);
		ReleaseCmd release = new ReleaseCmd(path, fh, flags, parts);
		cmd.setRelease(release);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response readBlocks(String path, FileHandle fh, long offset, long bytes, Map<Byte, Long> instanceMap) throws FSError, TException {
		// assuming this replica replicates the file, send to own partition
		Set<Byte> parts = Sets.newHashSet(Byte.valueOf(partition));
		Command cmd = newCommand(CommandType.READ_BLOCKS, parts, instanceMap);
		ReadBlocksCmd read = new ReadBlocksCmd(path, fh, offset, bytes, parts);
		cmd.setRead(read);
		ReadResult readResult = (ReadResult) replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		r.setReadBlocks(readResult);
		return r;
	}

	@Override
	public Response writeBlocks(String path, FileHandle fh, long offset, List<DBlock> blocks, Map<Byte, Long> instanceMap) throws FSError, TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.WRITE_BLOCKS, parts, instanceMap);
		WriteBlocksCmd write = new WriteBlocksCmd(path, fh, offset, blocks, parts);
		cmd.setWrite(write);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}
}
