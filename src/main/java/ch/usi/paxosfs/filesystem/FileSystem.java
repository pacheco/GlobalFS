package ch.usi.paxosfs.filesystem;

import ch.usi.paxosfs.rpc.FSError;

public interface FileSystem {
	public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError;
	public DirNode createDir(String absolutePath, int mode, int time, int uid, int gid) throws FSError;
	public LinkNode createLink(String absolutePath, String absoluteTarget, int time, int uid, int gid) throws FSError;
	public Node get(String path) throws FSError;
	public Node getRoot();
	public void removeFileOrLink(String path) throws FSError;
	public void removeDir(String path) throws FSError;
}
