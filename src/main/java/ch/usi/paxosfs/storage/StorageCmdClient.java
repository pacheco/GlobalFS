package ch.usi.paxosfs.storage;

import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import ch.usi.paxosfs.util.UUIDUtils;

public class StorageCmdClient {
	static List<Storage> storages;
	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException, ExecutionException {
		storages = new LinkedList<>();
		for (String storagePath: args) {
			if (storagePath.equals("http://fake")) {
				System.out.println("STORAGE: FAKE " + storagePath);
				storages.add(new FakeStorage());
			} else if (storagePath.startsWith("http://")) { // FIXME: simple hack so that i can test with a single storage without config files
				System.out.println("STORAGE: " + storagePath);
				storages.add(StorageFactory.storageFromUrls(storagePath));
			} else {
				storages.add(StorageFactory.storageFromConfig(FileSystems.getDefault().getPath(storagePath)));
			}
		}
		
		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String cmd = s.next();
			switch (cmd) {
			case "get": {
				Long key = s.nextLong();
				String value = new String(storages.get(0).get(UUIDUtils.longToBytes(key)).get());
				System.out.println(value);
				break;
			}
			case "put": {
				Long key = s.nextLong();
				String value = s.next();
				for (Storage storage: storages) {
					System.out.println(storage.put(UUIDUtils.longToBytes(key), value.getBytes()).get());
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
