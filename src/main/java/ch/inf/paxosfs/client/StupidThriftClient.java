package ch.inf.paxosfs.client;

import java.util.Scanner;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import ch.inf.paxosfs.replica.FSMain;
import ch.inf.paxosfs.rpc.FSError;
import ch.inf.paxosfs.rpc.FuseOps;

public class StupidThriftClient {
	private static void runServer(final String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				FSMain.main(args);
			}
		}).start();
	}

	public static void main(String[] args) throws InterruptedException, TException {
		runServer(args);
		Thread.sleep(3000);
		
		TTransport transport = new TSocket("localhost", 7777);
		transport.open();
		TProtocol protocol = new TBinaryProtocol(transport);
		FuseOps.Client client = new FuseOps.Client(protocol);
		Scanner in = new Scanner(System.in);
		System.out.println(in.delimiter());
		while (in.hasNext()) {
			String cmd = in.next();
			if (cmd.equals("attr")) {
				try {
					System.out.println(client.getattr(in.next()));
				} catch (FSError e) {
					e.printStackTrace();
				}
			}
			in.nextLine();
		}
		in.close();
	}
}
