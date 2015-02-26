package ch.usi.paxosfs.storage;

import ch.usi.paxosfs.util.UUIDUtils;

import java.nio.file.FileSystems;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class StorageCmdClient {
	public static void main(String[] args) throws Exception {
		Storage storage = StorageFactory.storageFromConfig(FileSystems.getDefault().getPath(args[0]));

		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String cmd = s.next();
			switch (cmd) {
			case "get": {
				Long key = s.nextLong();
				String value;
				try {
					value = new String(storage.get((byte) 0, UUIDUtils.longToBytes(key)).get());
					System.out.println(value);
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				break;
			}
			case "put": {
				Long key = s.nextLong();
				String value = s.next();
                try {
        		    System.out.println(storage.put((byte) 0, UUIDUtils.longToBytes(key), value.getBytes()).get());
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				break;
			}
			default:
				s.nextLine();
				continue;
			}
			s.nextLine();
		}
		s.close();
	}
}
