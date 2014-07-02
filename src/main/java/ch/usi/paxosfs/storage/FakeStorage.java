package ch.usi.paxosfs.storage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FakeStorage implements Storage {
	private class FakeFuture<T> implements Future<T> {
		private T theValue;
		public FakeFuture(T value) {
			theValue = value;
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
	}
	
	byte[] data = new byte[1024*512];

	@Override
	public Future<Boolean> put(byte[] key, byte[] value) {
		return new FakeFuture<Boolean>(Boolean.TRUE);
	}

	@Override
	public Future<byte[]> get(byte[] key) {
		return new FakeFuture<byte[]>(data);
	}

	@Override
	public Future<Boolean> delete(byte[] key) {
		return new FakeFuture<Boolean>(Boolean.TRUE);
	}
	

}
