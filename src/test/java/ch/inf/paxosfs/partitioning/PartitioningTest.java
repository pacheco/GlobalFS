package ch.inf.paxosfs.partitioning;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
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
	
	@Test
	public void testMultiPartitionOracle() {
		PartitioningOracle o = new DefaultMultiPartitionOracle(5);
		Set<Byte> all = Sets.newHashSet(Byte.valueOf((byte)1),
				Byte.valueOf((byte)2),
				Byte.valueOf((byte)3),
				Byte.valueOf((byte)4),
				Byte.valueOf((byte)5));
		assertTrue(o.partitionsOf("/").size() == 5);
		assertTrue(o.partitionsOf("/").containsAll(all));

		assertTrue(o.partitionsOf("/6").size() == 5);
		assertTrue(o.partitionsOf("/6").containsAll(all));		
		
		assertTrue(o.partitionsOf("/1a").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/1a").size() == 1);
		assertTrue(o.partitionsOf("/1a/adsf").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/1a/asdf").size() == 1);

		assertTrue(o.partitionsOf("/2/adsf").contains(Byte.valueOf((byte)2)));
		assertTrue(o.partitionsOf("/2/asdf").size() == 1);
		
		assertTrue(o.partitionsOf("/1").size() == 1);
		assertTrue(o.partitionsOf("/1").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/2").size() == 1);
		assertTrue(o.partitionsOf("/2").contains(Byte.valueOf((byte)2)));
		assertTrue(o.partitionsOf("/3").size() == 1);
		assertTrue(o.partitionsOf("/3").contains(Byte.valueOf((byte)3)));
		assertTrue(o.partitionsOf("/4").size() == 1);
		assertTrue(o.partitionsOf("/4").contains(Byte.valueOf((byte)4)));
		assertTrue(o.partitionsOf("/5").size() == 1);
		assertTrue(o.partitionsOf("/5").contains(Byte.valueOf((byte)5)));
}

}
