package ch.usi.paxosfs.storage;

import java.io.Reader;
import java.nio.file.Path;

/**
 * This interface provides access to a partitioned key value store. Methods expect a partition id identifying where the operation should run.
 * Storage implementations should provide a default constructor to be used by the StorageFactory.
 */
public interface Storage {
    /**
     * Initialize the Storage given the config. Actual contents of config is implementation dependent.
     *
     * @param configReader reader for the config
     */
    void initialize(Reader configReader) throws Exception;

    /**
     * Assign data to a key inside a given partition. Does the operation asynchronously returning a Future<Boolean> (false if the operation was unsuccessful)
     *
     * @param partition
     * @param key
     * @param value
     * @return
     */
    StorageFuture<Boolean> put(byte partition, byte[] key, byte[] value);

    /**
     * Fetch data related to a key in a given partition. Future.get() returns null if the key does not exist
     *
     * @param partition
     * @param key
     * @return
     */
    StorageFuture<byte[]> get(byte partition, byte[] key);

    /**
     * Delete data related to a key in a given partition
     *
     * @param partition
     * @param key
     * @return
     */
    StorageFuture<Boolean> delete(byte partition, byte[] key);
}
