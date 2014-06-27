package ch.usi.paxosfs.partitioning;

import java.util.Set;

public class DefaultStorageOracle implements StorageOracle {

	@Override
	/**
	 * This default implementation basically considers one storage site per partition (and they have the same id)
	 */
	public Set<Byte> storageOf(Set<Byte> partitions) {
		return partitions;
	}

}
