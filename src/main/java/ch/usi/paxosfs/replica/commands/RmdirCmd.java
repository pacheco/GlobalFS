/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.usi.paxosfs.replica.commands;

import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import java.util.*;

public class RmdirCmd implements org.apache.thrift.TBase<RmdirCmd, RmdirCmd._Fields>, java.io.Serializable, Cloneable, Comparable<RmdirCmd> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("RmdirCmd");

  private static final org.apache.thrift.protocol.TField PATH_FIELD_DESC = new org.apache.thrift.protocol.TField("path", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField PARENT_PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("parentPartition", org.apache.thrift.protocol.TType.SET, (short)7);
  private static final org.apache.thrift.protocol.TField PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("partition", org.apache.thrift.protocol.TType.SET, (short)8);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new RmdirCmdStandardSchemeFactory());
    schemes.put(TupleScheme.class, new RmdirCmdTupleSchemeFactory());
  }

  public String path; // required
  public Set<Byte> parentPartition; // required
  public Set<Byte> partition; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PATH((short)3, "path"),
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
        case 3: // PATH
          return PATH;
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
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PATH, new org.apache.thrift.meta_data.FieldMetaData("path", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.PARENT_PARTITION, new org.apache.thrift.meta_data.FieldMetaData("parentPartition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    tmpMap.put(_Fields.PARTITION, new org.apache.thrift.meta_data.FieldMetaData("partition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(RmdirCmd.class, metaDataMap);
  }

  public RmdirCmd() {
  }

  public RmdirCmd(
    String path,
    Set<Byte> parentPartition,
    Set<Byte> partition)
  {
    this();
    this.path = path;
    this.parentPartition = parentPartition;
    this.partition = partition;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public RmdirCmd(RmdirCmd other) {
    if (other.isSetPath()) {
      this.path = other.path;
    }
    if (other.isSetParentPartition()) {
      Set<Byte> __this__parentPartition = new HashSet<Byte>(other.parentPartition);
      this.parentPartition = __this__parentPartition;
    }
    if (other.isSetPartition()) {
      Set<Byte> __this__partition = new HashSet<Byte>(other.partition);
      this.partition = __this__partition;
    }
  }

  public RmdirCmd deepCopy() {
    return new RmdirCmd(this);
  }

  @Override
  public void clear() {
    this.path = null;
    this.parentPartition = null;
    this.partition = null;
  }

  public String getPath() {
    return this.path;
  }

  public RmdirCmd setPath(String path) {
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

  public RmdirCmd setParentPartition(Set<Byte> parentPartition) {
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

  public RmdirCmd setPartition(Set<Byte> partition) {
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
    case PATH:
      return getPath();

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
    case PATH:
      return isSetPath();
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
    if (that instanceof RmdirCmd)
      return this.equals((RmdirCmd)that);
    return false;
  }

  public boolean equals(RmdirCmd that) {
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
  public int compareTo(RmdirCmd other) {
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
    StringBuilder sb = new StringBuilder("RmdirCmd(");
    boolean first = true;

    sb.append("path:");
    if (this.path == null) {
      sb.append("null");
    } else {
      sb.append(this.path);
    }
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class RmdirCmdStandardSchemeFactory implements SchemeFactory {
    public RmdirCmdStandardScheme getScheme() {
      return new RmdirCmdStandardScheme();
    }
  }

  private static class RmdirCmdStandardScheme extends StandardScheme<RmdirCmd> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, RmdirCmd struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 3: // PATH
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.path = iprot.readString();
              struct.setPathIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // PARENT_PARTITION
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set64 = iprot.readSetBegin();
                struct.parentPartition = new HashSet<Byte>(2*_set64.size);
                for (int _i65 = 0; _i65 < _set64.size; ++_i65)
                {
                  byte _elem66;
                  _elem66 = iprot.readByte();
                  struct.parentPartition.add(_elem66);
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
                org.apache.thrift.protocol.TSet _set67 = iprot.readSetBegin();
                struct.partition = new HashSet<Byte>(2*_set67.size);
                for (int _i68 = 0; _i68 < _set67.size; ++_i68)
                {
                  byte _elem69;
                  _elem69 = iprot.readByte();
                  struct.partition.add(_elem69);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, RmdirCmd struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.path != null) {
        oprot.writeFieldBegin(PATH_FIELD_DESC);
        oprot.writeString(struct.path);
        oprot.writeFieldEnd();
      }
      if (struct.parentPartition != null) {
        oprot.writeFieldBegin(PARENT_PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.parentPartition.size()));
          for (byte _iter70 : struct.parentPartition)
          {
            oprot.writeByte(_iter70);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.partition != null) {
        oprot.writeFieldBegin(PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.partition.size()));
          for (byte _iter71 : struct.partition)
          {
            oprot.writeByte(_iter71);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class RmdirCmdTupleSchemeFactory implements SchemeFactory {
    public RmdirCmdTupleScheme getScheme() {
      return new RmdirCmdTupleScheme();
    }
  }

  private static class RmdirCmdTupleScheme extends TupleScheme<RmdirCmd> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, RmdirCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPath()) {
        optionals.set(0);
      }
      if (struct.isSetParentPartition()) {
        optionals.set(1);
      }
      if (struct.isSetPartition()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetPath()) {
        oprot.writeString(struct.path);
      }
      if (struct.isSetParentPartition()) {
        {
          oprot.writeI32(struct.parentPartition.size());
          for (byte _iter72 : struct.parentPartition)
          {
            oprot.writeByte(_iter72);
          }
        }
      }
      if (struct.isSetPartition()) {
        {
          oprot.writeI32(struct.partition.size());
          for (byte _iter73 : struct.partition)
          {
            oprot.writeByte(_iter73);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, RmdirCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.path = iprot.readString();
        struct.setPathIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TSet _set74 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.parentPartition = new HashSet<Byte>(2*_set74.size);
          for (int _i75 = 0; _i75 < _set74.size; ++_i75)
          {
            byte _elem76;
            _elem76 = iprot.readByte();
            struct.parentPartition.add(_elem76);
          }
        }
        struct.setParentPartitionIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TSet _set77 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.partition = new HashSet<Byte>(2*_set77.size);
          for (int _i78 = 0; _i78 < _set77.size; ++_i78)
          {
            byte _elem79;
            _elem79 = iprot.readByte();
            struct.partition.add(_elem79);
          }
        }
        struct.setPartitionIsSet(true);
      }
    }
  }

}

