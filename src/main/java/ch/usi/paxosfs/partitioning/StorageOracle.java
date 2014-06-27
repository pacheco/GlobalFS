package ch.usi.paxosfs.partitioning;

import java.util.Set;

public interface StorageOracle {
	/**
	 * Takes a set of partitions (where a given file is replicated) and returns the storage ids (where the blocks of a file should be placed)
	 * @param partitions
	 * @return
	 */
	Set<Byte> storageOf(Set<Byte> partitions);
}
