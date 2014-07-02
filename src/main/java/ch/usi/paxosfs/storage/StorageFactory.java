package ch.usi.paxosfs.storage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class StorageFactory {
	public static Storage storageFromUrls(String... serverUrl) {
		return new HttpStorageClient(serverUrl);
	}
	
	public static Storage storageFromConfig(Path configFile) throws FileNotFoundException {
		List<String> hosts = new LinkedList<>();
		Scanner sc = new Scanner(new FileInputStream(configFile.toFile()));
		while (sc.hasNext()) {
			if (sc.hasNext("#.*")) {
				sc.nextLine();
				continue;
			}
			String ip = sc.next();
			Integer port = sc.nextInt();
			hosts.add("http://" + ip + ":" + port);
		}
		sc.close();
		return new HttpStorageClient(hosts.toArray(new String[hosts.size()]));
	}
}
