package ch.usi.paxosfs.replica;

import com.google.common.net.HostAndPort;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by pacheco on 26.04.15.
 */
public class StaticReplicaManagerTest {
    @Test
    public void fromConfig() throws IOException, URISyntaxException, ReplicaManagerException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("replicacfg/3part.cfg");
        Path path = Paths.get(url.toURI());
        StaticReplicaManager rm = StaticReplicaManager.fromConfig(path);

        Assert.assertNotNull(rm);
        // non existing replicas
        Assert.assertNull(rm.getReplicaAddress((byte) 3, 0));
        Assert.assertNull(rm.getReplicaAddress((byte) 1, 2));
        Assert.assertNull(rm.getRandomReplicaAddress((byte) 3));

        HostAndPort addr;

        // specific replicas
        addr = rm.getReplicaAddress((byte) 1, 0);
        Assert.assertEquals("1.2.3.1:20000", addr.toString());
        addr = rm.getReplicaAddress((byte) 1, 1);
        Assert.assertEquals("1.2.3.2:20000", addr.toString());
        addr = rm.getReplicaAddress((byte) 2, 0);
        Assert.assertEquals("1.2.3.3:20000", addr.toString());
        addr = rm.getReplicaAddress((byte) 2, 1);
        Assert.assertEquals("1.2.3.4:20000", addr.toString());
        // random replica
        addr = rm.getRandomReplicaAddress((byte) 1);
        Assert.assertNotNull(addr);
        addr = rm.getRandomReplicaAddress((byte) 2);
        Assert.assertNotNull(addr);
    }
}
