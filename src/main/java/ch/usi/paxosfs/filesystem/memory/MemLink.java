package ch.usi.paxosfs.filesystem.memory;

import ch.usi.paxosfs.filesystem.LinkNode;
import ch.usi.paxosfs.filesystem.Permissions;
import ch.usi.paxosfs.rpc.Attr;
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
