package ch.usi.paxosfs.replica;

import com.google.common.net.HostAndPort;

/**
 * Created by pacheco on 26.04.15.
 */
public interface ReplicaManager {
    /**
     * Initializes the ReplicaManager. Clients must be sure to call this method before usage. Implementations might throw ReplicaManagerException on errors.
     * @throws ReplicaManagerException
     */
    public void start() throws ReplicaManagerException;

    /**
     * Get a random replica address for the partition. Implementations might throw ReplicaManagerException on errors.
     * @param partition
     * @return A replica address. Returns null if there were no replicas for the partition.
     * @throws ReplicaManagerException
     */
    public HostAndPort getRandomReplicaAddress(byte partition) throws ReplicaManagerException;

    /**
     * Get a replica address for the partition. Implementations might throw ReplicaManagerException on errors.
     * @param partition
     * @param replicaId
     * @return A replica address. Returns null if the replica is not found.
     * @throws ReplicaManagerException
     */
    public HostAndPort getReplicaAddress(byte partition, int replicaId) throws ReplicaManagerException;

    public void waitInitialization() throws InterruptedException;
}
