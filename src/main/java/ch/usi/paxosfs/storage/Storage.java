package ch.usi.paxosfs.storage;

import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * This interface provides access to a partitioned key value store. Methods expect a partition id identifying where the operation should run.
 * Storage implementations should provide a default constructor to be used by the StorageFactory.
 */
public interface Storage {
    /**
     * Initialize the Storage given the config file. The first line in the config file is the name of the class (used by StorageFactory)
     * but the rest is free to be used by each implementation as required.
     *
     * @param configFile path to the config file
     */
    void initialize(Path configFile) throws Exception;

    /**
     * Assign data to a key inside a given partition. Does the operation asynchronously returning a Future<Boolean> (false if the operation was unsuccessful)
     *
     * @param partition
     * @param key
     * @param value
     * @return
     */
    Future<Boolean> put(byte partition, byte[] key, byte[] value);

    /**
     * Fetch data related to a key in a given partition. Future.get() returns null if the key does not exist
     *
     * @param partition
     * @param key
     * @return
     */
    Future<byte[]> get(byte partition, byte[] key);

    /**
     * Delete data related to a key in a given partition
     *
     * @param partition
     * @param key
     * @return
     */
    Future<Boolean> delete(byte partition, byte[] key);
}
