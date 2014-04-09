package ch.usi.paxosfs.storage;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public class HttpStorageClient implements Storage {
	private Executor executor;
	private String serverUrl;

	/**
	 * Expects the server url string, as in "http://localhost:12345"
	 * @param serverUrl
	 * @throws IOException 
	 */
	public HttpStorageClient(String serverUrl) {
		this.executor = Executor.newInstance();
		this.serverUrl = serverUrl + "/";
	}
	
	@Override
	public boolean put(byte[] key, byte[] data) {
			try {
				Response r = this.executor.execute(Request.Put(this.serverUrl + new String(key))
						.addHeader("Content-Type", "text/plain")
						.addHeader("Sync-Mode", "sync")
						.connectTimeout(3000)
						.bodyByteArray(data));
				return r.returnResponse().getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
	}

	@Override
	public byte[] get(byte[] key) {
		try {
			Response r = this.executor.execute(Request.Get(this.serverUrl + new String(key))
					.connectTimeout(3000));
			HttpResponse resp = r.returnResponse();
			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return null;
			}
			return IOUtils.toByteArray(resp.getEntity().getContent());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean delete(byte[] key) {
		return false;
	}
	
	public static void main(String[] args) {
		Storage st = new HttpStorageClient("http://localhost:5000"); 
		System.out.println(st.put("1".getBytes(), "asdf".getBytes()));
		System.out.println(new String(st.get("1".getBytes())));
	}
}
