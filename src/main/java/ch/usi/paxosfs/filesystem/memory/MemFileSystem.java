package ch.usi.paxosfs.filesystem.memory;

import ch.usi.paxosfs.filesystem.*;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.util.Paths;
import fuse.FuseException;

import java.util.Iterator;


/**
 * Implements a filesystem tree in memory. This is not a thread-safe implementation.
 * @author pacheco
 *
 */
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
		MemDir parent = (MemDir) this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw errorAlreadyExists(absolutePath);
		}
		MemDir newDir = new MemDir(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newDir);
		return newDir;
	}

	public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		MemDir parent = (MemDir) this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw errorAlreadyExists(absolutePath);
		}
		FileNode newFile = new MemFile(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newFile);
		return newFile;
	}

	public LinkNode createLink(String absolutePath, String absoluteTarget, int time, int uid, int gid) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		MemDir parent = (MemDir) this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw errorAlreadyExists(absolutePath);
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
				throw errorNotFound(path);
			} else if (!current.isDir()) {
				throw errorNotDir(path);
			}
			current = ((DirNode)current).getChild(elem);
		}
		if (current == null){
			throw errorNotFound(path);
		}
		return current;
	}
	
	@Override
	public Node removeFileOrLink(String path) throws FSError {
		String parentPath = Paths.dirname(path);
		MemDir parent = (MemDir) getDir(parentPath);
		String name = Paths.basename(path);
		Node child = parent.getChild(name);
		if (child == null) {
			throw errorNotFound(path);
		} else if (child.isDir()) {
			throw errorIsDir(path);
		}
		return parent.removeChild(Paths.basename(name));
	}


	@Override
	public DirNode removeDir(String path) throws FSError {
		String parentPath = Paths.dirname(path);
		MemDir parent = (MemDir) getDir(parentPath);
		String name = Paths.basename(path);
		Node child = parent.getChild(name);
		if (child == null) {
			throw errorNotFound(path);
		} else if (!child.isDir()) {
			throw errorNotDir(path);
		}
		DirNode dir = (DirNode) child;
		if (!dir.getChildren().isEmpty()) {
			throw errorNotEmpty(path);
		}
		return (DirNode) parent.removeChild(Paths.basename(name));
	}

	@Override
	public DirNode getDir(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isDir()){
			throw errorNotDir(path);
		}
		return (DirNode) n;
	}
	
	@Override
	public FileNode getFile(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isFile()) {
			throw errorIsDir(path);
		}
		return (FileNode) n;
	}
	
	@Override
	public Node rename(String from, String to) throws FSError {
		MemDir parentFrom = (MemDir) getDir(Paths.dirname(from));
		String nameFrom = Paths.basename(from);
		MemDir parentTo = (MemDir) getDir(Paths.dirname(to));
		String nameTo = Paths.basename(to);
		
		Node nodeFrom = parentFrom.getChild(nameFrom);
		if (nodeFrom == null) {
			throw errorNotFound(from);
		}
		Node nodeTo = parentTo.getChild(nameTo);
		if (nodeTo != null) {
			if (nodeFrom.isDir()) {
				if (!nodeTo.isDir()) { 
					throw errorNotDir(to); 
				} else if (!((DirNode) nodeTo).getChildren().isEmpty()) {
					throw errorNotEmpty(to);
				}
			} else { // nodeFrom is a file or link
				if (nodeTo.isDir()) {
					throw errorIsDir(to);
				}
			}
			parentTo.removeChild(nameTo);
		}
		parentFrom.removeChild(nameFrom);
		parentTo.addChild(nameTo, nodeFrom);
		return nodeFrom;
	}
	
	@Override
	public void setFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError {
		MemFile f = (MemFile) this.getFile(absolutePath);
		f.setData(blocks);
	}

	@Override
	public void appendFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError {
		MemFile f = (MemFile) this.getFile(absolutePath);
		f.appendData(blocks);
	}

	@Override
	public void updateFileData(String absolutePath, Iterable<DBlock> blocks, long offset) throws FSError {
		MemFile f = (MemFile) this.getFile(absolutePath);
		f.updateData(blocks, offset);
	}

	@Override
	public void truncateFile(String absolutePath, long size) throws FSError {
		MemFile f = (MemFile) this.getFile(absolutePath);
		f.truncate(size);
	}
	
	private FSError errorNotFound(String file) {
		return new FSError(FuseException.ENOENT, "No such file or directory");
	}
	
	private FSError errorNotDir(String file) {
		return new FSError(FuseException.ENOTDIR, "Not a directory");
	}
	
	private FSError errorAlreadyExists(String file) {
		return new FSError(FuseException.EEXIST, "File already exists");
	}

	private FSError errorIsDir(String file) {
		return new FSError(FuseException.EISDIR, "Is a directory");
	}

	private FSError errorNotEmpty(String path) {
		return new FSError(FuseException.ENOTEMPTY, "Not empty");
	}
}
