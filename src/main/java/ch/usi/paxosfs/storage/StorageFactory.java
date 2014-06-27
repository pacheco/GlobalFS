package ch.usi.paxosfs.storage;

import java.nio.file.Path;

public class StorageFactory {
	public static Storage storageFromUrls(String... serverUrl) {
		return new HttpStorageClient(serverUrl);
	}
	
	public static Storage storageFromConfig(Path configFile) {
		// TODO: implement
		throw new RuntimeException("not implemented");
	}
}
