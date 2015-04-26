package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Storage that uses one "redis cluster" per partition.
 * The cluster topology (servers) will be found automatically by connecting to the addresses given (one per partition).
 */
public class RedisClusterStorage implements Storage {
    private Map<Byte, RedisClusterClient> partitionClients;
	private static Random rand = new Random();

	public RedisClusterStorage() {
        this.partitionClients = new HashMap<>();
	}

    private class RedisPutFuture implements StorageFuture<Boolean> {
        private final byte p;
        private final byte[] key;
        private final RedisFuture<String> f;

        RedisPutFuture(RedisFuture<String> f, byte p, byte[] key) {
            this.f = f;
            this.p = p;
            this.key = key;
        }

        @Override
        public byte getPartition() {
            return p;
        }

        @Override
        public byte[] getKey() {
            return key;
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
        public Boolean get() throws InterruptedException, ExecutionException {
            return f.get() == "OK";
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return f.get(timeout, unit) == "OK";
        }
    }

    private class RedisGetFuture implements StorageFuture<byte[]> {
        private final byte p;
        private final byte[] key;
        private final RedisFuture<String> f;

        RedisGetFuture(RedisFuture<String> f, byte p, byte[] key) {
            this.f = f;
            this.p = p;
            this.key = key;
        }

        @Override
        public byte getPartition() {
            return p;
        }

        @Override
        public byte[] getKey() {
            return key;
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
            String s = f.get();
            if (s == null) return null;
            return s.getBytes();
        }

        @Override
        public byte[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            String s = f.get(timeout, unit);
            if (s == null) return null;
            return s.getBytes();
        }
    }

    /**
     * The config file (excluding the first line) is expected to be a sequence of lines with the following format:
     *
     * partition_id address
     *
     * There can be a *single* address per partition (assumes redis cluster, cluster nodes will be found automatically)
     *
     * 1 1.2.3.4:5000
     * 1 1.2.3.5:5001
     * 2 1.2.3.5:5000
     * 3 1.2.3.6:5001
     *
     *
     */
    @Override
    public void initialize(Path configFile) throws Exception {
        Scanner sc = new Scanner(configFile);

        /* Each partition has a single server */
        Map<Byte, String> partitionServers = new HashMap<>();

        // skip first line
        sc.nextLine();

        while (sc.hasNext()) {
            if (sc.hasNext("#.*")) {
                sc.nextLine();
                continue;
            }
            Byte partition = Byte.valueOf(sc.nextByte());
            String url = sc.next();

            partitionServers.put(partition, url);
        }

        for (Byte partition: partitionServers.keySet()) {
            String url = partitionServers.get(partition);
            partitionClients.put(partition, new RedisClusterClient(RedisURI.create(url)));
        }
    }

    @Override
    public StorageFuture<Boolean> put(byte partition, byte[] key, byte[] value) {
        // TODO: does this create a *new* TCP connection everytime? If so we need to store and reuse it
        RedisClusterAsyncConnection<String, String> c = partitionClients.get(Byte.valueOf(partition)).connectClusterAsync();
        if (c == null) {
            return new DecidedStorageFuture<>(partition, key, Boolean.FALSE);
        }
        return new RedisPutFuture(c.set(new String(key), new String(value)), partition, key);
    }

    @Override
    public StorageFuture<byte[]> get(byte partition, byte[] key) {
        // TODO: does this create a *new* TCP connection everytime? If so we need to store and reuse it
        RedisClusterAsyncConnection<String, String> c = partitionClients.get(Byte.valueOf(partition)).connectClusterAsync();
        if (c == null) {
            return new DecidedStorageFuture<>(partition, key, null);
        }
        return new RedisGetFuture(c.get(new String(key)), partition, key);
    }

    @Override
    public StorageFuture<Boolean> delete(byte partition, byte[] key) {
        // TODO not implemented
        throw new NotImplementedException();
    }

    /**
     * Only used for testing
     */
    protected void clearStorage() throws ExecutionException, InterruptedException {
        for (RedisClusterClient c: partitionClients.values()) {
            RedisClusterAsyncConnection ac = c.connectClusterAsync();
            ac.flushall().get();
        }
    }
}
