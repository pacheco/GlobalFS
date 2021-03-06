package ch.usi.paxosfs.replica;

import com.google.common.net.HostAndPort;
import org.apache.zookeeper.*;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Register and find replicas on zookeeper by partition.
 * 
 * @author pacheco
 * 
 */
public class ZookeeperReplicaManager implements Watcher, ReplicaManager {
	private static String BASEPATH = "/paxosfs";

	private String zoohost;
	private ZooKeeper zk;
	private Random random = new Random();
	private byte partition;
	private int id;
	private String address;
	private boolean readonly = true;

	/**
	 * Clients use this constructor. The manager will be used only to find existing replicas.
	 * @param zoohost
	 */
	public ZookeeperReplicaManager(String zoohost) {
		this.zoohost = zoohost;
		this.readonly = true;
	}

	public ZookeeperReplicaManager(String zoohost, byte partition, int id, String address) {
		this.readonly = false;
		this.zoohost = zoohost;
		this.partition = partition;
		this.id = id;
		this.address = address;
	}

	public void start() throws ReplicaManagerException {
        try {
            this.zk = new ZooKeeper(this.zoohost, 3000, this);
        } catch (IOException e) {
            throw new ReplicaManagerException(e);
        }
    }

	private void registerReplica() throws KeeperException, InterruptedException {
		String path = BASEPATH;
		try {
			this.zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			if (e.code() == Code.NODEEXISTS) {
				// ignore
			} else {
				throw e;
			}
		}
		path += "/" + Byte.toString(partition);
		try {
			this.zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			if (e.code() == Code.NODEEXISTS) {
				// ignore
			} else {
				throw e;
			}
		}
		path += "/" + Integer.toString(id);
		try {
			this.zk.create(path, address.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		} catch (KeeperException e) {
			if (e.code() == Code.NODEEXISTS) {
				// ignore
			} else {
				throw e;
			}
		}
	}

	// FIXME: use watchers
	public HostAndPort getRandomReplicaAddress(byte partition) throws ReplicaManagerException {
        try {
            String path = BASEPATH + "/" + Byte.toString(partition);
            List<String> replicas = this.zk.getChildren(path, false);
            if (replicas.size() < 1) {
                return null;
            } else {
                String rep = replicas.get(this.random.nextInt(replicas.size()));
                path += "/" + rep;
                byte[] data = zk.getData(path, false, null);
                return HostAndPort.fromString(new String(data));
            }
        } catch (KeeperException | InterruptedException e) {
            throw new ReplicaManagerException(e);
        }
	}

	// FIXME: use watchers
	public HostAndPort getReplicaAddress(byte partition, int replicaId) throws ReplicaManagerException {
        try {
            String path = BASEPATH + "/" + Byte.toString(partition) + "/" + Integer.toString(replicaId);
            byte[] data = zk.getData(path, false, null);
            return HostAndPort.fromString(new String(data));
        } catch (KeeperException | InterruptedException e) {
            throw new ReplicaManagerException(e);
        }
	}

	@Override
	public void waitInitialization() throws InterruptedException {
		return;
	}

	@Override
	public void stop() {
		try {
			zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (this.readonly) { // no replica needs to be registered on zookeeper
			return;
		}
		if (event.getType() == EventType.None) {
			if (event.getState() == KeeperState.SyncConnected) {
				while (true) {
					try {
						this.registerReplica();
						break;
					} catch (KeeperException e) {
						break;
					} catch (InterruptedException e) {
						// try again
					}
				}
			}
		}
	}
}
