package ch.usi.paxosfs.filesystem.rocksdb;

import ch.usi.paxosfs.filesystem.*;
import ch.usi.paxosfs.rpc.DBlock;
import ch.usi.paxosfs.rpc.FSError;
import org.rocksdb.RocksDB;

/**
 * Created by pacheco on 9/16/15.
 */
public class RocksDBFileSystem implements FileSystem {
    RocksDB db;

    public RocksDBFileSystem() {
    }

    @Override
    public FileNode createFile(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
        return null;
    }

    @Override
    public DirNode createDir(String absolutePath, int mode, int time, int uid, int gid) throws FSError {
        return null;
    }

    @Override
    public LinkNode createLink(String absolutePath, String absoluteTarget) throws FSError {
        return null;
    }

    @Override
    public Node get(String path) throws FSError {
        return null;
    }

    @Override
    public Node getRoot() {
        return null;
    }

    @Override
    public Node removeFileOrLink(String path) throws FSError {
        return null;
    }

    @Override
    public DirNode removeDir(String path) throws FSError {
        return null;
    }

    @Override
    public Node rename(String from, String to) throws FSError {
        return null;
    }

    @Override
    public DirNode getDir(String path) throws FSError {
        return null;
    }

    @Override
    public FileNode getFile(String path) throws FSError {
        return null;
    }

    @Override
    public void setFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError {

    }

    @Override
    public void appendFileData(String absolutePath, Iterable<DBlock> blocks) throws FSError {

    }

    @Override
    public void updateFileData(String absolutePath, Iterable<DBlock> blocks, long offset) throws FSError {

    }

    @Override
    public void truncateFile(String absolutePath, long size) throws FSError {

    }
}
