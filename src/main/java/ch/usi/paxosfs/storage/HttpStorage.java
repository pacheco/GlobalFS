package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Request;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HttpStorage implements Storage {
	private static int TIMEOUT = 3000;
	private static Random rand = new Random();
    /* Each partition has a list of servers */
    private Map<Byte, List<String>> partitionServers;
	private ExecutorService threadpool;
	private Async asyncHttp;

    private class GetHandler implements ResponseHandler<byte[]> {
        @Override
        public byte[] handleResponse(HttpResponse httpResponse) throws IOException {
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return null;
            }
            InputStream in = httpResponse.getEntity().getContent();
            byte[] value = IOUtils.toByteArray(in);
            in.close();
            return value;
        }
    }

	private class PutHandler implements ResponseHandler<Boolean> {
		@Override
		public Boolean handleResponse(HttpResponse response) throws IOException {
			return Boolean.valueOf(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		}
	}

	public HttpStorage() {
		threadpool = Executors.newFixedThreadPool(100);
		asyncHttp = Async.newInstance().use(threadpool);
	}

    /**
     * Return a random server url from a given partiton
     * @param partition
     * @return
     */
	private String randomServer(Byte partition) {
        List<String> servers = this.partitionServers.get(partition);
        if (servers != null && servers.size() > 0) {
            Integer selected = rand.nextInt(servers.size());
            return servers.get(selected);
        }
        return null;
	}

    /**
     * Interpret a sequence of bytes as a Long value and return its string representation
     * @param bytes
     * @return
     */
	private static String bytesToLongAsString(byte[] bytes) {
		long l = UUIDUtils.bytesToLong(bytes);
		return String.valueOf(l);
	}
	
    /**
     * The config file (excluding the first line) is expected to be a sequence of lines with the following format:
     *
     * partition_id url
     *
     * There can be multiple urls per partition (the http:// prefix is required). For example:
     *
     * 1 http://1.2.3.4:5000
     * 1 http://1.2.3.5:5001
     * 2 http://1.2.3.5:5000
     * 3 http://1.2.3.6:5001
     *
     *
     */
    @Override
    public void initialize(Path configFile) throws Exception {
        Scanner sc = new Scanner(configFile);

        partitionServers = new HashMap<>();

        // skip first line
        sc.nextLine();

		while (sc.hasNext()) {
			if (sc.hasNext("#.*")) {
				sc.nextLine();
				continue;
			}
			Byte partition = Byte.valueOf(sc.nextByte());
            String url = sc.next();
            List<String> servers = partitionServers.get(partition);
            if (servers == null) {
                servers = new LinkedList<>();
                partitionServers.put(partition, servers);
            }
            servers.add(url + '/');
		}
    }

    @Override
    public Future<Boolean> put(byte partition, byte[] key, byte[] value) {
        String server = this.randomServer(Byte.valueOf(partition));
        if (server == null) { // unknown partition
            return new DecidedFuture<>(false);
        }
        Request req = Request.Put(server + bytesToLongAsString(key))
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Sync-Mode", "sync")
                .connectTimeout(TIMEOUT)
                .bodyByteArray(value);
        return asyncHttp.execute(req, new PutHandler());
    }

    @Override
    public Future<byte[]> get(byte partition, byte[] key) {
        String server = this.randomServer(Byte.valueOf(partition));
        if (server == null) { // unknown partition
            return new DecidedFuture<>(null);
        }
        Request req = Request.Get(server + bytesToLongAsString(key))
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Sync-Mode", "sync")
                .connectTimeout(TIMEOUT);
        return asyncHttp.execute(req, new GetHandler());
    }

    @Override
    public Future<Boolean> delete(byte partition, byte[] key) {
        // TODO not implemented
        throw new NotImplementedException();
    }
}
