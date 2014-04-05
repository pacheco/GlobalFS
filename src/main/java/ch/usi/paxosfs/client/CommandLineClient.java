package ch.usi.paxosfs.client;

import java.io.IOException;
import java.util.Scanner;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.zookeeper.KeeperException;

import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.partitioning.SinglePartitionOracle;
import ch.usi.paxosfs.partitioning.TwoPartitionOracle;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FuseOps;

public class CommandLineClient {
	private static TTransport[] transport;
	private static FuseOps.Client[] client;
	private static ReplicaManager rm;
	private static PartitioningOracle oracle;

	public static void main(String[] args) throws FSError, TException, KeeperException, InterruptedException, IOException {
		int nReplicas = Integer.parseInt(args[0]);
		String zoohost = args[1];
		
		rm = new ReplicaManager(zoohost);
		rm.start();

		if (nReplicas == 1) {
			oracle = new SinglePartitionOracle();
		} else if (nReplicas == 2){
			oracle = new TwoPartitionOracle("/a", "/b");
		} else {
			System.err.println("Only supports 1 or 2 partitions");
			return;
		}
		
		transport = new TTransport[nReplicas];
		client = new FuseOps.Client[nReplicas];
		
		for (byte i=1; i<=nReplicas; i++) {
			String replicaAddr = rm.getRandomReplicaAddress(i);
			String replicaHost = replicaAddr.split(":")[0];
			int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
			
			transport[i-1] = new TSocket(replicaHost, replicaPort);
			transport[i-1].open();
			TProtocol protocol = new TBinaryProtocol(transport[i-1]);
			client[i-1] = new FuseOps.Client(protocol);
		}


		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String cmd = s.next();
			try {
			switch (cmd) {
			case "statfs":
				System.out.println(client[0].statfs());
				break;
			case "getdir": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				System.out.println(client[partition].getdir(path));
			}
				break;
			case "mknod": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				client[partition].mknod(path, 0, 0, 0, 0);
				System.out.println("File created.");
			}
				break;
			case "getattr": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				System.out.println(client[partition].getattr(path));
			}
				break;
			default:
				System.out.println("Unknown command");
			}
			} catch (FSError e) {
				System.out.println(e.getErrorMsg());
			}
			s.nextLine();
		}
		s.close();
	}
}
