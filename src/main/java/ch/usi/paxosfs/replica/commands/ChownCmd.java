/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.usi.paxosfs.replica.commands;

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

public class ChownCmd implements org.apache.thrift.TBase<ChownCmd, ChownCmd._Fields>, java.io.Serializable, Cloneable, Comparable<ChownCmd> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ChownCmd");

  private static final org.apache.thrift.protocol.TField PATH_FIELD_DESC = new org.apache.thrift.protocol.TField("path", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField UID_FIELD_DESC = new org.apache.thrift.protocol.TField("uid", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField GID_FIELD_DESC = new org.apache.thrift.protocol.TField("gid", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("partition", org.apache.thrift.protocol.TType.SET, (short)7);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ChownCmdStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ChownCmdTupleSchemeFactory());
  }

  public String path; // required
  public int uid; // required
  public int gid; // required
  public Set<Byte> partition; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PATH((short)2, "path"),
    UID((short)3, "uid"),
    GID((short)4, "gid"),
    PARTITION((short)7, "partition");

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
        case 2: // PATH
          return PATH;
        case 3: // UID
          return UID;
        case 4: // GID
          return GID;
        case 7: // PARTITION
          return PARTITION;
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
  private static final int __UID_ISSET_ID = 0;
  private static final int __GID_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PATH, new org.apache.thrift.meta_data.FieldMetaData("path", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.UID, new org.apache.thrift.meta_data.FieldMetaData("uid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.GID, new org.apache.thrift.meta_data.FieldMetaData("gid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.PARTITION, new org.apache.thrift.meta_data.FieldMetaData("partition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ChownCmd.class, metaDataMap);
  }

  public ChownCmd() {
  }

  public ChownCmd(
    String path,
    int uid,
    int gid,
    Set<Byte> partition)
  {
    this();
    this.path = path;
    this.uid = uid;
    setUidIsSet(true);
    this.gid = gid;
    setGidIsSet(true);
    this.partition = partition;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ChownCmd(ChownCmd other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetPath()) {
      this.path = other.path;
    }
    this.uid = other.uid;
    this.gid = other.gid;
    if (other.isSetPartition()) {
      Set<Byte> __this__partition = new HashSet<Byte>(other.partition);
      this.partition = __this__partition;
    }
  }

  public ChownCmd deepCopy() {
    return new ChownCmd(this);
  }

  @Override
  public void clear() {
    this.path = null;
    setUidIsSet(false);
    this.uid = 0;
    setGidIsSet(false);
    this.gid = 0;
    this.partition = null;
  }

  public String getPath() {
    return this.path;
  }

  public ChownCmd setPath(String path) {
    this.path = path;
    return this;
  }

  public void unsetPath() {
    this.path = null;
  }

  /** Returns true if field path is set (has been assigned a value) and false otherwise */
  public boolean isSetPath() {
    return this.path != null;
  }

  public void setPathIsSet(boolean value) {
    if (!value) {
      this.path = null;
    }
  }

  public int getUid() {
    return this.uid;
  }

  public ChownCmd setUid(int uid) {
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

  public ChownCmd setGid(int gid) {
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

  public int getPartitionSize() {
    return (this.partition == null) ? 0 : this.partition.size();
  }

  public java.util.Iterator<Byte> getPartitionIterator() {
    return (this.partition == null) ? null : this.partition.iterator();
  }

  public void addToPartition(byte elem) {
    if (this.partition == null) {
      this.partition = new HashSet<Byte>();
    }
    this.partition.add(elem);
  }

  public Set<Byte> getPartition() {
    return this.partition;
  }

  public ChownCmd setPartition(Set<Byte> partition) {
    this.partition = partition;
    return this;
  }

  public void unsetPartition() {
    this.partition = null;
  }

  /** Returns true if field partition is set (has been assigned a value) and false otherwise */
  public boolean isSetPartition() {
    return this.partition != null;
  }

  public void setPartitionIsSet(boolean value) {
    if (!value) {
      this.partition = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case PATH:
      if (value == null) {
        unsetPath();
      } else {
        setPath((String)value);
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

    case PARTITION:
      if (value == null) {
        unsetPartition();
      } else {
        setPartition((Set<Byte>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PATH:
      return getPath();

    case UID:
      return Integer.valueOf(getUid());

    case GID:
      return Integer.valueOf(getGid());

    case PARTITION:
      return getPartition();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case PATH:
      return isSetPath();
    case UID:
      return isSetUid();
    case GID:
      return isSetGid();
    case PARTITION:
      return isSetPartition();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ChownCmd)
      return this.equals((ChownCmd)that);
    return false;
  }

  public boolean equals(ChownCmd that) {
    if (that == null)
      return false;

    boolean this_present_path = true && this.isSetPath();
    boolean that_present_path = true && that.isSetPath();
    if (this_present_path || that_present_path) {
      if (!(this_present_path && that_present_path))
        return false;
      if (!this.path.equals(that.path))
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

    boolean this_present_partition = true && this.isSetPartition();
    boolean that_present_partition = true && that.isSetPartition();
    if (this_present_partition || that_present_partition) {
      if (!(this_present_partition && that_present_partition))
        return false;
      if (!this.partition.equals(that.partition))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(ChownCmd other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetPath()).compareTo(other.isSetPath());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPath()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.path, other.path);
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
    lastComparison = Boolean.valueOf(isSetPartition()).compareTo(other.isSetPartition());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPartition()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.partition, other.partition);
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
    StringBuilder sb = new StringBuilder("ChownCmd(");
    boolean first = true;

    sb.append("path:");
    if (this.path == null) {
      sb.append("null");
    } else {
      sb.append(this.path);
    }
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
    sb.append("partition:");
    if (this.partition == null) {
      sb.append("null");
    } else {
      sb.append(this.partition);
    }
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

  private static class ChownCmdStandardSchemeFactory implements SchemeFactory {
    public ChownCmdStandardScheme getScheme() {
      return new ChownCmdStandardScheme();
    }
  }

  private static class ChownCmdStandardScheme extends StandardScheme<ChownCmd> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ChownCmd struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 2: // PATH
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.path = iprot.readString();
              struct.setPathIsSet(true);
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
          case 7: // PARTITION
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set136 = iprot.readSetBegin();
                struct.partition = new HashSet<Byte>(2*_set136.size);
                for (int _i137 = 0; _i137 < _set136.size; ++_i137)
                {
                  byte _elem138;
                  _elem138 = iprot.readByte();
                  struct.partition.add(_elem138);
                }
                iprot.readSetEnd();
              }
              struct.setPartitionIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ChownCmd struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.path != null) {
        oprot.writeFieldBegin(PATH_FIELD_DESC);
        oprot.writeString(struct.path);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(UID_FIELD_DESC);
      oprot.writeI32(struct.uid);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(GID_FIELD_DESC);
      oprot.writeI32(struct.gid);
      oprot.writeFieldEnd();
      if (struct.partition != null) {
        oprot.writeFieldBegin(PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.partition.size()));
          for (byte _iter139 : struct.partition)
          {
            oprot.writeByte(_iter139);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ChownCmdTupleSchemeFactory implements SchemeFactory {
    public ChownCmdTupleScheme getScheme() {
      return new ChownCmdTupleScheme();
    }
  }

  private static class ChownCmdTupleScheme extends TupleScheme<ChownCmd> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ChownCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPath()) {
        optionals.set(0);
      }
      if (struct.isSetUid()) {
        optionals.set(1);
      }
      if (struct.isSetGid()) {
        optionals.set(2);
      }
      if (struct.isSetPartition()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetPath()) {
        oprot.writeString(struct.path);
      }
      if (struct.isSetUid()) {
        oprot.writeI32(struct.uid);
      }
      if (struct.isSetGid()) {
        oprot.writeI32(struct.gid);
      }
      if (struct.isSetPartition()) {
        {
          oprot.writeI32(struct.partition.size());
          for (byte _iter140 : struct.partition)
          {
            oprot.writeByte(_iter140);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ChownCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.path = iprot.readString();
        struct.setPathIsSet(true);
      }
      if (incoming.get(1)) {
        struct.uid = iprot.readI32();
        struct.setUidIsSet(true);
      }
      if (incoming.get(2)) {
        struct.gid = iprot.readI32();
        struct.setGidIsSet(true);
      }
      if (incoming.get(3)) {
        {
          org.apache.thrift.protocol.TSet _set141 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.partition = new HashSet<Byte>(2*_set141.size);
          for (int _i142 = 0; _i142 < _set141.size; ++_i142)
          {
            byte _elem143;
            _elem143 = iprot.readByte();
            struct.partition.add(_elem143);
          }
        }
        struct.setPartitionIsSet(true);
      }
    }
  }

}

