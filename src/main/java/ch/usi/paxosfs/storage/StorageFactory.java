package ch.usi.paxosfs.storage;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class StorageFactory {
    /**
     * Instantiates the Storage client from a config file.
     * The first line of the config file should be the fully a qualified class name.
     * This class should implement the ch.usi.paxosfs.storage.Storage interface.
     * This method will instantiate the Storage and give it this same config file as argument.
     * Only the first line in the config file is required. The rest of its content is determined by the
     * Storage implementation.
     * @param configFile path to the configuration file
     * @return The Storage implementation. If there is some problem instantiating the class, returns null
     */
	public static Storage storageFromConfig(Path configFile) throws FileNotFoundException {
        List<String> hosts = new LinkedList<>();
        Storage st = null;
        Class clazz = null;
        try {
            BufferedReader r = new BufferedReader(new FileReader(configFile.toFile()));
            String className = r.readLine();
            clazz = Class.forName(className);
            if (Storage.class.isAssignableFrom(clazz)) { // check that it implements a Storage
                st = (Storage) clazz.newInstance();
                st.initialize(configFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            st = null;
        }

        return st;
	}
}
