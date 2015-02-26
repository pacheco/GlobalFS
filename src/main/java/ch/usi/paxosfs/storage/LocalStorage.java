package ch.usi.paxosfs.storage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Future;

/**
 * Storage that stores its values locally in HashMaps.
 * This storage does not use the config file. It will let a client 'put' using any partition id, maps are created when required.
 */
public class LocalStorage implements Storage {
    HashMap<Byte, HashMap<String, byte[]>> partitions;

    public void initialize(Path configFile) throws Exception {
        partitions = new HashMap<>();
    }

    @Override
    public Future<Boolean> put(byte partition, byte[] key, byte[] value) {
        HashMap<String, byte[]> store = partitions.get(Byte.valueOf(partition));
        if (store == null) {
            store = new HashMap<>();
            partitions.put(Byte.valueOf(partition), store);
        }
        store.put(new String(key), value);
        return new DecidedFuture<>(true);
    }

    @Override
    public Future<byte[]> get(byte partition, byte[] key) {
        HashMap<String, byte[]> store = partitions.get(Byte.valueOf(partition));
        byte[] value = null;
        if (store != null) {
            value = store.get(new String(key));
        }
        return new DecidedFuture<>(value);
    }

    @Override
    public Future<Boolean> delete(byte partition, byte[] key) {
        HashMap<String, byte[]> store = partitions.get(Byte.valueOf(partition));
        if (store != null) {
            if (store.remove(new String(key)) != null) {
                return new DecidedFuture<>(true);
            }
        }
        return new DecidedFuture<>(false);
    }
}
