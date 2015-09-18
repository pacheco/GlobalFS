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
			throw FSErrors.alreadyExists(absolutePath);
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
			throw FSErrors.alreadyExists(absolutePath);
		}
		FileNode newFile = new MemFile(mode, time, uid, gid);
		parent.addChild(Paths.basename(absolutePath), newFile);
		return newFile;
	}

	public LinkNode createLink(String absolutePath, String absoluteTarget) throws FSError {
		String parentPath = Paths.dirname(absolutePath);
		MemDir parent = (MemDir) this.getDir(parentPath);
		Node child = parent.getChild(Paths.basename(absolutePath));
		if (child != null) {
			throw FSErrors.alreadyExists(absolutePath);
		}
		LinkNode newLink = new MemLink(absoluteTarget, 0, 0, 0);
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
				throw FSErrors.notFound(path);
			} else if (!current.isDir()) {
				throw FSErrors.notDir(path);
			}
			current = ((MemDir)current).getChild(elem);
		}
		if (current == null){
			throw FSErrors.notFound(path);
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
			throw FSErrors.notFound(path);
		} else if (child.isDir()) {
			throw FSErrors.isDir(path);
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
			throw FSErrors.notFound(path);
		} else if (!child.isDir()) {
			throw FSErrors.notDir(path);
		}
		DirNode dir = (DirNode) child;
		if (!dir.getChildren().isEmpty()) {
			throw FSErrors.notEmpty(path);
		}
		return (DirNode) parent.removeChild(Paths.basename(name));
	}

	@Override
	public DirNode getDir(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isDir()){
			throw FSErrors.notDir(path);
		}
		return (DirNode) n;
	}
	
	@Override
	public FileNode getFile(String path) throws FSError {
		Node n = this.get(path);
		if (!n.isFile()) {
			throw FSErrors.isDir(path);
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
			throw FSErrors.notFound(from);
		}
		Node nodeTo = parentTo.getChild(nameTo);
		if (nodeTo != null) {
			if (nodeFrom.isDir()) {
				if (!nodeTo.isDir()) { 
					throw FSErrors.notDir(to);
				} else if (!((DirNode) nodeTo).getChildren().isEmpty()) {
					throw FSErrors.notEmpty(to);
				}
			} else { // nodeFrom is a file or link
				if (nodeTo.isDir()) {
					throw FSErrors.isDir(to);
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

}
