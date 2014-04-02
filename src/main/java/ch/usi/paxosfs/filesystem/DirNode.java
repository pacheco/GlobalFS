package ch.usi.paxosfs.filesystem;

import java.util.Collection;

public interface DirNode extends Node {
	Collection<String> getChildren();
	Node getChild(String name);
	void addChild(String name, Node child);
	Node removeChild(String name);
}
