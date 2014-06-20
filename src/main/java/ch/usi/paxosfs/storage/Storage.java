package ch.usi.paxosfs.storage;

import java.util.List;

public interface Storage {
	/**
	 * Assign data to a key. Return false if key already exists 
	 * @param key
	 * @param data
	 * @return
	 */
	boolean put(byte[] key, byte[] value);
	
	/**
	 * Put multiple keys into the DHT in parallel. The keys and values need the same number of elements.
	 * @param keys
	 * @param data
	 * @return
	 */
	List<Boolean> multiPut(List<byte[]> keys, List<byte[]> values);
	
	/**
	 * Fetch data related to a key. Return null if the key does not exist
	 * @param key
	 * @return
	 */
	byte[] get(byte[] key);
	
	/**
	 * Fetch multiple values in parallel
	 * @param keys
	 * @return
	 */
	List<byte[]> multiGet(List<byte[]> keys);
	
	/**
	 * Delete data related to a key
	 * @param key
	 * @return
	 */
	boolean delete(byte[] key);
}
