package ch.usi.paxosfs.storage;

import java.nio.file.Path;

public class NullStorage implements Storage {
    byte[] data = new byte[1024];

    @Override
    public void initialize(Path configFile) {
    }

    @Override
    public StorageFuture<Boolean> put(byte partition, byte[] key, byte[] value) {
        return new DecidedStorageFuture<>(partition, key, Boolean.TRUE);
    }

    @Override
    public StorageFuture<byte[]> get(byte partition, byte[] key) {
        return new DecidedStorageFuture<>(partition, key, data);
    }

    @Override
    public StorageFuture<Boolean> delete(byte partition, byte[] key) {
        return new DecidedStorageFuture<>(partition, key, Boolean.TRUE);
    }

}
