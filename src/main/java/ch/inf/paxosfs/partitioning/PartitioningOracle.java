package ch.inf.paxosfs.partitioning;



public interface PartitioningOracle {
	/**
	 * Return the id of the group that the command has to be sent to given the paths involved
	 * @param path
	 * @return
	 */
	public int partitionOf(String path);
}
