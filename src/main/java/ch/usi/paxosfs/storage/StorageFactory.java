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

    /**
     * Instantiates the Storage client from a config file.
     * The first line of the config file should be the fully a qualified class name.
     * This class should implement the ch.usi.paxosfs.storage.Storage interface.
     * This method will instantiate the Storage and give it this same config file as argument.
     * Only the first line in the config file is required. The rest of its content is determined by the
     * Storage implementation.
     * @param configFile path to the configuration file
     * @return
     */
	public static Storage storageFromConfig(Path configFile) throws FileNotFoundException {
		List<String> hosts = new LinkedList<>();
		Scanner sc = new Scanner(new FileInputStream(configFile.toFile()));
		while (sc.hasNext()) {
			if (sc.hasNext("#.*") || sc.hasNext("replication")) {
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
