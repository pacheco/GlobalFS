package ch.usi.paxosfs.storage;

import java.nio.file.Path;
import java.util.concurrent.Future;

public class NullStorage implements Storage {
    byte[] data = new byte[1024];

    @Override
    public void initialize(Path configFile) {
    }

    @Override
    public Future<Boolean> put(byte partition, byte[] key, byte[] value) {
        return new DecidedFuture<>(Boolean.TRUE);
    }

    @Override
    public Future<byte[]> get(byte partition, byte[] key) {
        return new DecidedFuture<>(data);
    }

    @Override
    public Future<Boolean> delete(byte partition, byte[] key) {
        return new DecidedFuture<>(Boolean.TRUE);
    }

}
