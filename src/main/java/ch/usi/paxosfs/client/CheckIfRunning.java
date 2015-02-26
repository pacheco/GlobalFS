package ch.usi.paxosfs.client;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.DebugCommands;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.Debug;
import ch.usi.paxosfs.rpc.FuseOps;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;

/**
 * Checks if paxos is running by sending NULL commands to each partition (and the global ring)
 */
public class CheckIfRunning {
    public static void main(String[] args) {
        ReplicaManager rm;

        int nPartitions = Integer.parseInt(args[0]);
        String zooHost = args[1];

        try {
            rm = new ReplicaManager(zooHost);
            rm.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        PartitioningOracle oracle = new DefaultMultiPartitionOracle(nPartitions);

        /* For each partition, connect to replica 0 and send a command */
        for (byte i=1; i<=nPartitions; i++) {
            String replicaAddr;
            try {
                replicaAddr = rm.getReplicaAddress(i, 0);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
                return;
            }
            String replicaHost = replicaAddr.split(":")[0];
            int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);

            TTransport transport = new TSocket(replicaHost, replicaPort);
            try {
                transport.open();
            } catch (TTransportException e) {
                e.printStackTrace();
                System.exit(1);
            }
            TProtocol protocol = new TBinaryProtocol(transport);
            FuseOps.Client client = new FuseOps.Client(protocol);

            // Send the command
            Debug debugCmd = new Debug();
            debugCmd.setType(DebugCommands.NULL.getId());
            debugCmd.putToData("partition", Integer.toString(i));

            try {
                client.debug(debugCmd);
            } catch (TException e) {
                e.printStackTrace();
                System.exit(1);
            }
            transport.close();
        }
        System.exit(0);
    }
}
