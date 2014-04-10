package ch.usi.paxosfs.filesystem.memory;

import java.util.Iterator;

import ch.usi.paxosfs.filesystem.DirNode;
import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.filesystem.FileSystem;
import ch.usi.paxosfs.filesystem.LinkNode;
import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.filesystem.Permissions;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.util.Paths;
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
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw alreadyExists(absolutePath);
		}
		DirNode newDir = new MemDir(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newDir);
		return newDir;
	}

	public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		DirNode parent = this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw alreadyExists(absolutePath);
		}
		FileNode newFile = new MemFile(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newFile);
		return newFile;
	}

	public LinkNode createLink(String absolutePath, String absoluteTarget, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		DirNode parent = this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw alreadyExists(absolutePath);
		}
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
	
	@Override
	public void removeFileOrLink(String path) throws FSError {
		String parentPath = Paths.dirname(path);
		DirNode parent = getDir(parentPath);
		String name = Paths.basename(path);
		Node child = parent.getChild(name);
		if (child == null) {
			throw notFound(path);
		} else if (child.isDir()) {
			throw isDir(path);
		}
		parent.removeChild(Paths.basename(name));
	}


	@Override
	public void removeDir(String path) throws FSError {
		String parentPath = Paths.dirname(path);
		DirNode parent = getDir(parentPath);
		String name = Paths.basename(path);
		Node child = parent.getChild(name);
		if (child == null) {
			throw notFound(path);
		} else if (!child.isDir()) {
			throw notDir(path);
		}
		DirNode dir = (DirNode) child;
		if (!dir.getChildren().isEmpty()) {
			throw notEmpty(path);
		}
		parent.removeChild(Paths.basename(name));
	}

	@Override
	public DirNode getDir(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isDir()){
			throw notDir(path);
		}
		return (DirNode) n;
	}
	
	@Override
	public Node rename(String from, String to) throws FSError {
		DirNode parentFrom = getDir(Paths.dirname(from));
		String nameFrom = Paths.basename(from);
		DirNode parentTo = getDir(Paths.dirname(to));
		String nameTo = Paths.basename(to);
		
		Node nodeFrom = parentFrom.getChild(nameFrom);
		if (nodeFrom == null) {
			throw notFound(from);
		}
		Node nodeTo = parentTo.getChild(nameTo);
		if (nodeTo != null) {
			if (nodeFrom.isDir()) {
				if (!nodeTo.isDir()) { 
					throw notDir(to); 
				} else if (!((DirNode) nodeTo).getChildren().isEmpty()) {
					throw notEmpty(to);
				}
			} else { // nodeFrom is a file or link
				if (nodeTo.isDir()) {
					throw isDir(to);
				}
			}
			parentTo.removeChild(nameTo);
		}
		parentFrom.removeChild(nameFrom);
		parentTo.addChild(nameTo, nodeFrom);
		return nodeFrom;
	}
	
	private FSError notFound(String file) {
		return new FSError(FuseException.ENOENT, "No such file or directory");
	}
	
	private FSError notDir(String file) {
		return new FSError(FuseException.ENOTDIR, "Not a directory");
	}
	
	private FSError alreadyExists(String file) {
		return new FSError(FuseException.EEXIST, "File already exists");
	}


	private FSError isDir(String file) {
		return new FSError(FuseException.EISDIR, "Is a directory");
	}

	private FSError notEmpty(String path) {
		return new FSError(FuseException.ENOTEMPTY, "Not empty");
	}
}
