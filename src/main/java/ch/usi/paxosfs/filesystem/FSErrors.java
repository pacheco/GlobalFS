package ch.usi.paxosfs.filesystem;

import ch.usi.paxosfs.rpc.FSError;
import fuse.FuseException;

/**
 * Created by pacheco on 9/16/15.
 */
public class FSErrors {
    public static FSError notFound(String file) {
        return new FSError(FuseException.ENOENT, "No such file or directory");
    }

    public static FSError notDir(String file) {
        return new FSError(FuseException.ENOTDIR, "Not a directory");
    }

    public static FSError alreadyExists(String file) {
        return new FSError(FuseException.EEXIST, "File already exists");
    }

    public static FSError isDir(String file) {
        return new FSError(FuseException.EISDIR, "Is a directory");
    }

    public static FSError notEmpty(String path) {
        return new FSError(FuseException.ENOTEMPTY, "Not empty");
    }
}
