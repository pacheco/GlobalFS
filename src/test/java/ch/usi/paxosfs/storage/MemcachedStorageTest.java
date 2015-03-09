package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by pacheco on 05/03/15.
 */
public class MemcachedStorageTest {
    Random rand = new Random();
    List<Process> storages = new LinkedList<>();

    private Storage newStorage(String configFile) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(configFile);
        Path path = Paths.get(url.toURI());
        Storage st = StorageFactory.storageFromConfig(path);
        return st;
    }

    @Before
    public void startStorageServers() throws Exception {
        storages.add(Runtime.getRuntime().exec("memcached -p 15001"));
        storages.add(Runtime.getRuntime().exec("memcached -p 15002"));
        storages.add(Runtime.getRuntime().exec("memcached -p 15003"));
    }

    @After
    public void killStorageServers() {
        for (Process s: storages) {
            s.destroy();
        }
    }

    @Test
    public void testCreate() throws Exception {
        Storage st = newStorage("storagecfg/memcachedstorage.cfg");
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof MemcachedStorage);
    }

    @Test
    public void testPutGet() throws Exception {
        Storage st = newStorage("storagecfg/memcachedstorage.cfg");
        ((MemcachedStorage)st).clearStorage();

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

        // test support for keys of 128 bits
        byte[] longKey1 = new byte[16];
        byte[] longKey2 = new byte[16]; longKey2[15] = 1;
        Assert.assertTrue(st.put((byte) 1, longKey1, value1).get());
        Assert.assertTrue(st.put((byte) 1, longKey2, value2).get());
        Assert.assertArrayEquals(st.get((byte) 1, longKey1).get(), value1);
        Assert.assertArrayEquals(st.get((byte) 1, longKey2).get(), value2);
    }

    /* delete is not implemented yet and throws NotImplementedException */
    @Test(expected = NotImplementedException.class)
    public void testDelete() throws Exception {
        Storage st = newStorage("storagecfg/memcachedstorage.cfg");

        byte[] key1 = UUIDUtils.longToBytes(33);
        st.delete((byte) 1, key1).get();
    }
}
