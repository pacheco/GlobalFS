package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.Utils;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Created by pacheco on 05/03/15.
 */
public class HttpStorageTest {
    Random rand = new Random();
    LocalTestServer[] localServers = new LocalTestServer[3];
    String cfg;

    private Storage newStorage(String config) throws Exception {
        StringReader r = new StringReader(config);
        Storage st = new HttpStorage();
        st.initialize(r);
        return st;
    }

    @Before
    public void startStorageServers() throws Exception {
        for (int i = 0; i < 3; i++) {
            localServers[i] = new LocalTestServer(null, null);
            localServers[i].register("*", new HttpStorageRequestHandler());

            localServers[i].start();
        }
        cfg = String.format("1 http://localhost:%d\n2 http://localhost:%d\n3 http://localhost:%d\n",
                localServers[0].getServiceAddress().getPort(),
                localServers[1].getServiceAddress().getPort(),
                localServers[2].getServiceAddress().getPort());
    }

    @After
    public void killStorageServers() {
        for (LocalTestServer s: localServers) {
            try {
                s.stop();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testCreate() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("storagecfg/httpstorage.cfg");
        Path path = Paths.get(url.toURI());
        Storage st = StorageFactory.storageFromConfig(path);
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof HttpStorage);
    }

    @Test
    public void testPutGet() throws Exception {
        Storage st = newStorage(cfg);

        // insert fails if the key already exists or the partition is not known (in the config file)
        byte[] key1 = UUIDUtils.longToBytes(33);
        byte[] key2 = UUIDUtils.longToBytes(66);
        byte[] key3 = UUIDUtils.longToBytes(1123);
        byte[] value1 = Utils.randomBytes(rand, 2222);
        byte[] value2 = Utils.randomBytes(rand, 10);
        byte[] value3 = Utils.randomBytes(rand, 500);
        Assert.assertTrue(st.put((byte) 1, key1, value1).get());
        Assert.assertTrue(st.put((byte) 1, key2, value2).get());
        Assert.assertTrue(st.put((byte) 1, key3, value3).get());
        Assert.assertTrue(st.put((byte) 2, key2, value2).get());
        Assert.assertTrue(st.put((byte) 2, key3, value3).get());
        Assert.assertTrue(st.put((byte) 3, key3, value3).get());
        Assert.assertFalse(st.put((byte) 0, key1, value1).get());
        Assert.assertFalse(st.put((byte) 4, key2, value2).get());
        Assert.assertFalse(st.put((byte) 1, key1, value1).get());
        Assert.assertFalse(st.put((byte) 2, key2, value2).get());

        // gets work for existing keys. Returns null otherwise
        byte[] nonExistentKey = UUIDUtils.longToBytes(4356);
        byte nonExistentPartition = 7;
        Assert.assertArrayEquals(st.get((byte) 1, key2).get(), value2);
        Assert.assertArrayEquals(st.get((byte) 3, key3).get(), value3);
        Assert.assertNull(st.get(nonExistentPartition, key2).get()); // non-existent partition
        Assert.assertNull(st.get((byte) 3, nonExistentKey).get()); // non-existent key

        // test support for keys of 16 bytes
        byte[] longKey1 = new byte[16];
        byte[] longKey2 = new byte[16]; longKey2[15] = 1;
        Assert.assertTrue(st.put((byte) 1, longKey1, value1).get());
        Assert.assertTrue(st.put((byte) 1, longKey2, value2).get());
        Assert.assertArrayEquals(st.get((byte) 1, longKey1).get(), value1);
        Assert.assertArrayEquals(st.get((byte) 1, longKey2).get(), value2);
    }

    /* delete is not supported and throws NotImplementedException */
    @Test(expected = RuntimeException.class)
    public void testDelete() throws Exception {
        Storage st = newStorage("cfg");
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof HttpStorage);

        byte[] key1 = UUIDUtils.longToBytes(33);
        st.delete((byte) 1, key1).get();
    }
}
