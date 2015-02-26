package ch.usi.paxosfs.partitioning;

import ch.usi.paxosfs.storage.*;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Created by pacheco on 24/02/15.
 */
public class StorageTest {
    Random rand = new Random();

    private Storage newStorage(String configFile) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(configFile);
        Path path = Paths.get(url.toURI());
        Storage st = StorageFactory.storageFromConfig(path);
        return st;
    }

    @Test
    public void testNullStorage() throws Exception {
        Storage st = newStorage("storagecfg/nullstorage.cfg");
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof NullStorage);

        // inserts and deletes always work
        byte[] key = UUIDUtils.longToBytes(33);
        byte[] value = UUIDUtils.longToBytes(99);
        Assert.assertTrue(st.put((byte)0, key, value).get());
        Assert.assertTrue(st.put((byte)99, key, value).get());
        Assert.assertTrue(st.delete((byte)1, key).get());
        Assert.assertTrue(st.delete((byte)3, key).get());

        // 'get' always returns the same value no matter what
        value = new byte[1024];
        Assert.assertArrayEquals(st.get((byte)0, key).get(), value);
        value = new byte[1024];
        Assert.assertArrayEquals(st.get((byte)1, key).get(), value);
        value = new byte[1024];
        Assert.assertArrayEquals(st.get((byte)2, key).get(), value);
    }

    @Test
    public void testLocalStorage() throws Exception {
        Storage st = newStorage("storagecfg/localstorage.cfg");
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof LocalStorage);

        // inserts always work
        byte[] key = UUIDUtils.longToBytes(33);
        byte[] key2 = UUIDUtils.longToBytes(66);
        byte[] value = UUIDUtils.longToBytes(99);
        byte[] value2 = UUIDUtils.longToBytes(555);
        Assert.assertTrue(st.put((byte) 0, key, value).get());
        Assert.assertTrue(st.put((byte) 0, key, value2).get());
        Assert.assertTrue(st.put((byte) 99, key, value).get());

        // gets work for existing keys. Returns null otherwise
        Assert.assertArrayEquals(st.get((byte) 0, key).get(), value2);
        Assert.assertArrayEquals(st.get((byte) 99, key).get(), value);
        Assert.assertNull(st.get((byte) 3, key).get()); // non-existent partition
        Assert.assertNull(st.get((byte) 0, key2).get()); // non-existent key

        // deletes work when the key exists
        Assert.assertTrue(st.delete((byte) 0, key).get());  // two deletes in a row
        Assert.assertFalse(st.delete((byte) 0, key).get());
        Assert.assertTrue(st.delete((byte) 99, key).get()); // two deletes in a row
        Assert.assertFalse(st.delete((byte) 99, key).get());
        Assert.assertFalse(st.delete((byte) 3, key).get()); // non-existent partition
        Assert.assertFalse(st.delete((byte) 99, key2).get()); // non-existent key
    }

    @Test
    public void testHttpStorage() throws Exception {
        Storage st = newStorage("storagecfg/httpstorage.cfg");
        Assert.assertNotNull(st);
        Assert.assertTrue(st instanceof HttpStorage);

        Process store1 = Runtime.getRuntime().exec("src/scripts/dht-fake.py 15001");
        Process store2 = Runtime.getRuntime().exec("src/scripts/dht-fake.py 15002");
        Process store3 = Runtime.getRuntime().exec("src/scripts/dht-fake.py 15003");

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


        byte[] nonExistentKey = UUIDUtils.longToBytes(4356);
//
//        // gets work for existing keys. Returns null otherwise
//        Assert.assertArrayEquals(st.get((byte) 0, key).get(), value2);
//        Assert.assertArrayEquals(st.get((byte) 99, key).get(), value);
//        Assert.assertNull(st.get((byte) 3, key).get()); // non-existent partition
//        Assert.assertNull(st.get((byte) 0, key2).get()); // non-existent key
//
//        // deletes work when the key exists
//        Assert.assertTrue(st.delete((byte) 0, key).get());  // two deletes in a row
//        Assert.assertFalse(st.delete((byte) 0, key).get());
//        Assert.assertTrue(st.delete((byte) 99, key).get()); // two deletes in a row
//        Assert.assertFalse(st.delete((byte) 99, key).get());
//        Assert.assertFalse(st.delete((byte) 3, key).get()); // non-existent partition
//        Assert.assertFalse(st.delete((byte) 99, key2).get()); // non-existent key

        store1.destroy();
        store2.destroy();
        store3.destroy();
    }
}
