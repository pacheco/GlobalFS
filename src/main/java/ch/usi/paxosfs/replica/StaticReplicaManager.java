package ch.usi.paxosfs.replica;

import com.google.common.net.HostAndPort;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * Register and find replicas on zookeeper by partition.
 * 
 * @author pacheco
 * 
 */
public class StaticReplicaManager implements ReplicaManager {
    private final Map<Byte, Map<Integer, HostAndPort>> partitionReplicas;
    private static final Random rand = new Random();

    StaticReplicaManager() {
        this.partitionReplicas = new HashMap<>();
    }

    StaticReplicaManager(Map<Byte, Map<Integer, HostAndPort>> partitionReplicas) {
        this.partitionReplicas = partitionReplicas;
    }

    /**
     * Add a replica to the ReplicaManager.
     * @param partition
     * @param id
     * @param addr
     */
    public void addReplica(byte partition, int id, HostAndPort addr) {
        Map<Integer, HostAndPort> replicas = this.partitionReplicas.get(Byte.valueOf(partition));
        if (replicas == null) {
            replicas = new HashMap<Integer, HostAndPort>();
            this.partitionReplicas.put(Byte.valueOf(partition), replicas);
        }
        replicas.put(Integer.valueOf(id), addr);
    }

    /**
     * Read replicas from a config file with lines of the format:
     *
     * partition replica_id host:port
     *
     * Example:
     *
     * 1 0 1.2.3.1:20000
     * 1 1 1.2.3.2:20000
     * 2 0 1.2.3.3:20000
     * 2 1 1.2.3.4:20000
     *
     * @param configFile Path to the configuration file
     * @return a StaticReplicaManager created from the configuration file
     * @throws IOException
     */
    public static StaticReplicaManager fromConfig(Path configFile) throws IOException {
        StaticReplicaManager rm = new StaticReplicaManager();

        Scanner sc = new Scanner(configFile);

        while (sc.hasNext()) {
            if (sc.hasNext("#.*")) {
                sc.nextLine();
                continue;
            }
            Byte partition = Byte.valueOf(sc.nextByte());
            Integer id = Integer.valueOf(sc.nextInt());
            String addr = sc.next();
            rm.addReplica(partition, id, HostAndPort.fromString(addr));
        }
        sc.close();
        return rm;
    }

    @Override
    public void start() throws ReplicaManagerException {}


    @Override
    public HostAndPort getRandomReplicaAddress(byte partition) throws ReplicaManagerException {
        Map<Integer, HostAndPort> replicas = partitionReplicas.get(Byte.valueOf(partition));
        if (replicas == null) return null;
        return replicas.get(rand.nextInt(replicas.size()));
    }

    @Override
    public HostAndPort getReplicaAddress(byte partition, int replicaId) throws ReplicaManagerException {
        Map<Integer, HostAndPort> replicas = partitionReplicas.get(Byte.valueOf(partition));
        if (replicas == null) return null;
        return replicas.get(Integer.valueOf(replicaId));
    }

    @Override
    public void waitInitialization() throws InterruptedException {}

    @Override
    public void stop() {}
}
