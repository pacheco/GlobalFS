/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.usi.paxosfs.replica.commands;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum CommandType implements org.apache.thrift.TEnum {
  ATTR(0),
  GETDIR(1),
  MKNOD(2),
  MKDIR(3),
  UNLINK(4),
  RMDIR(5),
  SYMLINK(6),
  RENAME(7),
  CHMOD(8),
  CHOWN(9),
  TRUNCATE(10),
  UTIME(11),
  OPEN(12),
  READ_BLOCKS(13),
  WRITE_BLOCKS(14),
  RELEASE(15),
  SIGNAL(16),
  DEBUG(17);

  private final int value;

  private CommandType(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static CommandType findByValue(int value) { 
    switch (value) {
      case 0:
        return ATTR;
      case 1:
        return GETDIR;
      case 2:
        return MKNOD;
      case 3:
        return MKDIR;
      case 4:
        return UNLINK;
      case 5:
        return RMDIR;
      case 6:
        return SYMLINK;
      case 7:
        return RENAME;
      case 8:
        return CHMOD;
      case 9:
        return CHOWN;
      case 10:
        return TRUNCATE;
      case 11:
        return UTIME;
      case 12:
        return OPEN;
      case 13:
        return READ_BLOCKS;
      case 14:
        return WRITE_BLOCKS;
      case 15:
        return RELEASE;
      case 16:
        return SIGNAL;
      case 17:
        return DEBUG;
      default:
        return null;
    }
  }
}
