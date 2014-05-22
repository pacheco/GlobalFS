package ch.usi.paxosfs.storage;

import java.util.List;

public interface Storage {
	/**
	 * Assign data to a key. Return false if key already exists 
	 * @param key
	 * @param data
	 * @return
	 */
	boolean put(byte[] key, byte[] data);
	
	/**
	 * Put multiple keys into the DHT in parallel
	 * @param keys
	 * @param data
	 * @return
	 */
	boolean multiPut(List<byte[]> keys, List<byte[]> data);
	
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
