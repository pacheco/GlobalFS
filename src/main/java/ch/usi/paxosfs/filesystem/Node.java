package ch.usi.paxosfs.filesystem;

import ch.usi.paxosfs.rpc.Attr;

public interface Node {
	boolean isDir();
	boolean isLink();
	boolean isFile();
	Attr getAttributes();
	int typeMode();
}
