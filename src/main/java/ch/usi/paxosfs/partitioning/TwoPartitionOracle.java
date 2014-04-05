package ch.usi.paxosfs.partitioning;

import java.util.HashSet;
import java.util.Set;

import ch.usi.paxosfs.util.Paths;

public class TwoPartitionOracle implements PartitioningOracle {
	Byte partitionA = Byte.valueOf((byte)1);
	String prefixA; 
	Byte partitionB = Byte.valueOf((byte)2);
	String prefixB;
	
	public TwoPartitionOracle(String prefixA, String prefixB) {
		this.prefixA = Paths.clean(prefixA) + "/";
		this.prefixB = Paths.clean(prefixB) + "/";
	}

	@Override
	public Set<Byte> partitionsOf(String path) {
		path = Paths.clean(path);
		path += "/";
		
		Set<Byte> involved = new HashSet<Byte>();
		if (path.startsWith(prefixA)) {
			involved.add(partitionA);
		}
		if (path.startsWith(prefixB)) {
			involved.add(partitionB);
		}
		// if its not specifically in any partition, its in all
		if (involved.isEmpty()) {
			involved.add(partitionA);
			involved.add(partitionB);
		}
		return involved;
	}
}
