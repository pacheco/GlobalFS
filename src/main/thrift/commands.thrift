namespace java ch.inf.paxosfs.replica.commands

include "fuseops.thrift"

enum CommandType {
    ATTR = 0,
    MKNOD = 2,
    MKDIR = 3,
    UNLINK = 4,
    RMDIR = 5,
    SYMLINK = 6,
    RENAME = 7,
    CHMOD = 8,
    CHOWN = 9,
    TRUNCATE = 10,
    UTIME = 11,
    OPEN = 12,
    READ_BLOCKS = 13,
    WRITE_BLOCKS = 14,
    RELEASE = 15,
    SIGNAL = 16,
}

struct AttrCmd {
    1: string path
}

struct MknodCmd {
    3: string path
    4: i32 mode
    5: i32 uid
    6: i32 gid
}

struct MkdirCmd {
    3: string path
    4: i32 mode
    5: i32 uid
    6: i32 gid
}

struct UnlinkCmd {
    3: string path
}

struct RmdirCmd {
    3: string path
}

struct SymlinkCmd {
    3: string target
    4: string path
    5: i32 uid
    6: i32 gid
}

struct RenameCmd {
    3: string from
    4: string to
}

struct ChmodCmd {
    3: string path
    4: i32 mode
}

struct ChownCmd {
    3: i32 uid
    4: i32 gid
}

struct TruncateCmd {
    3: string path
    4: i64 size
}

struct UtimeCmd {
    3: string path
    4: i32 atime
    5: i32 mtime
}

struct OpenCmd {
    3: string path
    4: i32 flags
}

struct ReadBlocksCmd {
    3: string path
    4: i32 fileHandle
    5: i64 offset
    6: i64 bytes
}

struct WriteBlocksCmd {
    3: string path
    4: i32 fileHandle
    5: i64 offset
    6: list<fuseops.DBlock> blocks
}

struct ReleaseCmd {
    3: string path
    4: i32 fileHandle
    5: i32 flags
}

struct RenameData {
    1: i32 mode
    2: i32 rdev
    3: i32 uid
    4: i32 gid
    5: i64 size
    6: list<i64> blocks
    7: i32 atime
    8: i32 mtime
    9: i32 ctime
}

struct StatFsCmd {
}

struct Signal {
    1: i32 fromPartition
    2: bool success
    3: optional RenameData renameData
}

struct Command {
    1: i32 type
    2: i64 reqId
    3: i32 reqTime
    4: list<i32> involvedPartitions
    5: optional AttrCmd attr
    7: optional MknodCmd mknod
    8: optional MkdirCmd mkdir
    9: optional UnlinkCmd unlink
    10: optional RmdirCmd rmdir
    11: optional SymlinkCmd symlink
    12: optional RenameCmd rename
    13: optional ChmodCmd chmod
    14: optional ChownCmd chown
    15: optional TruncateCmd truncate
    16: optional UtimeCmd utime
    17: optional OpenCmd open
    18: optional ReadBlocksCmd read
    19: optional WriteBlocksCmd write
    20: optional ReleaseCmd release
    21: optional StatFsCmd statfs
    22: optional Signal signal
}
