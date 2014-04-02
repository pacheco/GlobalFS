package ch.usi.paxosfs.partitioning;

import java.util.Set;

public interface PartitioningOracle {
	/**
	 * Return the ids of the groups responsible for this path
	 * @param path
	 * @return
	 */
	public Set<Byte> partitionsOf(String path);
}
