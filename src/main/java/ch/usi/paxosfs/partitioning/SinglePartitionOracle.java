package ch.usi.paxosfs.partitioning;

import com.google.common.collect.Sets;

import java.util.Set;


public class SinglePartitionOracle implements PartitioningOracle {
	byte partition;
	
	public SinglePartitionOracle() {
		this.partition = 1;
	}

	@Override
	public Set<Byte> partitionsOf(String path) {
		return Sets.newHashSet(Byte.valueOf(partition));
	}
}
