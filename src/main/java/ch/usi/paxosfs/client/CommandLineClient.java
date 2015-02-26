package ch.usi.paxosfs.client;

import ch.usi.paxosfs.partitioning.DefaultMultiPartitionOracle;
import ch.usi.paxosfs.partitioning.PartitioningOracle;
import ch.usi.paxosfs.replica.ReplicaManager;
import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.storage.Storage;
import ch.usi.paxosfs.storage.StorageFactory;
import ch.usi.paxosfs.util.PathsNIO;
import ch.usi.paxosfs.util.UUIDUtils;
import ch.usi.paxosfs.util.UnixConstants;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.nio.file.FileSystems;
import java.util.*;

public class CommandLineClient {
	private static TTransport[] transport;
	private static FuseOps.Client[] client;
	private static ReplicaManager rm;
	private static PartitioningOracle oracle;
	private static Map<Byte, Long> instanceMap = new HashMap<Byte, Long>(); 
	
	private static class PathCompleter implements Completer {
		@Override
		public int complete(String buffer, int cursor,
				List<CharSequence> candidates) {
			String[] parts = buffer.substring(0,cursor).split("\\s+");
			
			String origPath = parts[parts.length-1];
			String path = PathsNIO.clean(origPath);
			String dir;
			String name;
			if (origPath.endsWith("/")) {
				dir = path;
				name = "";
			} else {
				dir = PathsNIO.dirname(path);
				name = PathsNIO.basename(path);
			}
			int partition = oracle.partitionsOf(dir).iterator().next().intValue()-1;
			try {
				Response r = client[partition].getdir(dir, instanceMap);
				instanceMap.putAll(r.instanceMap);
				List<DirEntry> entries = r.getGetdir();
				for (DirEntry e: entries) {
					if (e.getName().startsWith(name)){
						// fix for root "/"
						if (dir.endsWith("/")) {
							candidates.add(dir + e.getName());
						} else {
							candidates.add(dir + "/" + e.getName());
						}
					}
				}
			} catch (TException e) {
			}
			return cursor - origPath.length();
		}
		
	}

	public static void main(String[] args) throws Exception {
		Random rand = new Random();
		
		if (args.length != 3) {
			System.err.println("client <npartitions> <zkhost> <storageCfg>");
			System.exit(1);
		}
		
		int nPartitions = Integer.parseInt(args[0]);
		String zoohost = args[1];
		String storageCfg = args[2];
		Storage storage = StorageFactory.storageFromConfig(FileSystems.getDefault().getPath(storageCfg));
		
		rm = new ReplicaManager(zoohost);
		rm.start();

		oracle = new DefaultMultiPartitionOracle(nPartitions);
		
		transport = new TTransport[nPartitions];
		client = new FuseOps.Client[nPartitions];
		
		for (byte i=1; i<=nPartitions; i++) {
			String replicaAddr = rm.getReplicaAddress(i, 0);
			String replicaHost = replicaAddr.split(":")[0];
			int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
			
			transport[i-1] = new TSocket(replicaHost, replicaPort);
			transport[i-1].open();
			TProtocol protocol = new TBinaryProtocol(transport[i-1]);
			client[i-1] = new FuseOps.Client(protocol);
		}


		ConsoleReader reader = new ConsoleReader();
		reader.setPrompt("> ");
		reader.addCompleter(new StringsCompleter("statfs", "getdir", "mknod", "getattr", 
				"mkdir", "rmdir", "unlink", "rename", 
				"open", "write", "read", "release"));
		reader.addCompleter(new PathCompleter());
		
		String line;
		FileHandle fh = null;
		while((line = reader.readLine()) != null) {
			String[] parts = line.split("\\s+");
			String cmd = parts[0];

			try {
			switch (cmd) {
			case "statfs": {
				Response r = client[0].statfs(instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println(r.statfs);
				break;
			}
			case "getdir": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].getdir(path, instanceMap);
				System.out.println(r.getdir);
				instanceMap.putAll(r.instanceMap);
				break;
			}
			case "mknod": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].mknod(path, 0, 0, 0, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("File created.");
				break;
			}
			case "getattr": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].getattr(path, instanceMap);
				System.out.println(r.getattr);
				instanceMap.putAll(r.instanceMap);
				break;
			}
			case "mkdir": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].mkdir(path, 0, 0, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("Dir created.");
				break;
			}
			case "rmdir": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].rmdir(path, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("Dir removed.");
				break;
			}
			case "unlink": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].unlink(path, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("File removed.");
				break;
			}
			case "rename": {
				if (parts.length < 3) continue;
				String from = parts[1];
				String to = parts[2];
				int partition = oracle.partitionsOf(from).iterator().next().intValue()-1;
				Response r = client[partition].rename(from, to, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("File renamed.");
				break;			
			}
			case "open": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].open(path, UnixConstants.O_RDWR.getValue(), instanceMap);
				fh = r.open;
				instanceMap.putAll(r.instanceMap);
				System.out.println(fh);
				break;
			}
			case "write": {
				if (parts.length < 4) continue;
				String path = parts[1];
				int offset = Integer.parseInt(parts[2]);
				String data = parts[3];
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				List<DBlock> blocks = new ArrayList<DBlock>();
				blocks.add(new DBlock(null, 0, data.length(), new HashSet<Byte>()));
				blocks.get(0).setId(UUIDUtils.longToBytes(rand.nextLong()));
				storage.put((byte) 0, blocks.get(0).getId(), data.getBytes()).get();
				Response r = client[partition].writeBlocks(path, fh, offset, blocks, instanceMap);
				instanceMap.putAll(r.instanceMap);
				System.out.println("File written");
				break;
			}
			case "read": {
				if (parts.length < 4) continue;
				String path = parts[1];
				int offset = Integer.parseInt(parts[2]);
				int bytes = Integer.parseInt(parts[3]);
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				Response r = client[partition].readBlocks(path, fh, offset, bytes, instanceMap);
				ReadResult rr = r.readBlocks;
				instanceMap.putAll(r.instanceMap);
				for (DBlock b : rr.getBlocks()) {
//					System.out.println(new String(b.getId()));
					if (b.getId().length == 0) {
						System.out.print(new byte[(int)b.size()]);
					}
					System.out.print(new String(storage.get((byte) 0, b.getId()).get()));
				}
				System.out.println("");
				break;
			}
			case "release": {
				if (parts.length < 2) continue;
				String path = parts[1];
				int partition = oracle.partitionsOf(path).iterator().next().intValue()-1;
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				Response r = client[partition].release(path, fh, 0, instanceMap);
				instanceMap.putAll(r.instanceMap);
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
		}
	}
}
