package ch.usi.paxosfs.filesystem;

import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.ReadResult;

import java.util.List;

public interface FileNode extends Node {
	/**
	 * List of all blocks
	 * @return
	 */
	List<DBlock> getBlocks();
	/**
	 * List of blocks/offsets for the bytes wanted
	 * @param offset
	 * @param bytes
	 * @return
	 */
	ReadResult getBlocks(long offset, long bytes);
	/**
	 * Set file contents
	 * @param blocks
	 * @param fileSize
	 */
	void setData(Iterable<DBlock> blocks);
	/**
	 * Append data to the end of the file
	 * @param blocks
	 * @param nBytes
	 */
	void appendData(Iterable<DBlock> blocks);
	/**
	 * Write data to a given offset
	 * @param blocks
	 * @param offset
	 */
	void updateData(Iterable<DBlock> blocks, long offset);
	/**
	 * Truncate file to a given size. If it is larger than the current size, a new null block (id == 0) of the necessary size will be created.
	 * @param index
	 */
	void truncate(long size);
}
