package ch.usi.paxosfs.storage;

import java.util.concurrent.Future;

/**
 * Created by pacheco on 14/04/15.
 */
public interface StorageFuture<T> extends Future<T> {
    public byte getPartition();
    public byte[] getKey();
}
