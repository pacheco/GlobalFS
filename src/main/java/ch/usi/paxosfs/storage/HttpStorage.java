package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import com.google.common.cache.*;
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

public class HttpStorage implements Storage {
	private static final int TIMEOUT = 3000;
    private static final int CACHE_WEIGHT = 32*1024*1024; // cache 32MB of data
	private static Random rand = new Random();
    private static Cache<String, byte[]> cache;
    private static Weigher<String, byte[]> cacheWeighter;
    /* Each partition has a list of servers */
    private Map<Byte, List<String>> partitionServers;
	private ExecutorService threadpool;
	private Async asyncHttp;

    private class GetHandler implements ResponseHandler<byte[]> {
        private final String keyStr;
        public GetHandler(String keyStr) {
            this.keyStr = keyStr;
        }

        @Override
        public byte[] handleResponse(HttpResponse httpResponse) throws IOException {
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return null;
            }
            InputStream in = httpResponse.getEntity().getContent();
            byte[] value = IOUtils.toByteArray(in);
            in.close();
            // put value into cache
            cache.put(keyStr, value);
            return value;
        }
    }

	private class PutHandler implements ResponseHandler<Boolean> {
        private final String key;
        private final byte[] value;

        public PutHandler(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }

		@Override
		public Boolean handleResponse(HttpResponse response) throws IOException {
            if (Boolean.valueOf(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
                // put value into the cache
                cache.put(key, value);
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
		}
	}

	public HttpStorage() {
		threadpool = Executors.newFixedThreadPool(100);
		asyncHttp = Async.newInstance().use(threadpool);
        cacheWeighter = new Weigher<String, byte[]>() {
            @Override
            public int weigh(String key, byte[] value) {
                return value.length;
            }
        };

        cache = CacheBuilder.newBuilder()
                .weigher(cacheWeighter)
                .maximumWeight(CACHE_WEIGHT)
                .build();
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
	private static String keyBytesToString(byte[] bytes) {
        return UUIDUtils.bytesToHex(bytes);
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
    public StorageFuture<Boolean> put(final byte partition, final byte[] key, final byte[] value) {
        String server = this.randomServer(Byte.valueOf(partition));
        if (server == null) { // unknown partition
            return new DecidedStorageFuture<>(partition, key, false);
        }
        final String keyStr = keyBytesToString(key);
        Request req = Request.Put(server + keyStr)
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Sync-Mode", "sync")
                .connectTimeout(TIMEOUT)
                .bodyByteArray(value);
        synchronized (asyncHttp) {
            return new StorageFutureWrapper<>(partition, key, asyncHttp.execute(req, new PutHandler(keyStr, value)));
        }
    }

    @Override
    public StorageFuture<byte[]> get(final byte partition, final byte[] key) {
        final String server = this.randomServer(Byte.valueOf(partition));
        if (server == null) { // unknown partition
            return new DecidedStorageFuture<>(partition, key, null);
        }
        // first look into the local cache
        final String keyStr = keyBytesToString(key);
        final byte[] value = cache.getIfPresent(keyStr);
        if (value != null) {
            return new DecidedStorageFuture<>(partition, key, value);
        }
        // go to the storage
        Request req = Request.Get(server + keyStr)
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Sync-Mode", "sync")
                .connectTimeout(TIMEOUT);
        synchronized (asyncHttp) {
            return new StorageFutureWrapper<>(partition, key, asyncHttp.execute(req, new GetHandler(keyStr)));
        }
    }

    @Override
    public StorageFuture<Boolean> delete(final byte partition, final byte[] key) {
        // TODO not implemented
        throw new NotImplementedException();
    }
}
