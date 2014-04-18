package ch.usi.paxosfs.partitioning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * This partitioning oracle will partition the path into the given number n of partitions. The partitioning works like this:
 * "/" - this is replicated by all partitions
 * "/1" - everything under this path is in partition 1
 * "/2" - everything under this path is in partition 2
 * ...
 * "/n" - everything under this path is in partition n 
 * @author pacheco
 *
 */
public class DefaultMultiPartitionOracle implements PartitioningOracle {
	private int partitions;
	private HashMap<String, Set<Byte>> partitionMapping = new HashMap<>();
	private HashSet<Byte> allPartitions = new HashSet<>();
	
	private String firstElem(String path) {
		int elemEnd = path.indexOf("/", 1);
		if (elemEnd == -1) elemEnd = path.length();
		return path.substring(1, elemEnd);
	}
	
	public DefaultMultiPartitionOracle(int numberOfPartitions) {
		this.partitions = numberOfPartitions;
		for (byte i = 1; i <= partitions; i++) {
			partitionMapping.put(String.valueOf(i), Sets.newHashSet(Byte.valueOf(i)));
			allPartitions.add(Byte.valueOf(i));
		}
	}
	
	@Override
	public Set<Byte> partitionsOf(String path) {
		if (path == "/") {
			return allPartitions;
		}
		Set<Byte> p;
		p = partitionMapping.get(firstElem(path));
		if (p == null) {
			return allPartitions;
		}
		return p;
	}
}