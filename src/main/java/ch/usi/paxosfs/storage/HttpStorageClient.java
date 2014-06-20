package ch.usi.paxosfs.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import ch.usi.paxosfs.util.UUIDUtils;

public class HttpStorageClient implements Storage {
	private static int TIMEOUT = 3000;
	private Executor executor;
	private String serverUrl;
	private ExecutorService threadpool;
	private Async asyncHttp;
	
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
	public HttpStorageClient(String serverUrl) {
		threadpool = Executors.newFixedThreadPool(100);
		asyncHttp = Async.newInstance().use(threadpool);
		this.executor = Executor.newInstance();
		this.serverUrl = serverUrl + "/";
	}
	
	/**
	 * Taken from http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	 */
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	private static String bytesToLongAsString(byte[] bytes) {
		long l = UUIDUtils.bytesToLong(bytes);
		return String.valueOf(l);
	}
	
	@Override
	public boolean put(byte[] key, byte[] value) {
			try {
				Response r = this.executor.execute(Request.Put(this.serverUrl + bytesToLongAsString(key))
						.addHeader("Content-Type", "application/octet-stream")
						.addHeader("Sync-Mode", "sync")
						.connectTimeout(TIMEOUT)
						.bodyByteArray(value));
				return r.returnResponse().getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
	}
	
	@Override
	public byte[] get(byte[] key) {
		try {
			Response r = this.executor.execute(Request.Get(this.serverUrl + bytesToLongAsString(key))
					.addHeader("Content-Type", "application/octet-stream")
					.addHeader("Sync-Mode", "sync")
					.connectTimeout(TIMEOUT));
			HttpResponse resp = r.returnResponse();
			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return null;
			}
			InputStream in = resp.getEntity().getContent();
			in.skip(6); // header???
			byte[] value = IOUtils.toByteArray(in); 
			in.close();
			return value;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean delete(byte[] key) {
		return false;
	}

	@Override
	public List<Boolean> multiPut(List<byte[]> keys, List<byte[]> values) {
		Queue<Future<Boolean>> futures = new LinkedList<>();
		ResponseHandler<Boolean> handler = new PutHandler();
		for (int i = 0; i < keys.size(); i++) {
			Request req = Request.Put(this.serverUrl + bytesToLongAsString(keys.get(i)))
					.addHeader("Content-Type", "application/octet-stream")
					.addHeader("Sync-Mode", "sync")
					.connectTimeout(TIMEOUT)
					.bodyByteArray(values.get(i));
			futures.add(asyncHttp.execute(req, handler));
		}
		
		List<Boolean> result = new ArrayList<>(keys.size());
		for (Future<Boolean> future: futures) {
			try {
				result.add(future.get());
			} catch (InterruptedException e) {
				result.add(Boolean.FALSE);
				e.printStackTrace();
			} catch (ExecutionException e) {
				result.add(Boolean.FALSE);
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	public List<byte[]> multiGet(List<byte[]> keys) {
		Queue<Future<Content>> futures = new LinkedList<>();
		for (int i = 0; i < keys.size(); i++) {
			Request req = Request.Get(this.serverUrl + bytesToLongAsString(keys.get(i)))
					.addHeader("Content-Type", "application/octet-stream")
					.addHeader("Sync-Mode", "sync")
					.connectTimeout(TIMEOUT);
			futures.add(asyncHttp.execute(req));
		}
		
		List<byte[]> values = new ArrayList<byte[]>(keys.size());
		for (Future<Content> future: futures) {
			try {
				Content c = future.get();
				InputStream in = c.asStream();
				in.skip(6);
				byte[] value = IOUtils.toByteArray(in);
				in.close();
				values.add(value);
			} catch (InterruptedException e) {
				values.add(null);
				e.printStackTrace();
			} catch (ExecutionException e) {
				values.add(null);
				e.printStackTrace();
			} catch (IOException e) {
				values.add(null);
				e.printStackTrace();
			}
		}
		return values;
	}
	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("main host blockLarge blockSmall");
		}
		Storage st = new HttpStorageClient(args[0]);
		long keyStart = 0;
		int n = 100;
		byte[] dataLarge = new byte[Integer.parseInt(args[1])];
		byte[] dataSmall = new byte[Integer.parseInt(args[2])];
		int proportion = dataLarge.length / dataSmall.length;
		float latencySum = 0.0f;
		System.out.print("Sequential put " + n + " times, blocks of " + dataLarge.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			st.put(UUIDUtils.longToBytes(keyStart++), dataLarge);
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		
		latencySum = 0.0f;
		System.out.print("Parallel put " + n + " times, " + proportion + " blocks of " + dataSmall.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			List<byte[]> keys = new ArrayList<>(proportion);
			List<byte[]> values = new ArrayList<>(proportion);
			for (int j = 0; j < proportion; j++) {
				keys.add(UUIDUtils.longToBytes(keyStart++));
				values.add(dataSmall);
			}
			st.multiPut(keys, values);
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
	
		/// reads
		latencySum = 0.0f;
		System.out.print("Sequential get, blocks of " + dataSmall.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			for (int j = 0; j < proportion; j++) {
				st.get(UUIDUtils.longToBytes(0));
			}
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
		
		latencySum = 0.0f;
		System.out.print("Parallel get, blocks of " + dataSmall.length + " ... ");
		for (int i = 0; i < n; i++) {
			long start = System.currentTimeMillis();
			List<byte[]> keys = new ArrayList<>(proportion);
			for (int j = 0; j < proportion; j++) {
				keys.add(UUIDUtils.longToBytes(0));
			}
			st.multiGet(keys);
			latencySum += System.currentTimeMillis() - start;
		}
		System.out.println(latencySum / n);
	
		
	}
}
