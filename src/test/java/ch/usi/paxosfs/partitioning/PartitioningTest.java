package ch.usi.paxosfs.partitioning;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

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
		PartitioningOracle o = new DefaultMultiPartitionOracle(12);
		Set<Byte> all = Sets.newHashSet();

        for (int i = 1; i <= 12; i++) {
            all.add((byte)i);
        }

		assertTrue(o.partitionsOf("/").size() == 12);
		assertTrue(o.partitionsOf("/").containsAll(all));

		assertTrue(o.partitionsOf("/13").size() == 12);
		assertTrue(o.partitionsOf("/13").containsAll(all));
        assertTrue(o.partitionsOf("/g").size() == 12);
        assertTrue(o.partitionsOf("/g").containsAll(all));

		assertTrue(o.partitionsOf("/1a").size() == 12);
        assertTrue(o.partitionsOf("/1a").containsAll(all));
		assertTrue(o.partitionsOf("/1a/asdf").size() == 12);
        assertTrue(o.partitionsOf("/1a/adsf").containsAll(all));

		assertTrue(o.partitionsOf("/2/adsf").contains(Byte.valueOf((byte)2)));
		assertTrue(o.partitionsOf("/2/asdf").size() == 1);
        assertTrue(o.partitionsOf("/3/4/adsf").contains(Byte.valueOf((byte)3)));
        assertTrue(o.partitionsOf("/3/4/adsf").size() == 1);
        assertTrue(o.partitionsOf("/12/asdf/1/asdf").contains(Byte.valueOf((byte) 12)));
        assertTrue(o.partitionsOf("/12/asdf/1/asdf").size() == 1);


		assertTrue(o.partitionsOf("/1").size() == 1);
		assertTrue(o.partitionsOf("/1").contains(Byte.valueOf((byte)1)));
		assertTrue(o.partitionsOf("/2").size() == 1);
		assertTrue(o.partitionsOf("/2").contains(Byte.valueOf((byte)2)));
		assertTrue(o.partitionsOf("/3").size() == 1);
		assertTrue(o.partitionsOf("/3").contains(Byte.valueOf((byte)3)));
		assertTrue(o.partitionsOf("/4").size() == 1);
		assertTrue(o.partitionsOf("/4").contains(Byte.valueOf((byte)4)));
		assertTrue(o.partitionsOf("/12").size() == 1);
		assertTrue(o.partitionsOf("/12").contains(Byte.valueOf((byte)12)));
}

}
