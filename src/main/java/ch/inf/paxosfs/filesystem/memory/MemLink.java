package ch.inf.paxosfs.filesystem.memory;

import ch.inf.paxosfs.filesystem.LinkNode;
import ch.inf.paxosfs.filesystem.Permissions;
import ch.inf.paxosfs.rpc.Attr;
import fuse.FuseFtypeConstants;

public class MemLink extends MemNode implements LinkNode {
	private final String target;
	
	@Override
	public boolean isLink() {
		return true;
	}
	
	public String getTarget() {
		return this.target;
	}
	
	public MemLink(String target, int time, int uid, int gid) {
		this.target = target;
		this.setAttributes(new Attr(0, Permissions.LINK, 1, uid, gid, 0, time, time, time, 0, 0));
	}

	public int typeMode() {
		return FuseFtypeConstants.TYPE_SYMLINK;
	}
}
