package ch.usi.paxosfs.client;

import java.util.Scanner;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FuseOps;

public class CommandLineClient {
	private static String replicaHost;
	private static int replicaPort;
	private static TTransport transport;
	private static FuseOps.Client client;

	public static void main(String[] args) throws FSError, TException {
		replicaHost = args[0];
		replicaPort = Integer.parseInt(args[1]);

		transport = new TSocket(replicaHost, replicaPort);
		transport.open();
		TProtocol protocol = new TBinaryProtocol(transport);
		client = new FuseOps.Client(protocol);

		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String cmd = s.next();
			try {
			switch (cmd) {
			case "statfs":
				System.out.println(client.statfs());
				break;
			case "getdir": {
				String path = s.next();
				System.out.println(client.getdir(path));
			}
				break;
			case "mknod": {
				String path = s.next();
				client.mknod(path, 0, 0, 0, 0);
				System.out.println("File created.");
			}
				break;
			case "getattr": {
				String path = s.next();
				System.out.println(client.getattr(path));
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
