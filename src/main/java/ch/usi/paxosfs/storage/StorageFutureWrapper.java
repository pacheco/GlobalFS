package ch.usi.paxosfs.storage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by pacheco on 14/04/15.
 */
public class StorageFutureWrapper<T> implements StorageFuture<T> {
    private final byte thePartition;
    private final byte[] theKey;
    private final Future<T> f;

    public StorageFutureWrapper(byte partition, byte[] key, Future<T> wrapped) {
        thePartition = partition;
        theKey = key;
        f = wrapped;
    }

    @Override
    public byte getPartition() {
        return thePartition;
    }

    @Override
    public byte[] getKey() {
        return theKey;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return f.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return f.isCancelled();
    }

    @Override
    public boolean isDone() {
        return f.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return f.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return f.get(timeout, unit);
    }
}
