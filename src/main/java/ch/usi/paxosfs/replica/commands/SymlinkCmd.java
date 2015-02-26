/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.usi.paxosfs.replica.commands;

import org.apache.thrift.EncodingUtils;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import java.util.*;

public class SymlinkCmd implements org.apache.thrift.TBase<SymlinkCmd, SymlinkCmd._Fields>, java.io.Serializable, Cloneable, Comparable<SymlinkCmd> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("SymlinkCmd");

  private static final org.apache.thrift.protocol.TField TARGET_FIELD_DESC = new org.apache.thrift.protocol.TField("target", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField PATH_FIELD_DESC = new org.apache.thrift.protocol.TField("path", org.apache.thrift.protocol.TType.STRING, (short)4);
  private static final org.apache.thrift.protocol.TField UID_FIELD_DESC = new org.apache.thrift.protocol.TField("uid", org.apache.thrift.protocol.TType.I32, (short)5);
  private static final org.apache.thrift.protocol.TField GID_FIELD_DESC = new org.apache.thrift.protocol.TField("gid", org.apache.thrift.protocol.TType.I32, (short)6);
  private static final org.apache.thrift.protocol.TField PARENT_PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("parentPartition", org.apache.thrift.protocol.TType.SET, (short)7);
  private static final org.apache.thrift.protocol.TField PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("partition", org.apache.thrift.protocol.TType.SET, (short)8);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new SymlinkCmdStandardSchemeFactory());
    schemes.put(TupleScheme.class, new SymlinkCmdTupleSchemeFactory());
  }

  public String target; // required
  public String path; // required
  public int uid; // required
  public int gid; // required
  public Set<Byte> parentPartition; // required
  public Set<Byte> partition; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    TARGET((short)3, "target"),
    PATH((short)4, "path"),
    UID((short)5, "uid"),
    GID((short)6, "gid"),
    PARENT_PARTITION((short)7, "parentPartition"),
    PARTITION((short)8, "partition");

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
        case 3: // TARGET
          return TARGET;
        case 4: // PATH
          return PATH;
        case 5: // UID
          return UID;
        case 6: // GID
          return GID;
        case 7: // PARENT_PARTITION
          return PARENT_PARTITION;
        case 8: // PARTITION
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
    tmpMap.put(_Fields.TARGET, new org.apache.thrift.meta_data.FieldMetaData("target", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.PATH, new org.apache.thrift.meta_data.FieldMetaData("path", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.UID, new org.apache.thrift.meta_data.FieldMetaData("uid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.GID, new org.apache.thrift.meta_data.FieldMetaData("gid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.PARENT_PARTITION, new org.apache.thrift.meta_data.FieldMetaData("parentPartition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    tmpMap.put(_Fields.PARTITION, new org.apache.thrift.meta_data.FieldMetaData("partition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(SymlinkCmd.class, metaDataMap);
  }

  public SymlinkCmd() {
  }

  public SymlinkCmd(
    String target,
    String path,
    int uid,
    int gid,
    Set<Byte> parentPartition,
    Set<Byte> partition)
  {
    this();
    this.target = target;
    this.path = path;
    this.uid = uid;
    setUidIsSet(true);
    this.gid = gid;
    setGidIsSet(true);
    this.parentPartition = parentPartition;
    this.partition = partition;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public SymlinkCmd(SymlinkCmd other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetTarget()) {
      this.target = other.target;
    }
    if (other.isSetPath()) {
      this.path = other.path;
    }
    this.uid = other.uid;
    this.gid = other.gid;
    if (other.isSetParentPartition()) {
      Set<Byte> __this__parentPartition = new HashSet<Byte>(other.parentPartition);
      this.parentPartition = __this__parentPartition;
    }
    if (other.isSetPartition()) {
      Set<Byte> __this__partition = new HashSet<Byte>(other.partition);
      this.partition = __this__partition;
    }
  }

  public SymlinkCmd deepCopy() {
    return new SymlinkCmd(this);
  }

  @Override
  public void clear() {
    this.target = null;
    this.path = null;
    setUidIsSet(false);
    this.uid = 0;
    setGidIsSet(false);
    this.gid = 0;
    this.parentPartition = null;
    this.partition = null;
  }

  public String getTarget() {
    return this.target;
  }

  public SymlinkCmd setTarget(String target) {
    this.target = target;
    return this;
  }

  public void unsetTarget() {
    this.target = null;
  }

  /** Returns true if field target is set (has been assigned a value) and false otherwise */
  public boolean isSetTarget() {
    return this.target != null;
  }

  public void setTargetIsSet(boolean value) {
    if (!value) {
      this.target = null;
    }
  }

  public String getPath() {
    return this.path;
  }

  public SymlinkCmd setPath(String path) {
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

  public SymlinkCmd setUid(int uid) {
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

  public SymlinkCmd setGid(int gid) {
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

  public int getParentPartitionSize() {
    return (this.parentPartition == null) ? 0 : this.parentPartition.size();
  }

  public java.util.Iterator<Byte> getParentPartitionIterator() {
    return (this.parentPartition == null) ? null : this.parentPartition.iterator();
  }

  public void addToParentPartition(byte elem) {
    if (this.parentPartition == null) {
      this.parentPartition = new HashSet<Byte>();
    }
    this.parentPartition.add(elem);
  }

  public Set<Byte> getParentPartition() {
    return this.parentPartition;
  }

  public SymlinkCmd setParentPartition(Set<Byte> parentPartition) {
    this.parentPartition = parentPartition;
    return this;
  }

  public void unsetParentPartition() {
    this.parentPartition = null;
  }

  /** Returns true if field parentPartition is set (has been assigned a value) and false otherwise */
  public boolean isSetParentPartition() {
    return this.parentPartition != null;
  }

  public void setParentPartitionIsSet(boolean value) {
    if (!value) {
      this.parentPartition = null;
    }
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

  public SymlinkCmd setPartition(Set<Byte> partition) {
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
    case TARGET:
      if (value == null) {
        unsetTarget();
      } else {
        setTarget((String)value);
      }
      break;

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

    case PARENT_PARTITION:
      if (value == null) {
        unsetParentPartition();
      } else {
        setParentPartition((Set<Byte>)value);
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
    case TARGET:
      return getTarget();

    case PATH:
      return getPath();

    case UID:
      return Integer.valueOf(getUid());

    case GID:
      return Integer.valueOf(getGid());

    case PARENT_PARTITION:
      return getParentPartition();

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
    case TARGET:
      return isSetTarget();
    case PATH:
      return isSetPath();
    case UID:
      return isSetUid();
    case GID:
      return isSetGid();
    case PARENT_PARTITION:
      return isSetParentPartition();
    case PARTITION:
      return isSetPartition();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof SymlinkCmd)
      return this.equals((SymlinkCmd)that);
    return false;
  }

  public boolean equals(SymlinkCmd that) {
    if (that == null)
      return false;

    boolean this_present_target = true && this.isSetTarget();
    boolean that_present_target = true && that.isSetTarget();
    if (this_present_target || that_present_target) {
      if (!(this_present_target && that_present_target))
        return false;
      if (!this.target.equals(that.target))
        return false;
    }

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

    boolean this_present_parentPartition = true && this.isSetParentPartition();
    boolean that_present_parentPartition = true && that.isSetParentPartition();
    if (this_present_parentPartition || that_present_parentPartition) {
      if (!(this_present_parentPartition && that_present_parentPartition))
        return false;
      if (!this.parentPartition.equals(that.parentPartition))
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
  public int compareTo(SymlinkCmd other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetTarget()).compareTo(other.isSetTarget());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTarget()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.target, other.target);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
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
    lastComparison = Boolean.valueOf(isSetParentPartition()).compareTo(other.isSetParentPartition());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetParentPartition()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.parentPartition, other.parentPartition);
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
    StringBuilder sb = new StringBuilder("SymlinkCmd(");
    boolean first = true;

    sb.append("target:");
    if (this.target == null) {
      sb.append("null");
    } else {
      sb.append(this.target);
    }
    first = false;
    if (!first) sb.append(", ");
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
    sb.append("parentPartition:");
    if (this.parentPartition == null) {
      sb.append("null");
    } else {
      sb.append(this.parentPartition);
    }
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

  private static class SymlinkCmdStandardSchemeFactory implements SchemeFactory {
    public SymlinkCmdStandardScheme getScheme() {
      return new SymlinkCmdStandardScheme();
    }
  }

  private static class SymlinkCmdStandardScheme extends StandardScheme<SymlinkCmd> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, SymlinkCmd struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 3: // TARGET
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.target = iprot.readString();
              struct.setTargetIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // PATH
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.path = iprot.readString();
              struct.setPathIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // UID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.uid = iprot.readI32();
              struct.setUidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // GID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.gid = iprot.readI32();
              struct.setGidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // PARENT_PARTITION
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set80 = iprot.readSetBegin();
                struct.parentPartition = new HashSet<Byte>(2*_set80.size);
                for (int _i81 = 0; _i81 < _set80.size; ++_i81)
                {
                  byte _elem82;
                  _elem82 = iprot.readByte();
                  struct.parentPartition.add(_elem82);
                }
                iprot.readSetEnd();
              }
              struct.setParentPartitionIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 8: // PARTITION
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set83 = iprot.readSetBegin();
                struct.partition = new HashSet<Byte>(2*_set83.size);
                for (int _i84 = 0; _i84 < _set83.size; ++_i84)
                {
                  byte _elem85;
                  _elem85 = iprot.readByte();
                  struct.partition.add(_elem85);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, SymlinkCmd struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.target != null) {
        oprot.writeFieldBegin(TARGET_FIELD_DESC);
        oprot.writeString(struct.target);
        oprot.writeFieldEnd();
      }
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
      if (struct.parentPartition != null) {
        oprot.writeFieldBegin(PARENT_PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.parentPartition.size()));
          for (byte _iter86 : struct.parentPartition)
          {
            oprot.writeByte(_iter86);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.partition != null) {
        oprot.writeFieldBegin(PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.partition.size()));
          for (byte _iter87 : struct.partition)
          {
            oprot.writeByte(_iter87);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class SymlinkCmdTupleSchemeFactory implements SchemeFactory {
    public SymlinkCmdTupleScheme getScheme() {
      return new SymlinkCmdTupleScheme();
    }
  }

  private static class SymlinkCmdTupleScheme extends TupleScheme<SymlinkCmd> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, SymlinkCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetTarget()) {
        optionals.set(0);
      }
      if (struct.isSetPath()) {
        optionals.set(1);
      }
      if (struct.isSetUid()) {
        optionals.set(2);
      }
      if (struct.isSetGid()) {
        optionals.set(3);
      }
      if (struct.isSetParentPartition()) {
        optionals.set(4);
      }
      if (struct.isSetPartition()) {
        optionals.set(5);
      }
      oprot.writeBitSet(optionals, 6);
      if (struct.isSetTarget()) {
        oprot.writeString(struct.target);
      }
      if (struct.isSetPath()) {
        oprot.writeString(struct.path);
      }
      if (struct.isSetUid()) {
        oprot.writeI32(struct.uid);
      }
      if (struct.isSetGid()) {
        oprot.writeI32(struct.gid);
      }
      if (struct.isSetParentPartition()) {
        {
          oprot.writeI32(struct.parentPartition.size());
          for (byte _iter88 : struct.parentPartition)
          {
            oprot.writeByte(_iter88);
          }
        }
      }
      if (struct.isSetPartition()) {
        {
          oprot.writeI32(struct.partition.size());
          for (byte _iter89 : struct.partition)
          {
            oprot.writeByte(_iter89);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, SymlinkCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(6);
      if (incoming.get(0)) {
        struct.target = iprot.readString();
        struct.setTargetIsSet(true);
      }
      if (incoming.get(1)) {
        struct.path = iprot.readString();
        struct.setPathIsSet(true);
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
        {
          org.apache.thrift.protocol.TSet _set90 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.parentPartition = new HashSet<Byte>(2*_set90.size);
          for (int _i91 = 0; _i91 < _set90.size; ++_i91)
          {
            byte _elem92;
            _elem92 = iprot.readByte();
            struct.parentPartition.add(_elem92);
          }
        }
        struct.setParentPartitionIsSet(true);
      }
      if (incoming.get(5)) {
        {
          org.apache.thrift.protocol.TSet _set93 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.partition = new HashSet<Byte>(2*_set93.size);
          for (int _i94 = 0; _i94 < _set93.size; ++_i94)
          {
            byte _elem95;
            _elem95 = iprot.readByte();
            struct.partition.add(_elem95);
          }
        }
        struct.setPartitionIsSet(true);
      }
    }
  }

}

