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
	1: i64 id,
    2: i32 flags,
    3: byte partition,
}

struct DBlock {
    1: binary id,
    2: i32 startOffset,
    3: i32 endOffset,
    4: set<byte> storage,
}

// THIS METHOD HAS TO BE ADDED TO THE DBLOCK CLASS
//public long size() {
//    return this.getEndOffset() - this.getStartOffset();
//}


struct ReadResult {
	1: list<DBlock> blocks,
}

// has the instanceMap seen by the command plus optional fields with the response if the method has any (field is named after the method called)
struct Response {
    1: map<byte, i64> instanceMap
    2: optional Attr getattr
    3: optional string readlink
    4: optional list<DirEntry> getdir
    5: optional FileSystemStats statfs
    6: optional FileHandle open
    7: optional ReadResult readBlocks
}

service FuseOps {
	Response getattr(1: string path, 2: map<byte, i64> instanceMap) throws (1: FSError e),
	Response readlink(1: string path, 2: map<byte, i64> instanceMap) throws (1: FSError e),
	Response getdir(1: string path, 2: map<byte, i64> instanceMap) throws (1: FSError e),
	Response mknod(1: string path, 2: i32 mode, 3: i32 rdev, 4: i32 uid, 5: i32 gid, 6: map<byte, i64> instanceMap) throws (1: FSError e),	
	Response mkdir(1: string path, 2: i32 mode, 3: i32 uid, 4: i32 gid, 5: map<byte, i64> instanceMap) throws (1: FSError e),
	Response unlink(1: string path, 2: map<byte, i64> instanceMap) throws (1: FSError e),
	Response rmdir(1: string path, 2: map<byte, i64> instanceMap) throws (1: FSError e),
	Response symlink(1: string target, 2: string path, 3: i32 uid, 4: i32 gid, 5: map<byte, i64> instanceMap) throws (1: FSError e),
	Response rename(1: string fromPath, 2: string toPath, 3: map<byte, i64> instanceMap) throws (1: FSError e),
	Response chmod(1: string path, 2: i32 mode, 3: map<byte, i64> instanceMap) throws (1: FSError e),
	Response chown(1: string path, 2: i32 uid, 3: i32 gid, 4: map<byte, i64> instanceMap) throws (1: FSError e),
	Response truncate(1: string path, 2: i64 size, 3: map<byte, i64> instanceMap) throws (1: FSError e),
	Response utime(1: string path, 2: i64 atime, 3: i64 mtime, 4: map<byte, i64> instanceMap) throws (1: FSError e),
	Response statfs(1: map<byte, i64> instanceMap) throws (1: FSError e),
	Response open(1: string path, 2: i32 flags, 3: map<byte, i64> instanceMap) throws (1: FSError e),
	Response readBlocks(1: string path, 2: FileHandle fh, 3: i64 offset, 4: i64 bytes, 5: map<byte, i64> instanceMap) throws (1: FSError e),
	Response writeBlocks(1: string path, 2: FileHandle fh, 3: i64 offset, 4: list<DBlock> blocks, 5: map<byte, i64> instanceMap) throws (1: FSError e),
	Response release(1: string path, 2: FileHandle fh, 3: i32 flags, 4: map<byte, i64> instanceMap) throws (1: FSError e),
}
