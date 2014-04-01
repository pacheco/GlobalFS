package ch.inf.paxosfs.filesystem.memory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ch.inf.paxosfs.filesystem.FileNode;
import ch.inf.paxosfs.rpc.Attr;
import ch.inf.paxosfs.rpc.DBlock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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
	public List<DBlock> getBlocks(long offset, long bytes) {
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
		
		long startingBlockOffset = b.getEndOffset() - (currOffset - offset); 
		DBlock startingBlock = new DBlock(b.getId(), startingBlockOffset, b.getEndOffset());
		int startingBlockIdx = iter.previousIndex();
		
		// find ending block and calculate its endingOffset
		while (currOffset <= offset + bytes && iter.hasNext()) {
			b = iter.next();
			currOffset += b.size();
		}
		
		ArrayList<DBlock> ret = Lists.newArrayList(this.blocks.subList(startingBlockIdx, iter.previousIndex()));
		ret.set(0, startingBlock); // put adjusted starting block on the return list

		if (currOffset > offset) {
			// put adjusted ending block
			long endingBlockOffset = b.getEndOffset() - (currOffset - (offset + bytes));
			DBlock endingBlock = new DBlock(b.getId(), b.getStartOffset(), endingBlockOffset);
			ret.set(iter.previousIndex()-1, endingBlock);
		}
					
		return ret;
	}

	@Override
	public void setData(Iterable<DBlock> blocks) {
		this.blocks = Lists.newLinkedList(blocks);
		int size = 0;
		for (DBlock b : blocks) {
			size += b.size();
		}
		this.getAttributes().setSize(size);
	}

	@Override
	public void appendData(Iterable<DBlock> blocks) {
		int appendSize = 0;
		for (DBlock b : blocks) {
			this.blocks.add(b);
			appendSize += b.size();
		}
		this.getAttributes().setSize(this.getAttributes().getSize() + appendSize);
	}

	@Override
	public void updateData(Iterable<DBlock> blocks, long offset) {
		// handle some special cases
		if (offset < 0) {
			// FIXME: maybe doing this implicitly is not good
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
		long bytesWritten = 0;
		for (DBlock b: blocks) {
			iter.add(b);
			bytesWritten += b.size();
		}
		
		long bytesToRemove = bytesWritten;

		// now we need to remove blocks and fix offsets
		
		if (startOverwrite) {
			// write starts in the middle of a block
			long offdiff = currentOffset - offset;
			offsetBlock.setEndOffset(offsetBlock.getEndOffset() - offdiff);
			bytesToRemove -= offdiff;
		}
		
		if (startOverwrite && bytesWritten < currentOffset - offset) { 
			// handling special case: data written is "splitting" a single block. Copy it and fix the offsets
			DBlock copy = new DBlock(offsetBlock.id, 
					offsetBlock.getEndOffset() + bytesWritten, 
					offsetBlock.getEndOffset() + currentOffset - offset);
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

	@Override
	public void truncate(long size) {
		long sizeDiff = size - this.getAttributes().getSize();
		if (sizeDiff > 0) {
			// increase file size by adding a null block of the size difference
			blocks.add(new DBlock(0, 0, sizeDiff));
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
					b.setEndOffset(b.getEndOffset() - (acumm - size));
					if (b.size() == 0) {
						iter.remove();
					}
					break;
				}
			}
			this.getAttributes().setSize(size);

			// TODO: we will probably need another data structure to avoid
			// traversing the list again here
			// TODO: removed blocks could be stored for DHT garbage collection.
			// This is not so simple because we can have blocks used in multiple
			// positions (after an overwrite to the middle of a block it will be
			// split)
			this.blocks.subList(iter.nextIndex(), this.blocks.size()).clear();
		}
	}
}
