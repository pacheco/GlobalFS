package ch.inf.paxosfs.filesystem.memory;

import java.util.Iterator;

import ch.inf.paxosfs.filesystem.DirNode;
import ch.inf.paxosfs.filesystem.FileNode;
import ch.inf.paxosfs.filesystem.FileSystem;
import ch.inf.paxosfs.filesystem.LinkNode;
import ch.inf.paxosfs.filesystem.Node;
import ch.inf.paxosfs.filesystem.Permissions;
import ch.inf.paxosfs.rpc.FSError;
import ch.inf.paxosfs.util.Paths;
import fuse.FuseException;



public class MemFileSystem implements FileSystem {
	private Node root;
	
	public MemFileSystem(int time, int rootUid, int rootGid) {
		root = new MemDir(Permissions.ALL, time, rootUid, rootGid);
	}

	public Node getRoot() {
		return root;
	}

	public DirNode createDir(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		DirNode parent = this.getDir(parentPath);
		DirNode newDir = new MemDir(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newDir);
		return newDir;
	}

	public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		DirNode parent = this.getDir(parentPath);
		FileNode newFile = new MemFile(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newFile);
		return newFile;
	}

	public LinkNode createLink(String absolutePath, String absoluteTarget, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		DirNode parent = this.getDir(parentPath);
		LinkNode newLink = new MemLink(absoluteTarget, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newLink);
		return newLink;
	}

	public Node get(String path) throws FSError {
		path = Paths.clean(path);
		assert Paths.isAbsolute(path);
		
		Iterator<String> iter = Paths.elementIterator(path);
		iter.next(); // remove root
		Node current = getRoot();
		while (iter.hasNext()) {
			String elem = iter.next();
			if (current == null){
				throw notFound(path);
			} else if (!current.isDir()) {
				throw notDir(path);
			}
			current = ((DirNode)current).getChild(elem);
		}
		if (current == null){
			throw notFound(path);
		}
		return current;
	}
	
	
	// helper to get a directory. Throws exception if not found or if not a directory
	private DirNode getDir(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isDir()){
			throw notDir(path);
		}
		return (DirNode) n;
	}
	
	private FSError notFound(String file) {
		return new FSError(FuseException.ENOENT, file + ": No such file or directory");
	}
	
	private FSError notDir(String file) {
		return new FSError(FuseException.ENOTDIR, file + ": Not a directory");
	}
}
