package ch.inf.paxosfs.partitioning;


public class SinglePartitionOracle implements PartitioningOracle {
	int partition;
	
	public SinglePartitionOracle(int partition) {
		this.partition = partition;
	}

	@Override
	public int partitionOf(String path) {
		return partition;
	}
	
}
