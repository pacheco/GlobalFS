namespace java ch.usi.paxosfs.replica.commands

include "fuseops.thrift"

enum CommandType {
    ATTR = 0,
    GETDIR = 1,
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
    DEBUG = 17,
    READLINK = 18,
}

struct AttrCmd {
    1: string path

    2: set<byte> partition
}

struct GetdirCmd {
    1: string path
    
    2: set<byte> partition
}

struct MknodCmd {
    3: string path
    4: i32 mode
    5: i32 uid
    6: i32 gid

    7: set<byte> parentPartition
    8: set<byte> partition
}

struct MkdirCmd {
    3: string path
    4: i32 mode
    5: i32 uid
    6: i32 gid

    7: set<byte> parentPartition
    8: set<byte> partition
}

struct UnlinkCmd {
    3: string path

    7: set<byte> parentPartition
    8: set<byte> partition
}

struct RmdirCmd {
    3: string path

    7: set<byte> parentPartition
    8: set<byte> partition
}

struct SymlinkCmd {
    3: string target
    4: string path

    7: set<byte> parentPartition
    8: set<byte> partition
}

struct ReadlinkCmd {
    1: string path
    2: set<byte> partition
}

struct RenameCmd {
    3: string from
    4: string to

    7: set<byte> parentPartitionFrom
    8: set<byte> partitionFrom
    9: set<byte> parentPartitionTo
    10: set<byte> partitionTo
}

struct ChmodCmd {
    3: string path
    4: i32 mode

    7: set<byte> partition
}

struct ChownCmd {
    2: string path
    3: i32 uid
    4: i32 gid

    7: set<byte> partition
}

struct TruncateCmd {
    3: string path
    4: i64 size

    7: set<byte> partition
}

struct UtimeCmd {
    3: string path
    4: i32 atime
    5: i32 mtime

    7: set<byte> partition
}

struct OpenCmd {
    3: string path
    4: i32 flags

    7: set<byte> partition
}

struct ReadBlocksCmd {
    3: string path
    4: fuseops.FileHandle fileHandle
    5: i64 offset
    6: i64 bytes

    7: set<byte> partition
}

struct WriteBlocksCmd {
    3: string path
    4: fuseops.FileHandle fileHandle
    5: i64 offset
    6: list<fuseops.DBlock> blocks

    7: set<byte> partition
}

struct ReleaseCmd {
    3: string path
    4: fuseops.FileHandle fileHandle
    5: i32 flags

    7: set<byte> partition
}

struct StatFsCmd {
}

struct RenameData {
    1: i32 mode
    2: i32 rdev
    3: i32 uid
    4: i32 gid
    5: i64 size
    6: list<fuseops.DBlock> blocks
    7: i32 atime
    8: i32 mtime
    9: i32 ctime
}

struct Signal {
    1: byte fromPartition
    2: bool success
    3: optional RenameData renameData
    4: optional fuseops.FSError error
}

struct Command {
    1: i32 type
    2: i64 reqId
    3: i32 reqTime
    4: optional GetdirCmd getdir
    5: optional AttrCmd attr
    7: optional MknodCmd mknod
    8: optional MkdirCmd mkdir
    9: optional UnlinkCmd unlink
    10: optional RmdirCmd rmdir
    11: optional SymlinkCmd symlink
    26: optional ReadlinkCmd readlink
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
    23: optional fuseops.Debug debug
    24: set<byte> involvedPartitions
    25: optional map<byte, i64> instanceMap
}
