namespace java ch.usi.paxosfs.rpc
namespace py paxosfs

exception FSError {
	1: i32 errorCode,
	2: string errorMsg,
}

struct Attr {
    1: i64 inode,
    2: i32 mode,
    3: i32 nlink,
    4: i32 uid,
    5: i32 gid,
    6: i32 rdev,
    7: i32 atime,
    8: i32 mtime,
    9: i32 ctime,
    10: i64 size,
    11: i64 blocks,
}

struct DirEntry {
	1: string name,
	2: i32 inode,
	3: i32 mode,
}

struct FileSystemStats {
	1: i32 blockSize,
	2: i32 blocks,
	3: i32 blocksFree,
	4: i32 blocksAvail,
	5: i32 files,
	6: i32 filesFree,
	7: i32 namelen,
}

struct FileHandle {
	1: i32 id,
}

struct DBlock {
    1: i64 id,
    2: i64 startOffset,
    3: i64 endOffset,
}

// THIS METHOD HAS TO BE ADDED TO THE DBLOCK CLASS
//public long size() {
//    return this.getEndOffset() - this.getStartOffset();
//}


struct ReadResult {
	1: i64 len,
	2: list<DBlock> blocks,
}

service FuseOps {
	Attr getattr(1: string path) throws (1: FSError e),
	string readlink(1: string path) throws (1: FSError e),
	list<DirEntry> getdir(1: string path) throws (1: FSError e),
	void mknod(1: string path, 2: i32 mode, 3: i32 rdev, 4: i32 uid, 5: i32 gid) throws (1: FSError e),	
	void mkdir(1: string path, 2: i32 mode, 3: i32 uid, 4: i32 gid) throws (1: FSError e),
	void unlink(1: string path) throws (1: FSError e),
	void rmdir(1: string path) throws (1: FSError e),
	void symlink(1: string target, 2: string path, 3: i32 uid, 4: i32 gid) throws (1: FSError e),
	void rename(1: string fromPath, 2: string toPath) throws (1: FSError e),
	void chmod(1: string path, 2: i32 mode) throws (1: FSError e),
	void chown(1: string path, 2: i32 uid, 3: i32 gid) throws (1: FSError e),
	void truncate(1: string path, 2: i64 size) throws (1: FSError e),
	void utime(1: string path, 2: i64 atime, 3: i64 mtime) throws (1: FSError e),
	FileSystemStats statfs() throws (1: FSError e),
	FileHandle open(1: string path, 2: i32 flags) throws (1: FSError e),
	ReadResult readBlocks(1: string path, 2: FileHandle fh, 3: i64 offset, 4: i64 bytes) throws (1: FSError e),
	void writeBlocks(1: string path, 2: FileHandle fh, 3: i64 offset, 4: list<DBlock> blocks) throws (1: FSError e),
	void release(1: string path, 2: FileHandle fh, 3: i32 flags) throws (1: FSError e),
}
