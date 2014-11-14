package ch.usi.paxosfs.filesystem;

import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FSError;

public interface FileSystem {
	public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError;
	public DirNode createDir(String absolutePath, int mode, int time, int uid, int gid) throws FSError;
	public LinkNode createLink(String absolutePath, String absoluteTarget, int time, int uid, int gid) throws FSError;
	public Node get(String path) throws FSError;
	public Node getRoot();
	public Node removeFileOrLink(String path) throws FSError;
	public DirNode removeDir(String path) throws FSError;
	// returns the resulting node
	public Node rename(String from, String to) throws FSError;
	public DirNode getDir(String path) throws FSError;
	public FileNode getFile(String path) throws FSError;
	public void setFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError;
	public void appendFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError;
	public void updateFileData(String absolutePath, Iterable<DBlock> blocks, long offset) throws FSError;
	public void truncateFile(String absolutePath, long size) throws FSError;
}
