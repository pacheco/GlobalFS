package ch.usi.paxosfs.filesystem.memory;

import java.util.Collection;
import java.util.HashMap;

import ch.usi.paxosfs.filesystem.DirNode;
import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.rpc.Attr;
import fuse.FuseFtypeConstants;

public class MemDir extends MemNode implements DirNode {
	private HashMap<String, Node> children;
	
	@Override
	public boolean isDir() {
		return true;
	}

	public MemDir(int mode, int time, int uid, int gid) {
		this.children = new HashMap<String, Node>();
		this.setAttributes(new Attr(0, mode, 0, uid, gid, 0, time, time, time, 0, 0));
	}

	public Collection<String> getChildren() {
		return children.keySet();
	}

	public Node getChild(String name) {
		return children.get(name);
	}

	public void addChild(String name, Node child) {
		this.children.put(name, child);
	}

	public Node removeChild(String name) {
		return children.remove(name);
	}
	
	public int typeMode() {
		return FuseFtypeConstants.TYPE_DIR;
	}
	
	public boolean isEmpty() {
		return this.children.isEmpty();
	}
}
