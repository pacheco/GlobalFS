package ch.usi.paxosfs.filesystem.memory;

import ch.usi.paxosfs.filesystem.Node;
import ch.usi.paxosfs.rpc.Attr;

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
