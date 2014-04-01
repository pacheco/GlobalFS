package ch.inf.paxosfs.partitioning;



public interface PartitioningOracle {
	/**
	 * Return the ids of the groups responsible for this path
	 * @param path
	 * @return
	 */
	public int[] partitionsOf(String path);
	
	/**
	 * Returns true if a given partition is responsible for this path
	 * @param path
	 * @param partition
	 * @return
	 */
	public boolean partitionHasPath(String path, int partition);
}
