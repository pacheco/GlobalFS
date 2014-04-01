import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.inf.paxosfs.filesystem.FileNode;
import ch.inf.paxosfs.filesystem.memory.MemFile;
import ch.inf.paxosfs.rpc.DBlock;

public class FileNodeTest {

	@Test
	public void testSetData() {
		FileNode f = new MemFile(0, 0, 0, 0);
		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));
		blocks.add(new DBlock(3, 0, 1024));
		blocks.add(new DBlock(4, 0, 1024));

		f.setData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 * 4);

		blocks = new ArrayList<DBlock>();
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));
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
		blocks.add(new DBlock(1, 0, 1024));
		f.setData(blocks);

		blocks = new ArrayList<DBlock>();
		blocks.add(new DBlock(2, 0, 512));
		f.appendData(blocks);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 + 512);

		List<DBlock> fileBlocks = f.getBlocks();
		Assert.assertEquals(fileBlocks.size(), 2);
		Assert.assertEquals(fileBlocks.get(0).id, 1);
		Assert.assertEquals(fileBlocks.get(1).id, 2);
	}

	@Test
	public void testTruncate() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(456);
		Assert.assertEquals(f.getAttributes().getSize(), 456);
		Assert.assertEquals(f.getBlocks().size(), 1);
		Assert.assertEquals(f.getBlocks().get(0).id, 0);
		Assert.assertEquals(f.getBlocks().get(0).size(), 456);

		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(new DBlock(1, 0, 1024));
		f.setData(blocks);
		f.truncate(1024);
		Assert.assertEquals(f.getAttributes().getSize(), 1024);
		Assert.assertEquals(f.getBlocks().size(), 1);
		Assert.assertEquals(f.getBlocks().get(0).id, 1);
		Assert.assertEquals(f.getBlocks().get(0).size(), 1024);

		f.truncate(1025);
		Assert.assertEquals(f.getAttributes().getSize(), 1025);
		Assert.assertEquals(f.getBlocks().size(), 2);
		Assert.assertEquals(f.getBlocks().get(1).id, 0);
		Assert.assertEquals(f.getBlocks().get(1).size(), 1);

		f.truncate(0);
		Assert.assertEquals(0, f.getAttributes().getSize());
		Assert.assertEquals(0, f.getBlocks().size());
	}

	@Test
	public void updateDataPastLimit() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(1024);
		ArrayList<DBlock> blocks = new ArrayList<DBlock>();
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));

		f.updateData(blocks, 512);
		Assert.assertEquals(f.getAttributes().getSize(), 1024 * 2 + 512);
		Assert.assertEquals(f.getBlocks().size(), 3);
		Assert.assertEquals(f.getBlocks().get(0).size(), 512);
		Assert.assertEquals(f.getBlocks().get(1).size(), 1024);
		Assert.assertEquals(f.getBlocks().get(2).size(), 1024);

		// offset past the end
		blocks.clear();
		blocks.add(new DBlock(3, 0, 1024));
		f.updateData(blocks, 3 * 1024);
		Assert.assertEquals(1024 * 4, f.getAttributes().getSize());
		Assert.assertEquals(f.getBlocks().get(3).id, 0); // added a null block
															// of 512
		Assert.assertEquals(f.getBlocks().get(3).size(), 512);
		Assert.assertEquals(f.getBlocks().get(4).id, 3);
		Assert.assertEquals(f.getBlocks().get(4).size(), 1024);
	}

	@Test
	public void updateDataSplitBlock() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(1024);

		ArrayList<DBlock> blocks = new ArrayList<>();
		blocks.add(new DBlock(1, 0, 1));
		f.updateData(blocks, 512);
		Assert.assertEquals(3, f.getBlocks().size());
		Assert.assertEquals(1024, f.getAttributes().getSize());
		Assert.assertEquals(0, f.getBlocks().get(0).id);
		Assert.assertEquals(512, f.getBlocks().get(0).size());
		Assert.assertEquals(1, f.getBlocks().get(1).id);
		Assert.assertEquals(1, f.getBlocks().get(1).size());
		Assert.assertEquals(0, f.getBlocks().get(2).id);
		Assert.assertEquals(1024 - 513, f.getBlocks().get(2).size());

		// split block again. 3 parts
		f.updateData(blocks, 720);
		Assert.assertEquals(5, f.getBlocks().size());
		Assert.assertEquals(1024, f.getAttributes().getSize());
		Assert.assertEquals(0, f.getBlocks().get(0).id);
		Assert.assertEquals(512, f.getBlocks().get(0).size());
		Assert.assertEquals(1, f.getBlocks().get(1).id);
		Assert.assertEquals(1, f.getBlocks().get(1).size());
		Assert.assertEquals(0, f.getBlocks().get(2).id);
		Assert.assertEquals(720 - 513, f.getBlocks().get(2).size());
		Assert.assertEquals(1, f.getBlocks().get(3).id);
		Assert.assertEquals(1, f.getBlocks().get(3).size());
		Assert.assertEquals(0, f.getBlocks().get(4).id);
		Assert.assertEquals(1024 - 721, f.getBlocks().get(4).size());
	}

	@Test
	public void updateData() {
		FileNode f = new MemFile(0, 0, 0, 0);
		f.truncate(2048);
		ArrayList<DBlock> blocks = new ArrayList<>();
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));

		// update at 0
		f.updateData(blocks, 0);
		Assert.assertEquals(2048, f.getAttributes().getSize());
		Assert.assertEquals(2, f.getBlocks().size());

		// update begins right after a block
		blocks.clear();
		blocks.add(new DBlock(3, 0, 512));
		f.updateData(blocks, 1024);
		Assert.assertEquals(2048, f.getAttributes().getSize());
		Assert.assertEquals(3, f.getBlocks().size());
		Assert.assertEquals(1024, f.getBlocks().get(0).size());
		Assert.assertEquals(512, f.getBlocks().get(2).size());

		// update overwrites an entire block
		blocks.clear();
		f.truncate(0);
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));
		blocks.add(new DBlock(3, 0, 1024));
		f.appendData(blocks);

		blocks.clear();
		blocks.add(new DBlock(4, 0, 1024));
		blocks.add(new DBlock(5, 0, 1024));
		f.updateData(blocks, 512);
		Assert.assertEquals(1024 * 3, f.getAttributes().getSize());
		Assert.assertEquals(4, f.getBlocks().size());

		Assert.assertEquals(1, f.getBlocks().get(0).id);
		Assert.assertEquals(512, f.getBlocks().get(0).size());

		Assert.assertEquals(4, f.getBlocks().get(1).id);
		Assert.assertEquals(1024, f.getBlocks().get(1).size());

		Assert.assertEquals(5, f.getBlocks().get(2).id);
		Assert.assertEquals(1024, f.getBlocks().get(2).size());

		Assert.assertEquals(3, f.getBlocks().get(3).id);
		Assert.assertEquals(512, f.getBlocks().get(3).size());
	}

	@Test
	public void testGetBlocks() {
		FileNode f = new MemFile(0, 0, 0, 0);

		ArrayList<DBlock> blocks = new ArrayList<>();
		blocks.add(new DBlock(1, 0, 1024));
		blocks.add(new DBlock(2, 0, 1024));
		blocks.add(new DBlock(3, 0, 1024));

		f.appendData(blocks);

		// read out of bounds
		List<DBlock> ret = f.getBlocks(1024 * 3, 10);
		Assert.assertNull(ret);
		// read 0 bytes
		ret = f.getBlocks(0, 0);
		Assert.assertNull(ret);
		// read invalid offset
		ret = f.getBlocks(-1, 10);
		Assert.assertNull(ret);

		// read from byte 0
		ret = f.getBlocks(0, 1024 * 3);
		Assert.assertEquals(3, ret.size());
		Assert.assertEquals(0, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(1024, ret.get(1).getEndOffset());
		Assert.assertEquals(0, ret.get(2).getStartOffset());
		Assert.assertEquals(1024, ret.get(2).getEndOffset());

		// read from byte 1
		ret = f.getBlocks(1, 1024 * 3);
		Assert.assertEquals(3, ret.size());
		Assert.assertEquals(1, ret.get(0).getStartOffset());
		Assert.assertEquals(1024, ret.get(0).getEndOffset());
		Assert.assertEquals(0, ret.get(1).getStartOffset());
		Assert.assertEquals(1024, ret.get(1).getEndOffset());
		Assert.assertEquals(0, ret.get(2).getStartOffset());
		Assert.assertEquals(1024, ret.get(2).getEndOffset());	}
}
