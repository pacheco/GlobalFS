package ch.usi.paxosfs.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

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
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.ReadResult;
import ch.usi.paxosfs.storage.HttpStorageClient;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.UnixConstants;

public class CommandLineClient {
	private static TTransport[] transport;
	private static FuseOps.Client[] client;
	private static ReplicaManager rm;
	private static PartitioningOracle oracle;

	public static void main(String[] args) throws FSError, TException, KeeperException, InterruptedException, IOException {
		int nReplicas = Integer.parseInt(args[0]);
		String zoohost = args[1];
		String storageHost = args[2];
		Storage storage = new HttpStorageClient(storageHost);
		
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
		FileHandle fh = null;
		while (s.hasNext()) {
			String cmd = s.next();
			try {
			switch (cmd) {
			case "statfs": {
				System.out.println(client[0].statfs());
				break;
			}
			case "getdir": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				System.out.println(client[partition].getdir(path));
				break;
			}
			case "mknod": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				client[partition].mknod(path, 0, 0, 0, 0);
				System.out.println("File created.");
				break;
			}
			case "getattr": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				System.out.println(client[partition].getattr(path));
				break;
			}
			case "mkdir": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				client[partition].mkdir(path, 0, 0, 0);
				System.out.println("Dir created.");
				break;
			}
			case "rmdir": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				client[partition].rmdir(path);
				System.out.println("Dir removed.");
				break;
			}
			case "unlink": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				client[partition].unlink(path);
				System.out.println("File removed.");
				break;
			}
			case "rename": {
				String from = s.next();
				String to = s.next();
				int partition = oracle.partitionsOf(from).iterator().next().intValue()-1;
				client[partition].rename(from, to);
				System.out.println("File renamed.");
				break;			
			}
			case "open": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				fh = client[partition].open(path, UnixConstants.O_RDWR.getValue());
				System.out.println(fh);
				break;
			}
			case "write": {
				String path = s.next();
				int offset = s.nextInt();
				String data = s.next();
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				List<DBlock> blocks = new ArrayList<DBlock>();
				blocks.add(new DBlock(null, 0, data.length()));
				blocks.get(0).setId(UUIDUtils.toBytes(UUID.randomUUID()));
				storage.put(blocks.get(0).getId(), data.getBytes());
				client[partition].writeBlocks(path, fh, offset, blocks);
				System.out.println("File written");
				break;
			}
			case "read": {
				String path = s.next();
				int offset = s.nextInt();
				int bytes = s.nextInt();
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				ReadResult rr = client[partition].readBlocks(path, fh, offset, bytes);
				for (DBlock b : rr.getBlocks()) {
//					System.out.println(new String(b.getId()));
					if (b.getId().length == 0) {
						System.out.print(new byte[(int)b.size()]);
					}
					System.out.print(new String(storage.get(b.getId())));
				}
				System.out.println("");
				break;
			}
			case "release": {
				String path = s.next();
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				client[partition].release(path, fh, 0);
				System.out.println("File closed");
				fh = null;
				break;
			}			
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
