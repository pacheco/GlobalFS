package ch.usi.paxosfs.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

import ch.usi.paxosfs.util.UUIDUtils;

public class HttpStorageClient implements Storage {
	private static int TIMEOUT = 3000;
	private static Random rand = new Random();
	private List<String> serverUrls;
	private ExecutorService threadpool;
	private Async asyncHttp;

	private class GetFuture implements Future<byte[]> {
		private Future<Content> f;
		public GetFuture(Future<Content> contentFuture) {
			f = contentFuture;
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
		public byte[] get() throws InterruptedException, ExecutionException {
			try {
				Content c = f.get();
				InputStream in = c.asStream();
				in.skip(6);
				byte[] value = IOUtils.toByteArray(in);
				in.close();
				return value;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		@Override
		public byte[] get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			try {
				Content c = f.get(timeout, unit);
				InputStream in = c.asStream();
				in.skip(6);
				byte[] value = IOUtils.toByteArray(in);
				in.close();
				return value;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}			
		}
	}
	
	private class PutHandler implements ResponseHandler<Boolean> {
		@Override
		public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			return Boolean.valueOf(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		}
	}

	/**
	 * Expects the server url string, as in "http://localhost:12345"
	 * @param serverUrl
	 * @throws IOException 
	 */
	public HttpStorageClient(String... serverUrls) {
		threadpool = Executors.newFixedThreadPool(100);
		asyncHttp = Async.newInstance().use(threadpool);
		this.serverUrls = new ArrayList<>(serverUrls.length);
		for (String url : serverUrls) {
			this.serverUrls.add(url + '/');
		}
	}
	
	private String randomUrl() {
		Integer proxy = rand.nextInt(this.serverUrls.size());
		return this.serverUrls.get(proxy);
	}
	
	private static String bytesToLongAsString(byte[] bytes) {
		long l = UUIDUtils.bytesToLong(bytes);
		return String.valueOf(l);
	}
	
	@Override
	public Future<Boolean> put(byte[] key, byte[] value) {
		Request req = Request.Put(this.randomUrl() + bytesToLongAsString(key))
				.addHeader("Content-Type", "application/octet-stream")
				.addHeader("Sync-Mode", "sync")
				.connectTimeout(TIMEOUT)
				.bodyByteArray(value);
		return asyncHttp.execute(req, new PutHandler());
	}

	@Override
	public Future<byte[]> get(byte[] key) {
		Request req = Request.Get(this.randomUrl() + bytesToLongAsString(key))
				.addHeader("Content-Type", "application/octet-stream")
				.addHeader("Sync-Mode", "sync")
				.connectTimeout(TIMEOUT);
		return new GetFuture(asyncHttp.execute(req));
	}

	@Override
	public Future<Boolean> delete(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}	
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		if (args.length < 3) {
			System.out.println("main host blockLarge blockSmall");
			return;
		}
		Storage st = new HttpStorageClient(args[0].split(","));
		long keyStart = 0;
		int n = 100;
		byte[] dataLarge = new byte[Integer.parseInt(args[1])];
		byte[] dataSmall = new byte[Integer.parseInt(args[2])];
		int proportion = dataLarge.length / dataSmall.length;
		float latencySum = 0.0f;
		System.out.print("Sequential put " + n + " times, blocks of " + dataLarge.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			st.put(UUIDUtils.longToBytes(keyStart), dataLarge).get();
			//keyStart++;
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		
		latencySum = 0.0f;
		System.out.print("Parallel put " + n + " times, " + proportion + " blocks of " + dataSmall.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			List<Future<Boolean>> putFutures = new ArrayList<>(proportion);
			for (int j = 0; j < proportion; j++) {
				putFutures.add(st.put(UUIDUtils.longToBytes(keyStart), dataSmall));
				//keyStart++;
			}
			for (Future<Boolean> f: putFutures) {
				f.get(); // wait for the operations to finish
			}
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		
		/// reads
		latencySum = 0.0f;
		keyStart = 0;
		System.out.print("Sequential get, blocks of " + dataLarge.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			st.get(UUIDUtils.longToBytes(keyStart)).get();
			//keyStart++;
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		
		latencySum = 0.0f;
		System.out.print("Parallel get, blocks of " + dataSmall.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			List<Future<byte[]>> getFutures = new ArrayList<>(proportion);
			for (int j = 0; j < proportion; j++) {
				getFutures.add(st.get(UUIDUtils.longToBytes(keyStart)));
				//keyStart++;
			}
			for (Future<byte[]> f : getFutures) {
				f.get(); // wait for operations to finish
			}
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		System.exit(0);		
	}
}
