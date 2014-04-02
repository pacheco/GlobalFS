package ch.inf.paxosfs.filesystem;

import ch.inf.paxosfs.rpc.Attr;

public interface Node {
	boolean isDir();
	boolean isLink();
	boolean isFile();
	Attr getAttributes();
	int typeMode();
}
