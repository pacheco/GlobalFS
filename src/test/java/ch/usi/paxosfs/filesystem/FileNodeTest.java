package ch.usi.paxosfs.filesystem;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.filesystem.memory.MemFile;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.ReadResult;

public class FileNodeTest {
	private static byte[] uuid(int i) {
		if (i == 0) return new byte[0];
		byte[] id = new byte[16];
		ByteBuffer.wrap(id).putInt(i);
		return id;
	}
	
	private DBlock newDBlock(int i, int start, int end) {
		DBlock b;
		b = new DBlock(null, start, end, new HashSet<Byte>());
		b.setId(uuid(i));
		return b;
	}

	@Test
	public void testSetData() {
		FileNode f = new MemFile(0, 0, 0, 0);
		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));
		blocks.add(newDBlock(3, 0, 1024));
		blocks.add(newDBlock(4, 0, 1024));

		f.setData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 * 4);

		blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));
		f.setData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 * 2);

		blocks = new ArrayList<DBlock>();
		f.setData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 0);
	}

	@Test
	public void testAppend() {
		FileNode f = new MemFile(0, 0, 0, 0);
		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(1, 0, 1024));
		f.setData(blocks);

		blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(2, 0, 512));
		f.appendData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 + 512);

		List<DBlock> fileBlocks = f.getBlocks();
		Assert.assertEquals(fileBlocks.size(), 2);
		Assert.assertArrayEquals(uuid(1), fileBlocks.get(0).getId());
		Assert.assertArrayEquals(uuid(2), fileBlocks.get(1).getId());
	}

	@Test
	public void testTruncate() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(456);
		Assert.assertEquals(f.getAttributes().getSize(), 456);
		Assert.assertEquals(f.getBlocks().size(), 1);
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(0).getId());
		Assert.assertEquals(f.getBlocks().get(0).size(), 456);

		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(1, 0, 1024));
		f.setData(blocks);
		f.truncate(1024);
		Assert.assertEquals(f.getAttributes().getSize(), 1024);
		Assert.assertEquals(f.getBlocks().size(), 1);
		Assert.assertArrayEquals(uuid(1), f.getBlocks().get(0).getId());
		Assert.assertEquals(f.getBlocks().get(0).size(), 1024);

		f.truncate(1025);
		Assert.assertEquals(f.getAttributes().getSize(), 1025);
		Assert.assertEquals(f.getBlocks().size(), 2);
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(1).getId());
		Assert.assertEquals(f.getBlocks().get(1).size(), 1);

		f.truncate(0);
		Assert.assertEquals(0, f.getAttributes().getSize());
		Assert.assertEquals(0, f.getBlocks().size());
		
		f.truncate(1024*1024*1024*1024L);
		ReadResult rr = f.getBlocks(0, 10);
		Assert.assertEquals(rr.getBlocks().size(), 1);
		Assert.assertEquals(rr.getBlocks().get(0).size(), 10);
		rr = f.getBlocks(10, 10);
		Assert.assertEquals(rr.getBlocks().size(), 1);
		Assert.assertEquals(rr.getBlocks().get(0).size(), 10);
	}

	@Test
	public void updateDataPastLimit() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(1024);
		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));

		f.updateData(blocks, 512);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 * 2 + 512);
		Assert.assertEquals(f.getBlocks().size(), 3);
		Assert.assertEquals(f.getBlocks().get(0).size(), 512);
		Assert.assertEquals(f.getBlocks().get(1).size(), 1024);
		Assert.assertEquals(f.getBlocks().get(2).size(), 1024);

		// offset past the end
		blocks.clear();
		blocks.add(newDBlock(3, 0, 1024));
		f.updateData(blocks, 3 * 1024);
		Assert.assertEquals(1024 * 4, f.getAttributes().getSize());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(3).getId()); // added a null block
															// of 512
		Assert.assertEquals(f.getBlocks().get(3).size(), 512);
		Assert.assertArrayEquals(uuid(3), f.getBlocks().get(4).getId());
		Assert.assertEquals(f.getBlocks().get(4).size(), 1024);
	}

	@Test
	public void updateDataSplitBlock() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(1024);

		ArrayList<DBlock> blocks = new ArrayList<>();
		blocks.add(newDBlock(1, 0, 1));
		f.updateData(blocks, 512);
		Assert.assertEquals(3, f.getBlocks().size());
		Assert.assertEquals(1024, f.getAttributes().getSize());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(0).getId());
		Assert.assertEquals(512, f.getBlocks().get(0).size());
		Assert.assertArrayEquals(uuid(1), f.getBlocks().get(1).getId());
		Assert.assertEquals(1, f.getBlocks().get(1).size());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(2).getId());
		Assert.assertEquals(1024 - 513, f.getBlocks().get(2).size());

		// split block again. 3 parts
		f.updateData(blocks, 720);
		Assert.assertEquals(5, f.getBlocks().size());
		Assert.assertEquals(1024, f.getAttributes().getSize());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(0).getId());
		Assert.assertEquals(512, f.getBlocks().get(0).size());
		Assert.assertArrayEquals(uuid(1), f.getBlocks().get(1).getId());
		Assert.assertEquals(1, f.getBlocks().get(1).size());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(2).getId());
		Assert.assertEquals(720 - 513, f.getBlocks().get(2).size());
		Assert.assertArrayEquals(uuid(1), f.getBlocks().get(3).getId());
		Assert.assertEquals(1, f.getBlocks().get(3).size());
		Assert.assertArrayEquals(uuid(0), f.getBlocks().get(4).getId());
		Assert.assertEquals(1024 - 721, f.getBlocks().get(4).size());
	}

	@Test
	public void updateData() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(2048);
		ArrayList<DBlock> blocks = new ArrayList<>();
		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));

		// update at 0
		f.updateData(blocks, 0);
		Assert.assertEquals(2048, f.getAttributes().getSize());
		Assert.assertEquals(2, f.getBlocks().size());

		// update begins right after a block
		blocks.clear();
		blocks.add(newDBlock(3, 0, 512));
		f.updateData(blocks, 1024);
		Assert.assertEquals(2048, f.getAttributes().getSize());
		Assert.assertEquals(3, f.getBlocks().size());
		Assert.assertEquals(1024, f.getBlocks().get(0).size());
		Assert.assertEquals(512, f.getBlocks().get(2).size());

		// update overwrites an entire block
		blocks.clear();
		f.truncate(0);
		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));
		blocks.add(newDBlock(3, 0, 1024));
		f.appendData(blocks);

		blocks.clear();
		blocks.add(newDBlock(4, 0, 1024));
		blocks.add(newDBlock(5, 0, 1024));
		f.updateData(blocks, 512);
		Assert.assertEquals(1024 * 3, f.getAttributes().getSize());
		Assert.assertEquals(4, f.getBlocks().size());

		Assert.assertArrayEquals(uuid(1), f.getBlocks().get(0).getId());
		Assert.assertEquals(512, f.getBlocks().get(0).size());

		Assert.assertArrayEquals(uuid(4), f.getBlocks().get(1).getId());
		Assert.assertEquals(1024, f.getBlocks().get(1).size());

		Assert.assertArrayEquals(uuid(5), f.getBlocks().get(2).getId());
		Assert.assertEquals(1024, f.getBlocks().get(2).size());

		Assert.assertArrayEquals(uuid(3), f.getBlocks().get(3).getId());
		Assert.assertEquals(512, f.getBlocks().get(3).size());
	}

	@Test
	public void testGetBlocks() {
		FileNode f = new MemFile(0, 0, 0, 0);
		List<DBlock> ret;
		ArrayList<DBlock> blocks = new ArrayList<>();

		blocks.add(newDBlock(1, 0, 1024));
		blocks.add(newDBlock(2, 0, 1024));
		blocks.add(newDBlock(3, 0, 512));
		blocks.add(newDBlock(4, 0, 512));
		f.setData(blocks);

		// read out of bounds
		Assert.assertNull(f.getBlocks(1024 * 3, 10));
		// read 0 bytes
		Assert.assertNull(f.getBlocks(0, 0));
		// read invalid offset
		Assert.assertNull(f.getBlocks(-1, 10));
		
		// read from byte 0, exactly how much is available -> return everything
		ret = f.getBlocks(0, 1024 * 3).getBlocks();
		Assert.assertEquals(4, ret.size());
		Assert.assertEquals(0, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(1024, ret.get(1).getEndOffset());
		Assert.assertEquals(0, ret.get(2).getStartOffset());
		Assert.assertEquals(512, ret.get(2).getEndOffset());
		Assert.assertEquals(0, ret.get(3).getStartOffset());
		Assert.assertEquals(512, ret.get(3).getEndOffset());

		// read from byte 1, more bytes than available -> return from 1 up to what is available
		ret = f.getBlocks(1, 1024 * 4).getBlocks();
		Assert.assertEquals(4, ret.size());
		Assert.assertEquals(1, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(1024, ret.get(1).getEndOffset());
		Assert.assertEquals(0, ret.get(2).getStartOffset());
		Assert.assertEquals(512, ret.get(2).getEndOffset());
		Assert.assertEquals(0, ret.get(3).getStartOffset());
		Assert.assertEquals(512, ret.get(3).getEndOffset());
		
		// read the exact block size
		ret = f.getBlocks(0, 1024).getBlocks();
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals(0, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		
		// read less than available -> return what was asked for
		ret = f.getBlocks(1020, 500).getBlocks(); 
		Assert.assertEquals(2, ret.size());
		Assert.assertEquals(1020, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(496, ret.get(1).getEndOffset());
		
		ret = f.getBlocks(1026, 1024).getBlocks(); // from 1020, read 2048 bytes
		Assert.assertEquals(2, ret.size());
		Assert.assertEquals(2, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(2, ret.get(1).getEndOffset());
	}
}
