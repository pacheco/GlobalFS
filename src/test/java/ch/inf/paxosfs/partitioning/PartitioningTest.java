package ch.inf.paxosfs.partitioning;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.partitioning.TwoPartitionOracle;

public class PartitioningTest {

	@Test
	public void testTwoPartitionOracle() {
		PartitioningOracle o = new TwoPartitionOracle("/foo", "/bar");
		assertTrue(o.partitionsOf("/").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/").contains(Byte.valueOf((byte)2)));
		
		assertTrue(o.partitionsOf("/foo/").contains(Byte.valueOf((byte)1)));
		assertTrue(!o.partitionsOf("/foo/").contains(Byte.valueOf((byte)2)));

		assertTrue(!o.partitionsOf("/bar/").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/bar/").contains(Byte.valueOf((byte)2)));
		
		assertTrue(o.partitionsOf("/foo").contains(Byte.valueOf((byte)1)));
		assertTrue(!o.partitionsOf("/foo").contains(Byte.valueOf((byte)2)));

		assertTrue(!o.partitionsOf("/bar").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/bar").contains(Byte.valueOf((byte)2)));

		assertTrue(o.partitionsOf("/foolia").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/foolia").contains(Byte.valueOf((byte)2)));

		assertTrue(o.partitionsOf("/barao").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/barao").contains(Byte.valueOf((byte)2)));
		
	}

}
