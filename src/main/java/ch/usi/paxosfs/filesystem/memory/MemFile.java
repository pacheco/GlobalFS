package ch.usi.paxosfs.filesystem.memory;

import ch.usi.paxosfs.filesystem.FileNode;
import ch.usi.paxosfs.rpc.Attr;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.ReadResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import fuse.FuseFtypeConstants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class MemFile extends MemNode implements FileNode {
	private LinkedList<DBlock> blocks;

	public MemFile(int mode, int time, int uid, int gid) {
		this.setAttributes(new Attr(0, mode, 1, uid, gid, 0, time, time, time, 0, 0));
		blocks = new LinkedList<DBlock>();
	}

	@Override
	public boolean isFile() {
		return true;
	}

	public List<DBlock> getBlocks() {
		return ImmutableList.copyOf(blocks);
	}
	
	@Override
	public ReadResult getBlocks(long offset, long bytes) {
		if (offset < 0 || offset >= this.getAttributes().getSize() || bytes <= 0) {
			return null;
		}
		
		long currOffset = 0;
		ListIterator<DBlock> iter = this.blocks.listIterator();
		// find starting block and calculate its startingOffset
		DBlock b = null;
		while (iter.hasNext()) {
			b = iter.next();
			currOffset += b.size();
			if (currOffset > offset) {
				break;
			}
		}
		assert(currOffset > offset);
		
		int startingBlockOffset = b.getEndOffset() - (int)(currOffset - offset); 
		DBlock startingBlock = new DBlock(null, startingBlockOffset, b.getEndOffset(), b.getStorage());
		startingBlock.setId(b.getId());
		int startingBlockIdx = iter.previousIndex();
		
		// find ending block and calculate its endOffset
		while (currOffset < offset + bytes && iter.hasNext()) {
			b = iter.next();
			currOffset += b.size();
		}
		
		ArrayList<DBlock> readBlocks = Lists.newArrayList(this.blocks.subList(startingBlockIdx, iter.previousIndex()+1));
		readBlocks.set(0, startingBlock); // put adjusted starting block on the return list

		if (currOffset > offset + bytes) {
			// put adjusted ending block
			int endingBlockOffset = b.getEndOffset() - (int) (currOffset - (offset + bytes));
			if (iter.previousIndex() == startingBlockIdx) { // only one block - the one we created before
				readBlocks.get(0).setEndOffset(endingBlockOffset);
			} else {
				DBlock endingBlock = new DBlock(null, b.getStartOffset(), endingBlockOffset, b.getStorage());
				endingBlock.setId(b.getId());
				readBlocks.set(readBlocks.size()-1, endingBlock);
			}
		}
		ReadResult rr = new ReadResult(readBlocks);
					
		return rr;
	}

	public void setData(Iterable<DBlock> blocks) {
		this.blocks = Lists.newLinkedList(blocks);
		int size = 0;
		for (DBlock b : blocks) {
			size += b.size();
		}
		this.getAttributes().setSize(size);
	}

	public void appendData(Iterable<DBlock> blocks) {
		int appendSize = 0;
		for (DBlock b : blocks) {
			this.blocks.add(b);
			appendSize += b.size();
		}
		this.getAttributes().setSize(this.getAttributes().getSize() + appendSize);
	}

	public void updateData(Iterable<DBlock> blocks, long offset) {
		// handle some special cases
		if (offset < 0) {
			// FIXME: throw an error instead of using zero implicitly?
			offset = 0;
		}
		if (offset >= this.getAttributes().getSize()) {
			// just an append
			this.truncate(offset);
			this.appendData(blocks);
			return;
		}
		
		ListIterator<DBlock> iter = this.blocks.listIterator();
		// position we will update as we traverse the blocks
		long currentOffset = 0;
		// block where the offset starts
		DBlock offsetBlock = null;
		boolean startOverwrite = false;
		while (iter.hasNext()) {
			if (currentOffset == offset) {
				// starting at block limit
				break;
			}
			offsetBlock = iter.next();
			currentOffset += offsetBlock.size();
			if (currentOffset == offset) {
				break;
			} else if (currentOffset > offset) {
				// overwrite part of the beginning block
				startOverwrite = true;
				break;
			}
		}
		
		// add new data
		int bytesWritten = 0;
		for (DBlock b: blocks) {
			iter.add(b);
			bytesWritten += b.size();
		}
		
		int bytesToRemove = bytesWritten;

		// now we need to remove blocks and fix offsets
		
		if (startOverwrite) {
			// write starts in the middle of a block
			int offdiff = (int) (currentOffset - offset);
			offsetBlock.setEndOffset(offsetBlock.getEndOffset() - offdiff);
			bytesToRemove -= offdiff;
		}
		
		if (startOverwrite && bytesWritten < currentOffset - offset) { 
			// handling special case: data written is "splitting" a single block. Copy it and fix the offsets
			DBlock copy = new DBlock(null, 
					offsetBlock.getEndOffset() + bytesWritten, 
					offsetBlock.getEndOffset() + (int) (currentOffset - offset),
					offsetBlock.getStorage());
			copy.setId(offsetBlock.getId());
			iter.add(copy);
		} else {
			// iterate remaining blocks removing until we "overwrite" (that is, remove from the block list) bytesWritten bytes
			while (iter.hasNext()) {
				DBlock b = iter.next();
				if (b.size() > bytesToRemove) {
					// removed enough blocks, fix offset
					b.setStartOffset(b.getStartOffset() + bytesToRemove);
					bytesToRemove = 0;
					break;
				} else {
					iter.remove();
					bytesToRemove -= b.size();
				}
			}
			if (bytesToRemove > 0) {
				// written past limit, update file size
				this.getAttributes().setSize(this.getAttributes().getSize() + bytesToRemove);
			}
		}
	}

	public void truncate(long size) {
		long sizeDiff = size - this.getAttributes().getSize();
		if (sizeDiff > 0L) {
			// increase file size by adding a null block of the size difference
			while (sizeDiff > Integer.MAX_VALUE) {
				DBlock nullBlock = new DBlock(null, 0, (int) Integer.MAX_VALUE, null); 
				blocks.add(nullBlock);
				sizeDiff -= Integer.MAX_VALUE;
				nullBlock.setId(new byte[0]);
			}
			if (sizeDiff > 0) {
				DBlock nullBlock = new DBlock(null, 0, (int) sizeDiff, null); 
				blocks.add(nullBlock);
				nullBlock.setId(new byte[0]);
			}
			this.getAttributes().setSize(size);
		} else if (sizeDiff == 0) {
			// same size
			return;
		} else {
			// truncate file
			long acumm = 0;
			ListIterator<DBlock> iter = this.blocks.listIterator();
			while (iter.hasNext()) {
				DBlock b = iter.next();
				acumm += b.size();
				if (acumm >= size) {
					// new last block
					b.setEndOffset(b.getEndOffset() - (int)(acumm - size));
					if (b.size() == 0) {
						iter.remove();
					}
					break;
				}
			}
			this.getAttributes().setSize(size);

			// TODO: we will probably need another data structure to avoid traversing the list again here
			this.blocks.subList(iter.nextIndex(), this.blocks.size()).clear();
		}
	}

	public int typeMode() {
		return FuseFtypeConstants.TYPE_FILE;
	}
}
