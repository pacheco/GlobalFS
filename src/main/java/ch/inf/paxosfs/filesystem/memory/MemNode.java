package ch.inf.paxosfs.filesystem.memory;

import ch.inf.paxosfs.filesystem.Node;
import ch.inf.paxosfs.rpc.Attr;

public abstract class MemNode implements Node {
	private Attr attr;
	
	public boolean isDir(){
		return false;
	}
	
	public boolean isLink(){
		return false;
	}
	
	public boolean isFile(){
		return false;
	}
	
	public Attr getAttributes() {
		return attr;
	}
	
	public void setAttributes(Attr attr) {
		this.attr = attr;
	}
}
