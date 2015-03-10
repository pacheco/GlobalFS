package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
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
import java.util.concurrent.*;

public class MemcachedStorage implements Storage {
    private Map<Byte, MemcachedClient> partitionClients;
	private static Random rand = new Random();

	public MemcachedStorage() {
        this.partitionClients = new HashMap<>();
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
     * partition_id address
     *
     * There can be multiple addresses per partition. For example:
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

        /* Each partition has a list of servers */
        Map<Byte, List<String>> partitionServers = new HashMap<>();

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
            servers.add(url);
		}

        for (Byte partition: partitionServers.keySet()) {
            partitionClients.put(partition, new MemcachedClient(AddrUtil.getAddresses(partitionServers.get(partition))));
        }
    }

    @Override
    public Future<Boolean> put(byte partition, byte[] key, byte[] value) {
        MemcachedClient c = partitionClients.get(Byte.valueOf(partition));
        if (c == null) {
            return new DecidedFuture<>(Boolean.FALSE);
        }
        return c.add(keyBytesToString(key), 0, value);
    }

    @Override
    public Future<byte[]> get(byte partition, byte[] key) {
        MemcachedClient c = partitionClients.get(Byte.valueOf(partition));
        if (c == null) {
            return new DecidedFuture<>(null);
        }
        return (Future<byte[]>)(Future<?>) c.asyncGet(keyBytesToString(key));
    }

    @Override
    public Future<Boolean> delete(byte partition, byte[] key) {
        // TODO not implemented
        throw new NotImplementedException();
    }

    /**
     * Only used for testing
     */
    protected void clearStorage() {
        for (MemcachedClient c: partitionClients.values()) {
            c.flush();
        }
    }
}