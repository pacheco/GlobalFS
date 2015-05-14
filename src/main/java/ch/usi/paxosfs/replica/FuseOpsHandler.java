package ch.usi.paxosfs.replica;

import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.commands.*;
import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.util.Paths;
import com.google.common.collect.Sets;
import org.apache.thrift.TException;

import java.util.*;

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
	public Response getattr(String path, Map<Byte, Long> instanceMap) throws TException {
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
	public Response getdir(String path, Map<Byte, Long> instanceMap) throws TException {
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
	public Response mknod(String path, int mode, int rdev, int uid, int gid, Map<Byte, Long> instanceMap) throws TException {
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
	public Response mkdir(String path, int mode, int uid, int gid, Map<Byte, Long> instanceMap) throws TException {
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
	public Response symlink(String target, String path, Map<Byte, Long> instanceMap) throws TException {
        Set<Byte> parts = oracle.partitionsOf(path);
        Set<Byte> parentParts = oracle.partitionsOf(Paths.dirname(path));
        Set<Byte> involvedPartitions = Sets.union(parts, parentParts);
        Command cmd = newCommand(CommandType.SYMLINK, involvedPartitions, instanceMap);
        SymlinkCmd symlink = new SymlinkCmd(target, path, parentParts, parts);
        cmd.setSymlink(symlink);
        replica.submitCommand(cmd);
        Response r = new Response(replica.getInstanceMap());
        return r;
	}
	
	@Override
	public Response readlink(String path, Map<Byte, Long> instanceMap) throws TException {
        // can be sent to ANY partition that replicates the path - we send it to the first returned by the oracle
        Set<Byte> parts = Sets.newHashSet(Byte.valueOf(partition));
        Command cmd = newCommand(CommandType.READLINK, parts, instanceMap);
        ReadlinkCmd readlink = new ReadlinkCmd(path, parts);
        cmd.setReadlink(readlink);
        String result = (String) replica.submitCommand(cmd);
        Response r = new Response(replica.getInstanceMap());
        r.setReadlink(result);
        return r;
	}

	@Override
	public Response unlink(String path, Map<Byte, Long> instanceMap) throws TException {
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
	public Response rmdir(String path, Map<Byte, Long> instanceMap) throws TException {
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
	public Response rename(String from, String to, Map<Byte, Long> instanceMap) throws TException {
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
	public Response chmod(String path, int mode, Map<Byte, Long> instanceMap) throws TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.CHMOD, parts, instanceMap);
		ChmodCmd chmod = new ChmodCmd(path, mode, parts);
		cmd.setChmod(chmod);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response chown(String path, int uid, int gid, Map<Byte, Long> instanceMap) throws TException {
		// TODO: implement
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response truncate(String path, long size, Map<Byte, Long> instanceMap) throws TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.TRUNCATE, parts, instanceMap);
		TruncateCmd truncate = new TruncateCmd(path, size, parts);
		cmd.setTruncate(truncate);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response utime(String path, long atime, long mtime, Map<Byte, Long> instanceMap) throws TException {
		// TODO implement
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response statfs(Map<Byte, Long> instanceMap) throws TException {
		// TODO: implement this if we care about statfs
		Response r = new Response(replica.getInstanceMap());
		r.setStatfs(new FileSystemStats(32*1024, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 1024));
		return r;
	}

	@Override
	public Response open(String path, int flags, Map<Byte, Long> instanceMap) throws TException {
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
	public Response release(String path, FileHandle fh, int flags, Map<Byte, Long> instanceMap) throws TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.RELEASE, parts, instanceMap);
		ReleaseCmd release = new ReleaseCmd(path, fh, flags, parts);
		cmd.setRelease(release);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response debug(Debug debug) throws TException {
        Set<Byte> partitions;
        if (debug.getType() == DebugCommands.POPULATE_FILE.getId()) {
            partitions = oracle.partitionsOf("/");
        } else {
            partitions = new HashSet<>();
            partitions.add(Byte.valueOf(Byte.parseByte(debug.getData().get("partition"))));
        }

		Command cmd = newCommand(CommandType.DEBUG, partitions, null);
		cmd.setDebug(debug);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}

	@Override
	public Response readBlocks(String path, FileHandle fh, long offset, long bytes, Map<Byte, Long> instanceMap) throws TException {
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
	public Response writeBlocks(String path, FileHandle fh, long offset, List<DBlock> blocks, Map<Byte, Long> instanceMap) throws TException {
		Set<Byte> parts = oracle.partitionsOf(path);
		Command cmd = newCommand(CommandType.WRITE_BLOCKS, parts, instanceMap);
		WriteBlocksCmd write = new WriteBlocksCmd(path, fh, offset, blocks, parts);
		cmd.setWrite(write);
		replica.submitCommand(cmd);
		Response r = new Response(replica.getInstanceMap());
		return r;
	}
}
