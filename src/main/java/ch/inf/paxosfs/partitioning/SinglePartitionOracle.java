package ch.inf.paxosfs.partitioning;

import java.util.Set;

import com.google.common.collect.Sets;


public class SinglePartitionOracle implements PartitioningOracle {
	byte partition;
	
	public SinglePartitionOracle(byte partition) {
		this.partition = partition;
	}

	@Override
	public Set<Byte> partitionsOf(String path) {
		return Sets.newHashSet(partition);
	}
}
