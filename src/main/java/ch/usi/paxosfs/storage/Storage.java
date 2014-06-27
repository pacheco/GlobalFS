package ch.usi.paxosfs.storage;

import java.util.concurrent.Future;

public interface Storage {
	/**
	 * Assign data to a key. Does the operation asynchronously returning a Future<Boolean> (false if the operation was unsucessful) 
	 * @param key
	 * @param data
	 * @return
	 */
	Future<Boolean> put(byte[] key, byte[] value);
	
	/**
	 * Fetch data related to a key. Return null if the key does not exist
	 * @param key
	 * @return
	 */
	Future<byte[]> get(byte[] key);
	
	/**
	 * Delete data related to a key
	 * @param key
	 * @return
	 */
	Future<Boolean> delete(byte[] key);
}
