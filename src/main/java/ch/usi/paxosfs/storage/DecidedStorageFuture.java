package ch.usi.paxosfs.storage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Used when a future is needed but its value is already available/decided.
 */
class DecidedStorageFuture<T> implements StorageFuture<T> {
    private final T theValue;
    private final byte thePartition;
    private final byte[] theKey;

    public DecidedStorageFuture(final byte partition, final byte[] key, final T value) {
        theValue = value;
        theKey = key;
        thePartition = partition;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return theValue;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return theValue;
    }

    @Override
    public byte getPartition() {
        return thePartition;
    }

    @Override
    public byte[] getKey() {
        return theKey;
    }
}
