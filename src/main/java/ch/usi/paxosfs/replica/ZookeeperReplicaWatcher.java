package ch.usi.paxosfs.replica;

import com.google.common.net.HostAndPort;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Register and find replicas on zookeeper by partition.
 * 
 * @author pacheco
 * 
 */
public class ZookeeperReplicaWatcher implements ReplicaManager {
	private static String BASEPATH = "/paxosfs";

	private String zoohost;
	private Random random = new Random();
    private CuratorFramework zk;
    private TreeCache replicas;

    /**
	 * Clients use this constructor. The manager will be used only to find existing replicas.
	 * @param zoohost
	 */
	public ZookeeperReplicaWatcher(String zoohost) {
		this.zoohost = zoohost;
    }

	public void start() throws ReplicaManagerException {
        this.zk = CuratorFrameworkFactory.newClient(zoohost, new ExponentialBackoffRetry(500, 5));
        zk.start();
        try {
            boolean connected = zk.blockUntilConnected(10, TimeUnit.SECONDS);
            if (!connected) {
                throw new ReplicaManagerException("Could not connect to zookeeper");
            }
            replicas = new TreeCache(zk, BASEPATH);
            replicas.start();
        } catch (InterruptedException e) {
            throw new ReplicaManagerException(e);
        } catch (Exception e) {
            throw new ReplicaManagerException(e);
        }
    }

	public HostAndPort getRandomReplicaAddress(byte partition) throws ReplicaManagerException {
        Map <String, ChildData> partitionReplicas = replicas.getCurrentChildren(BASEPATH + "/" + partition);
        Object[] entries = partitionReplicas.entrySet().toArray();
        ChildData replica = (ChildData) entries[random.nextInt(entries.length)];
        if (replica != null) {
            String data = new String(replica.getData());
            return HostAndPort.fromString(data);
        } else {
            throw new ReplicaManagerException("No replica available");
        }
	}

	public HostAndPort getReplicaAddress(byte partition, int replicaId) throws ReplicaManagerException {
        ChildData replica = replicas.getCurrentData(BASEPATH + "/" + partition + "/" + replicaId);
        if (replica != null) {
            String data = new String(replica.getData());
            return HostAndPort.fromString(data);
        } else {
            throw new ReplicaManagerException("No replica available");
        }
    }
}
