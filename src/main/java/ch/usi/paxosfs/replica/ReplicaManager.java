package ch.usi.paxosfs.replica;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * Register and find replicas on zookeeper by partition.
 * 
 * @author pacheco
 * 
 */
public class ReplicaManager implements Watcher {
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
	public ReplicaManager(String zoohost) {
		this.zoohost = zoohost;
		this.readonly = true;
	}

	public ReplicaManager(String zoohost, byte partition, int id, String address) {
		this.readonly = false;
		this.zoohost = zoohost;
		this.partition = partition;
		this.id = id;
		this.address = address;
	}

	public void start() throws IOException {
		this.zk = new ZooKeeper(this.zoohost, 3000, this);
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

	public String getRandomReplicaAddress(byte partition) throws KeeperException, InterruptedException {
		String path = BASEPATH + "/" + Byte.toString(partition);
		List<String> replicas = this.zk.getChildren(path, false);
		if (replicas.size() < 1) {
			return null;
		} else {
			String rep = replicas.get(this.random.nextInt(replicas.size()));
			path += "/" + rep;
			byte[] data = zk.getData(path, false, null);
			return new String(data);
		}
	}

	public String getReplicaAddress(byte partition, int replicaId) throws KeeperException, InterruptedException {
		String path = BASEPATH + "/" + Byte.toString(partition) + "/" + Integer.toString(replicaId);
		byte[] data = zk.getData(path, false, null);
		return new String(data);
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
