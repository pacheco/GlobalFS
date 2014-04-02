package ch.usi.paxosfs.client;

import org.apache.thrift.TException;

import ch.usi.paxosfs.replica.FSMain;

public class FuseWithReplica {
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
		
		PaxosFileSystem.main(new String[]{"localhost", "7777", "/Users/pacheco/fake"});
	}
}
