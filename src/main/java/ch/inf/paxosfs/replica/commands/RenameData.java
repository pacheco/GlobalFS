/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.inf.paxosfs.replica.commands;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameData implements org.apache.thrift.TBase<RenameData, RenameData._Fields>, java.io.Serializable, Cloneable, Comparable<RenameData> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("RenameData");

  private static final org.apache.thrift.protocol.TField MODE_FIELD_DESC = new org.apache.thrift.protocol.TField("mode", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField RDEV_FIELD_DESC = new org.apache.thrift.protocol.TField("rdev", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField UID_FIELD_DESC = new org.apache.thrift.protocol.TField("uid", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField GID_FIELD_DESC = new org.apache.thrift.protocol.TField("gid", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField SIZE_FIELD_DESC = new org.apache.thrift.protocol.TField("size", org.apache.thrift.protocol.TType.I64, (short)5);
  private static final org.apache.thrift.protocol.TField BLOCKS_FIELD_DESC = new org.apache.thrift.protocol.TField("blocks", org.apache.thrift.protocol.TType.LIST, (short)6);
  private static final org.apache.thrift.protocol.TField ATIME_FIELD_DESC = new org.apache.thrift.protocol.TField("atime", org.apache.thrift.protocol.TType.I32, (short)7);
  private static final org.apache.thrift.protocol.TField MTIME_FIELD_DESC = new org.apache.thrift.protocol.TField("mtime", org.apache.thrift.protocol.TType.I32, (short)8);
  private static final org.apache.thrift.protocol.TField CTIME_FIELD_DESC = new org.apache.thrift.protocol.TField("ctime", org.apache.thrift.protocol.TType.I32, (short)9);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new RenameDataStandardSchemeFactory());
    schemes.put(TupleScheme.class, new RenameDataTupleSchemeFactory());
  }

  public int mode; // required
  public int rdev; // required
  public int uid; // required
  public int gid; // required
  public long size; // required
  public List<Long> blocks; // required
  public int atime; // required
  public int mtime; // required
  public int ctime; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    MODE((short)1, "mode"),
    RDEV((short)2, "rdev"),
    UID((short)3, "uid"),
    GID((short)4, "gid"),
    SIZE((short)5, "size"),
    BLOCKS((short)6, "blocks"),
    ATIME((short)7, "atime"),
    MTIME((short)8, "mtime"),
    CTIME((short)9, "ctime");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // MODE
          return MODE;
        case 2: // RDEV
          return RDEV;
        case 3: // UID
          return UID;
        case 4: // GID
          return GID;
        case 5: // SIZE
          return SIZE;
        case 6: // BLOCKS
          return BLOCKS;
        case 7: // ATIME
          return ATIME;
        case 8: // MTIME
          return MTIME;
        case 9: // CTIME
          return CTIME;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __MODE_ISSET_ID = 0;
  private static final int __RDEV_ISSET_ID = 1;
  private static final int __UID_ISSET_ID = 2;
  private static final int __GID_ISSET_ID = 3;
  private static final int __SIZE_ISSET_ID = 4;
  private static final int __ATIME_ISSET_ID = 5;
  private static final int __MTIME_ISSET_ID = 6;
  private static final int __CTIME_ISSET_ID = 7;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.MODE, new org.apache.thrift.meta_data.FieldMetaData("mode", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.RDEV, new org.apache.thrift.meta_data.FieldMetaData("rdev", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.UID, new org.apache.thrift.meta_data.FieldMetaData("uid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.GID, new org.apache.thrift.meta_data.FieldMetaData("gid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.SIZE, new org.apache.thrift.meta_data.FieldMetaData("size", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.BLOCKS, new org.apache.thrift.meta_data.FieldMetaData("blocks", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64))));
    tmpMap.put(_Fields.ATIME, new org.apache.thrift.meta_data.FieldMetaData("atime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MTIME, new org.apache.thrift.meta_data.FieldMetaData("mtime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.CTIME, new org.apache.thrift.meta_data.FieldMetaData("ctime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(RenameData.class, metaDataMap);
  }

  public RenameData() {
  }

  public RenameData(
    int mode,
    int rdev,
    int uid,
    int gid,
    long size,
    List<Long> blocks,
    int atime,
    int mtime,
    int ctime)
  {
    this();
    this.mode = mode;
    setModeIsSet(true);
    this.rdev = rdev;
    setRdevIsSet(true);
    this.uid = uid;
    setUidIsSet(true);
    this.gid = gid;
    setGidIsSet(true);
    this.size = size;
    setSizeIsSet(true);
    this.blocks = blocks;
    this.atime = atime;
    setAtimeIsSet(true);
    this.mtime = mtime;
    setMtimeIsSet(true);
    this.ctime = ctime;
    setCtimeIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public RenameData(RenameData other) {
    __isset_bitfield = other.__isset_bitfield;
    this.mode = other.mode;
    this.rdev = other.rdev;
    this.uid = other.uid;
    this.gid = other.gid;
    this.size = other.size;
    if (other.isSetBlocks()) {
      List<Long> __this__blocks = new ArrayList<Long>(other.blocks);
      this.blocks = __this__blocks;
    }
    this.atime = other.atime;
    this.mtime = other.mtime;
    this.ctime = other.ctime;
  }

  public RenameData deepCopy() {
    return new RenameData(this);
  }

  @Override
  public void clear() {
    setModeIsSet(false);
    this.mode = 0;
    setRdevIsSet(false);
    this.rdev = 0;
    setUidIsSet(false);
    this.uid = 0;
    setGidIsSet(false);
    this.gid = 0;
    setSizeIsSet(false);
    this.size = 0;
    this.blocks = null;
    setAtimeIsSet(false);
    this.atime = 0;
    setMtimeIsSet(false);
    this.mtime = 0;
    setCtimeIsSet(false);
    this.ctime = 0;
  }

  public int getMode() {
    return this.mode;
  }

  public RenameData setMode(int mode) {
    this.mode = mode;
    setModeIsSet(true);
    return this;
  }

  public void unsetMode() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MODE_ISSET_ID);
  }

  /** Returns true if field mode is set (has been assigned a value) and false otherwise */
  public boolean isSetMode() {
    return EncodingUtils.testBit(__isset_bitfield, __MODE_ISSET_ID);
  }

  public void setModeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MODE_ISSET_ID, value);
  }

  public int getRdev() {
    return this.rdev;
  }

  public RenameData setRdev(int rdev) {
    this.rdev = rdev;
    setRdevIsSet(true);
    return this;
  }

  public void unsetRdev() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __RDEV_ISSET_ID);
  }

  /** Returns true if field rdev is set (has been assigned a value) and false otherwise */
  public boolean isSetRdev() {
    return EncodingUtils.testBit(__isset_bitfield, __RDEV_ISSET_ID);
  }

  public void setRdevIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __RDEV_ISSET_ID, value);
  }

  public int getUid() {
    return this.uid;
  }

  public RenameData setUid(int uid) {
    this.uid = uid;
    setUidIsSet(true);
    return this;
  }

  public void unsetUid() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __UID_ISSET_ID);
  }

  /** Returns true if field uid is set (has been assigned a value) and false otherwise */
  public boolean isSetUid() {
    return EncodingUtils.testBit(__isset_bitfield, __UID_ISSET_ID);
  }

  public void setUidIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __UID_ISSET_ID, value);
  }

  public int getGid() {
    return this.gid;
  }

  public RenameData setGid(int gid) {
    this.gid = gid;
    setGidIsSet(true);
    return this;
  }

  public void unsetGid() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __GID_ISSET_ID);
  }

  /** Returns true if field gid is set (has been assigned a value) and false otherwise */
  public boolean isSetGid() {
    return EncodingUtils.testBit(__isset_bitfield, __GID_ISSET_ID);
  }

  public void setGidIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __GID_ISSET_ID, value);
  }

  public long getSize() {
    return this.size;
  }

  public RenameData setSize(long size) {
    this.size = size;
    setSizeIsSet(true);
    return this;
  }

  public void unsetSize() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __SIZE_ISSET_ID);
  }

  /** Returns true if field size is set (has been assigned a value) and false otherwise */
  public boolean isSetSize() {
    return EncodingUtils.testBit(__isset_bitfield, __SIZE_ISSET_ID);
  }

  public void setSizeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __SIZE_ISSET_ID, value);
  }

  public int getBlocksSize() {
    return (this.blocks == null) ? 0 : this.blocks.size();
  }

  public java.util.Iterator<Long> getBlocksIterator() {
    return (this.blocks == null) ? null : this.blocks.iterator();
  }

  public void addToBlocks(long elem) {
    if (this.blocks == null) {
      this.blocks = new ArrayList<Long>();
    }
    this.blocks.add(elem);
  }

  public List<Long> getBlocks() {
    return this.blocks;
  }

  public RenameData setBlocks(List<Long> blocks) {
    this.blocks = blocks;
    return this;
  }

  public void unsetBlocks() {
    this.blocks = null;
  }

  /** Returns true if field blocks is set (has been assigned a value) and false otherwise */
  public boolean isSetBlocks() {
    return this.blocks != null;
  }

  public void setBlocksIsSet(boolean value) {
    if (!value) {
      this.blocks = null;
    }
  }

  public int getAtime() {
    return this.atime;
  }

  public RenameData setAtime(int atime) {
    this.atime = atime;
    setAtimeIsSet(true);
    return this;
  }

  public void unsetAtime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ATIME_ISSET_ID);
  }

  /** Returns true if field atime is set (has been assigned a value) and false otherwise */
  public boolean isSetAtime() {
    return EncodingUtils.testBit(__isset_bitfield, __ATIME_ISSET_ID);
  }

  public void setAtimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ATIME_ISSET_ID, value);
  }

  public int getMtime() {
    return this.mtime;
  }

  public RenameData setMtime(int mtime) {
    this.mtime = mtime;
    setMtimeIsSet(true);
    return this;
  }

  public void unsetMtime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MTIME_ISSET_ID);
  }

  /** Returns true if field mtime is set (has been assigned a value) and false otherwise */
  public boolean isSetMtime() {
    return EncodingUtils.testBit(__isset_bitfield, __MTIME_ISSET_ID);
  }

  public void setMtimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MTIME_ISSET_ID, value);
  }

  public int getCtime() {
    return this.ctime;
  }

  public RenameData setCtime(int ctime) {
    this.ctime = ctime;
    setCtimeIsSet(true);
    return this;
  }

  public void unsetCtime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __CTIME_ISSET_ID);
  }

  /** Returns true if field ctime is set (has been assigned a value) and false otherwise */
  public boolean isSetCtime() {
    return EncodingUtils.testBit(__isset_bitfield, __CTIME_ISSET_ID);
  }

  public void setCtimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __CTIME_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case MODE:
      if (value == null) {
        unsetMode();
      } else {
        setMode((Integer)value);
      }
      break;

    case RDEV:
      if (value == null) {
        unsetRdev();
      } else {
        setRdev((Integer)value);
      }
      break;

    case UID:
      if (value == null) {
        unsetUid();
      } else {
        setUid((Integer)value);
      }
      break;

    case GID:
      if (value == null) {
        unsetGid();
      } else {
        setGid((Integer)value);
      }
      break;

    case SIZE:
      if (value == null) {
        unsetSize();
      } else {
        setSize((Long)value);
      }
      break;

    case BLOCKS:
      if (value == null) {
        unsetBlocks();
      } else {
        setBlocks((List<Long>)value);
      }
      break;

    case ATIME:
      if (value == null) {
        unsetAtime();
      } else {
        setAtime((Integer)value);
      }
      break;

    case MTIME:
      if (value == null) {
        unsetMtime();
      } else {
        setMtime((Integer)value);
      }
      break;

    case CTIME:
      if (value == null) {
        unsetCtime();
      } else {
        setCtime((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MODE:
      return Integer.valueOf(getMode());

    case RDEV:
      return Integer.valueOf(getRdev());

    case UID:
      return Integer.valueOf(getUid());

    case GID:
      return Integer.valueOf(getGid());

    case SIZE:
      return Long.valueOf(getSize());

    case BLOCKS:
      return getBlocks();

    case ATIME:
      return Integer.valueOf(getAtime());

    case MTIME:
      return Integer.valueOf(getMtime());

    case CTIME:
      return Integer.valueOf(getCtime());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case MODE:
      return isSetMode();
    case RDEV:
      return isSetRdev();
    case UID:
      return isSetUid();
    case GID:
      return isSetGid();
    case SIZE:
      return isSetSize();
    case BLOCKS:
      return isSetBlocks();
    case ATIME:
      return isSetAtime();
    case MTIME:
      return isSetMtime();
    case CTIME:
      return isSetCtime();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof RenameData)
      return this.equals((RenameData)that);
    return false;
  }

  public boolean equals(RenameData that) {
    if (that == null)
      return false;

    boolean this_present_mode = true;
    boolean that_present_mode = true;
    if (this_present_mode || that_present_mode) {
      if (!(this_present_mode && that_present_mode))
        return false;
      if (this.mode != that.mode)
        return false;
    }

    boolean this_present_rdev = true;
    boolean that_present_rdev = true;
    if (this_present_rdev || that_present_rdev) {
      if (!(this_present_rdev && that_present_rdev))
        return false;
      if (this.rdev != that.rdev)
        return false;
    }

    boolean this_present_uid = true;
    boolean that_present_uid = true;
    if (this_present_uid || that_present_uid) {
      if (!(this_present_uid && that_present_uid))
        return false;
      if (this.uid != that.uid)
        return false;
    }

    boolean this_present_gid = true;
    boolean that_present_gid = true;
    if (this_present_gid || that_present_gid) {
      if (!(this_present_gid && that_present_gid))
        return false;
      if (this.gid != that.gid)
        return false;
    }

    boolean this_present_size = true;
    boolean that_present_size = true;
    if (this_present_size || that_present_size) {
      if (!(this_present_size && that_present_size))
        return false;
      if (this.size != that.size)
        return false;
    }

    boolean this_present_blocks = true && this.isSetBlocks();
    boolean that_present_blocks = true && that.isSetBlocks();
    if (this_present_blocks || that_present_blocks) {
      if (!(this_present_blocks && that_present_blocks))
        return false;
      if (!this.blocks.equals(that.blocks))
        return false;
    }

    boolean this_present_atime = true;
    boolean that_present_atime = true;
    if (this_present_atime || that_present_atime) {
      if (!(this_present_atime && that_present_atime))
        return false;
      if (this.atime != that.atime)
        return false;
    }

    boolean this_present_mtime = true;
    boolean that_present_mtime = true;
    if (this_present_mtime || that_present_mtime) {
      if (!(this_present_mtime && that_present_mtime))
        return false;
      if (this.mtime != that.mtime)
        return false;
    }

    boolean this_present_ctime = true;
    boolean that_present_ctime = true;
    if (this_present_ctime || that_present_ctime) {
      if (!(this_present_ctime && that_present_ctime))
        return false;
      if (this.ctime != that.ctime)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(RenameData other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetMode()).compareTo(other.isSetMode());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMode()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.mode, other.mode);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRdev()).compareTo(other.isSetRdev());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRdev()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.rdev, other.rdev);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetUid()).compareTo(other.isSetUid());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetUid()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.uid, other.uid);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetGid()).compareTo(other.isSetGid());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetGid()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.gid, other.gid);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSize()).compareTo(other.isSetSize());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSize()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.size, other.size);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetBlocks()).compareTo(other.isSetBlocks());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetBlocks()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.blocks, other.blocks);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAtime()).compareTo(other.isSetAtime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAtime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.atime, other.atime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMtime()).compareTo(other.isSetMtime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMtime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.mtime, other.mtime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCtime()).compareTo(other.isSetCtime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCtime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.ctime, other.ctime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("RenameData(");
    boolean first = true;

    sb.append("mode:");
    sb.append(this.mode);
    first = false;
    if (!first) sb.append(", ");
    sb.append("rdev:");
    sb.append(this.rdev);
    first = false;
    if (!first) sb.append(", ");
    sb.append("uid:");
    sb.append(this.uid);
    first = false;
    if (!first) sb.append(", ");
    sb.append("gid:");
    sb.append(this.gid);
    first = false;
    if (!first) sb.append(", ");
    sb.append("size:");
    sb.append(this.size);
    first = false;
    if (!first) sb.append(", ");
    sb.append("blocks:");
    if (this.blocks == null) {
      sb.append("null");
    } else {
      sb.append(this.blocks);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("atime:");
    sb.append(this.atime);
    first = false;
    if (!first) sb.append(", ");
    sb.append("mtime:");
    sb.append(this.mtime);
    first = false;
    if (!first) sb.append(", ");
    sb.append("ctime:");
    sb.append(this.ctime);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class RenameDataStandardSchemeFactory implements SchemeFactory {
    public RenameDataStandardScheme getScheme() {
      return new RenameDataStandardScheme();
    }
  }

  private static class RenameDataStandardScheme extends StandardScheme<RenameData> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, RenameData struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // MODE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.mode = iprot.readI32();
              struct.setModeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // RDEV
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.rdev = iprot.readI32();
              struct.setRdevIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // UID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.uid = iprot.readI32();
              struct.setUidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // GID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.gid = iprot.readI32();
              struct.setGidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // SIZE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.size = iprot.readI64();
              struct.setSizeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // BLOCKS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list8 = iprot.readListBegin();
                struct.blocks = new ArrayList<Long>(_list8.size);
                for (int _i9 = 0; _i9 < _list8.size; ++_i9)
                {
                  long _elem10;
                  _elem10 = iprot.readI64();
                  struct.blocks.add(_elem10);
                }
                iprot.readListEnd();
              }
              struct.setBlocksIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // ATIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.atime = iprot.readI32();
              struct.setAtimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 8: // MTIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.mtime = iprot.readI32();
              struct.setMtimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 9: // CTIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.ctime = iprot.readI32();
              struct.setCtimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, RenameData struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(MODE_FIELD_DESC);
      oprot.writeI32(struct.mode);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(RDEV_FIELD_DESC);
      oprot.writeI32(struct.rdev);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(UID_FIELD_DESC);
      oprot.writeI32(struct.uid);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(GID_FIELD_DESC);
      oprot.writeI32(struct.gid);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(SIZE_FIELD_DESC);
      oprot.writeI64(struct.size);
      oprot.writeFieldEnd();
      if (struct.blocks != null) {
        oprot.writeFieldBegin(BLOCKS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.I64, struct.blocks.size()));
          for (long _iter11 : struct.blocks)
          {
            oprot.writeI64(_iter11);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(ATIME_FIELD_DESC);
      oprot.writeI32(struct.atime);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(MTIME_FIELD_DESC);
      oprot.writeI32(struct.mtime);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(CTIME_FIELD_DESC);
      oprot.writeI32(struct.ctime);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class RenameDataTupleSchemeFactory implements SchemeFactory {
    public RenameDataTupleScheme getScheme() {
      return new RenameDataTupleScheme();
    }
  }

  private static class RenameDataTupleScheme extends TupleScheme<RenameData> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, RenameData struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetMode()) {
        optionals.set(0);
      }
      if (struct.isSetRdev()) {
        optionals.set(1);
      }
      if (struct.isSetUid()) {
        optionals.set(2);
      }
      if (struct.isSetGid()) {
        optionals.set(3);
      }
      if (struct.isSetSize()) {
        optionals.set(4);
      }
      if (struct.isSetBlocks()) {
        optionals.set(5);
      }
      if (struct.isSetAtime()) {
        optionals.set(6);
      }
      if (struct.isSetMtime()) {
        optionals.set(7);
      }
      if (struct.isSetCtime()) {
        optionals.set(8);
      }
      oprot.writeBitSet(optionals, 9);
      if (struct.isSetMode()) {
        oprot.writeI32(struct.mode);
      }
      if (struct.isSetRdev()) {
        oprot.writeI32(struct.rdev);
      }
      if (struct.isSetUid()) {
        oprot.writeI32(struct.uid);
      }
      if (struct.isSetGid()) {
        oprot.writeI32(struct.gid);
      }
      if (struct.isSetSize()) {
        oprot.writeI64(struct.size);
      }
      if (struct.isSetBlocks()) {
        {
          oprot.writeI32(struct.blocks.size());
          for (long _iter12 : struct.blocks)
          {
            oprot.writeI64(_iter12);
          }
        }
      }
      if (struct.isSetAtime()) {
        oprot.writeI32(struct.atime);
      }
      if (struct.isSetMtime()) {
        oprot.writeI32(struct.mtime);
      }
      if (struct.isSetCtime()) {
        oprot.writeI32(struct.ctime);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, RenameData struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(9);
      if (incoming.get(0)) {
        struct.mode = iprot.readI32();
        struct.setModeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.rdev = iprot.readI32();
        struct.setRdevIsSet(true);
      }
      if (incoming.get(2)) {
        struct.uid = iprot.readI32();
        struct.setUidIsSet(true);
      }
      if (incoming.get(3)) {
        struct.gid = iprot.readI32();
        struct.setGidIsSet(true);
      }
      if (incoming.get(4)) {
        struct.size = iprot.readI64();
        struct.setSizeIsSet(true);
      }
      if (incoming.get(5)) {
        {
          org.apache.thrift.protocol.TList _list13 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.I64, iprot.readI32());
          struct.blocks = new ArrayList<Long>(_list13.size);
          for (int _i14 = 0; _i14 < _list13.size; ++_i14)
          {
            long _elem15;
            _elem15 = iprot.readI64();
            struct.blocks.add(_elem15);
          }
        }
        struct.setBlocksIsSet(true);
      }
      if (incoming.get(6)) {
        struct.atime = iprot.readI32();
        struct.setAtimeIsSet(true);
      }
      if (incoming.get(7)) {
        struct.mtime = iprot.readI32();
        struct.setMtimeIsSet(true);
      }
      if (incoming.get(8)) {
        struct.ctime = iprot.readI32();
        struct.setCtimeIsSet(true);
      }
    }
  }

}

