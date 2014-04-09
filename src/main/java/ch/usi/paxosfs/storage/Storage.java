package ch.usi.paxosfs.storage;

public interface Storage {
	/**
	 * Assign data to a key. Return false if key already exists 
	 * @param key
	 * @param data
	 * @return
	 */
	boolean put(byte[] key, byte[] data);
	
	/**
	 * Fetch data related to a key. Return null if the key does not exist
	 * @param key
	 * @return
	 */
	byte[] get(byte[] key);
	
	/**
	 * Delete data related to a key
	 * @param key
	 * @return
	 */
	boolean delete(byte[] key);
}
