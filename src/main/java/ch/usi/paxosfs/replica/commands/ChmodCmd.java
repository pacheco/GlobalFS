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

public class ChmodCmd implements org.apache.thrift.TBase<ChmodCmd, ChmodCmd._Fields>, java.io.Serializable, Cloneable, Comparable<ChmodCmd> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ChmodCmd");

  private static final org.apache.thrift.protocol.TField PATH_FIELD_DESC = new org.apache.thrift.protocol.TField("path", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField MODE_FIELD_DESC = new org.apache.thrift.protocol.TField("mode", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField PARTITION_FIELD_DESC = new org.apache.thrift.protocol.TField("partition", org.apache.thrift.protocol.TType.SET, (short)7);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ChmodCmdStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ChmodCmdTupleSchemeFactory());
  }

  public String path; // required
  public int mode; // required
  public Set<Byte> partition; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PATH((short)3, "path"),
    MODE((short)4, "mode"),
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
        case 3: // PATH
          return PATH;
        case 4: // MODE
          return MODE;
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
  private static final int __MODE_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PATH, new org.apache.thrift.meta_data.FieldMetaData("path", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.MODE, new org.apache.thrift.meta_data.FieldMetaData("mode", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.PARTITION, new org.apache.thrift.meta_data.FieldMetaData("partition", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BYTE))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ChmodCmd.class, metaDataMap);
  }

  public ChmodCmd() {
  }

  public ChmodCmd(
    String path,
    int mode,
    Set<Byte> partition)
  {
    this();
    this.path = path;
    this.mode = mode;
    setModeIsSet(true);
    this.partition = partition;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ChmodCmd(ChmodCmd other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetPath()) {
      this.path = other.path;
    }
    this.mode = other.mode;
    if (other.isSetPartition()) {
      Set<Byte> __this__partition = new HashSet<Byte>(other.partition);
      this.partition = __this__partition;
    }
  }

  public ChmodCmd deepCopy() {
    return new ChmodCmd(this);
  }

  @Override
  public void clear() {
    this.path = null;
    setModeIsSet(false);
    this.mode = 0;
    this.partition = null;
  }

  public String getPath() {
    return this.path;
  }

  public ChmodCmd setPath(String path) {
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

  public int getMode() {
    return this.mode;
  }

  public ChmodCmd setMode(int mode) {
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

  public ChmodCmd setPartition(Set<Byte> partition) {
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

    case MODE:
      if (value == null) {
        unsetMode();
      } else {
        setMode((Integer)value);
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

    case MODE:
      return Integer.valueOf(getMode());

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
    case MODE:
      return isSetMode();
    case PARTITION:
      return isSetPartition();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ChmodCmd)
      return this.equals((ChmodCmd)that);
    return false;
  }

  public boolean equals(ChmodCmd that) {
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

    boolean this_present_mode = true;
    boolean that_present_mode = true;
    if (this_present_mode || that_present_mode) {
      if (!(this_present_mode && that_present_mode))
        return false;
      if (this.mode != that.mode)
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
  public int compareTo(ChmodCmd other) {
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
    StringBuilder sb = new StringBuilder("ChmodCmd(");
    boolean first = true;

    sb.append("path:");
    if (this.path == null) {
      sb.append("null");
    } else {
      sb.append(this.path);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("mode:");
    sb.append(this.mode);
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

  private static class ChmodCmdStandardSchemeFactory implements SchemeFactory {
    public ChmodCmdStandardScheme getScheme() {
      return new ChmodCmdStandardScheme();
    }
  }

  private static class ChmodCmdStandardScheme extends StandardScheme<ChmodCmd> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ChmodCmd struct) throws org.apache.thrift.TException {
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
          case 4: // MODE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.mode = iprot.readI32();
              struct.setModeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // PARTITION
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set128 = iprot.readSetBegin();
                struct.partition = new HashSet<Byte>(2*_set128.size);
                for (int _i129 = 0; _i129 < _set128.size; ++_i129)
                {
                  byte _elem130;
                  _elem130 = iprot.readByte();
                  struct.partition.add(_elem130);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ChmodCmd struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.path != null) {
        oprot.writeFieldBegin(PATH_FIELD_DESC);
        oprot.writeString(struct.path);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(MODE_FIELD_DESC);
      oprot.writeI32(struct.mode);
      oprot.writeFieldEnd();
      if (struct.partition != null) {
        oprot.writeFieldBegin(PARTITION_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, struct.partition.size()));
          for (byte _iter131 : struct.partition)
          {
            oprot.writeByte(_iter131);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ChmodCmdTupleSchemeFactory implements SchemeFactory {
    public ChmodCmdTupleScheme getScheme() {
      return new ChmodCmdTupleScheme();
    }
  }

  private static class ChmodCmdTupleScheme extends TupleScheme<ChmodCmd> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ChmodCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPath()) {
        optionals.set(0);
      }
      if (struct.isSetMode()) {
        optionals.set(1);
      }
      if (struct.isSetPartition()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetPath()) {
        oprot.writeString(struct.path);
      }
      if (struct.isSetMode()) {
        oprot.writeI32(struct.mode);
      }
      if (struct.isSetPartition()) {
        {
          oprot.writeI32(struct.partition.size());
          for (byte _iter132 : struct.partition)
          {
            oprot.writeByte(_iter132);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ChmodCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.path = iprot.readString();
        struct.setPathIsSet(true);
      }
      if (incoming.get(1)) {
        struct.mode = iprot.readI32();
        struct.setModeIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TSet _set133 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.BYTE, iprot.readI32());
          struct.partition = new HashSet<Byte>(2*_set133.size);
          for (int _i134 = 0; _i134 < _set133.size; ++_i134)
          {
            byte _elem135;
            _elem135 = iprot.readByte();
            struct.partition.add(_elem135);
          }
        }
        struct.setPartitionIsSet(true);
      }
    }
  }

}

