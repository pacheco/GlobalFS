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

public class WriteBlocksCmd implements org.apache.thrift.TBase<WriteBlocksCmd, WriteBlocksCmd._Fields>, java.io.Serializable, Cloneable, Comparable<WriteBlocksCmd> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("WriteBlocksCmd");

  private static final org.apache.thrift.protocol.TField PATH_FIELD_DESC = new org.apache.thrift.protocol.TField("path", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField FILE_HANDLE_FIELD_DESC = new org.apache.thrift.protocol.TField("fileHandle", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField OFFSET_FIELD_DESC = new org.apache.thrift.protocol.TField("offset", org.apache.thrift.protocol.TType.I64, (short)5);
  private static final org.apache.thrift.protocol.TField BLOCKS_FIELD_DESC = new org.apache.thrift.protocol.TField("blocks", org.apache.thrift.protocol.TType.LIST, (short)6);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new WriteBlocksCmdStandardSchemeFactory());
    schemes.put(TupleScheme.class, new WriteBlocksCmdTupleSchemeFactory());
  }

  public String path; // required
  public int fileHandle; // required
  public long offset; // required
  public List<ch.inf.paxosfs.rpc.DBlock> blocks; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PATH((short)3, "path"),
    FILE_HANDLE((short)4, "fileHandle"),
    OFFSET((short)5, "offset"),
    BLOCKS((short)6, "blocks");

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
        case 4: // FILE_HANDLE
          return FILE_HANDLE;
        case 5: // OFFSET
          return OFFSET;
        case 6: // BLOCKS
          return BLOCKS;
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
  private static final int __FILEHANDLE_ISSET_ID = 0;
  private static final int __OFFSET_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PATH, new org.apache.thrift.meta_data.FieldMetaData("path", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.FILE_HANDLE, new org.apache.thrift.meta_data.FieldMetaData("fileHandle", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.OFFSET, new org.apache.thrift.meta_data.FieldMetaData("offset", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.BLOCKS, new org.apache.thrift.meta_data.FieldMetaData("blocks", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ch.inf.paxosfs.rpc.DBlock.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(WriteBlocksCmd.class, metaDataMap);
  }

  public WriteBlocksCmd() {
  }

  public WriteBlocksCmd(
    String path,
    int fileHandle,
    long offset,
    List<ch.inf.paxosfs.rpc.DBlock> blocks)
  {
    this();
    this.path = path;
    this.fileHandle = fileHandle;
    setFileHandleIsSet(true);
    this.offset = offset;
    setOffsetIsSet(true);
    this.blocks = blocks;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public WriteBlocksCmd(WriteBlocksCmd other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetPath()) {
      this.path = other.path;
    }
    this.fileHandle = other.fileHandle;
    this.offset = other.offset;
    if (other.isSetBlocks()) {
      List<ch.inf.paxosfs.rpc.DBlock> __this__blocks = new ArrayList<ch.inf.paxosfs.rpc.DBlock>(other.blocks.size());
      for (ch.inf.paxosfs.rpc.DBlock other_element : other.blocks) {
        __this__blocks.add(new ch.inf.paxosfs.rpc.DBlock(other_element));
      }
      this.blocks = __this__blocks;
    }
  }

  public WriteBlocksCmd deepCopy() {
    return new WriteBlocksCmd(this);
  }

  @Override
  public void clear() {
    this.path = null;
    setFileHandleIsSet(false);
    this.fileHandle = 0;
    setOffsetIsSet(false);
    this.offset = 0;
    this.blocks = null;
  }

  public String getPath() {
    return this.path;
  }

  public WriteBlocksCmd setPath(String path) {
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

  public int getFileHandle() {
    return this.fileHandle;
  }

  public WriteBlocksCmd setFileHandle(int fileHandle) {
    this.fileHandle = fileHandle;
    setFileHandleIsSet(true);
    return this;
  }

  public void unsetFileHandle() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __FILEHANDLE_ISSET_ID);
  }

  /** Returns true if field fileHandle is set (has been assigned a value) and false otherwise */
  public boolean isSetFileHandle() {
    return EncodingUtils.testBit(__isset_bitfield, __FILEHANDLE_ISSET_ID);
  }

  public void setFileHandleIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __FILEHANDLE_ISSET_ID, value);
  }

  public long getOffset() {
    return this.offset;
  }

  public WriteBlocksCmd setOffset(long offset) {
    this.offset = offset;
    setOffsetIsSet(true);
    return this;
  }

  public void unsetOffset() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __OFFSET_ISSET_ID);
  }

  /** Returns true if field offset is set (has been assigned a value) and false otherwise */
  public boolean isSetOffset() {
    return EncodingUtils.testBit(__isset_bitfield, __OFFSET_ISSET_ID);
  }

  public void setOffsetIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __OFFSET_ISSET_ID, value);
  }

  public int getBlocksSize() {
    return (this.blocks == null) ? 0 : this.blocks.size();
  }

  public java.util.Iterator<ch.inf.paxosfs.rpc.DBlock> getBlocksIterator() {
    return (this.blocks == null) ? null : this.blocks.iterator();
  }

  public void addToBlocks(ch.inf.paxosfs.rpc.DBlock elem) {
    if (this.blocks == null) {
      this.blocks = new ArrayList<ch.inf.paxosfs.rpc.DBlock>();
    }
    this.blocks.add(elem);
  }

  public List<ch.inf.paxosfs.rpc.DBlock> getBlocks() {
    return this.blocks;
  }

  public WriteBlocksCmd setBlocks(List<ch.inf.paxosfs.rpc.DBlock> blocks) {
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

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case PATH:
      if (value == null) {
        unsetPath();
      } else {
        setPath((String)value);
      }
      break;

    case FILE_HANDLE:
      if (value == null) {
        unsetFileHandle();
      } else {
        setFileHandle((Integer)value);
      }
      break;

    case OFFSET:
      if (value == null) {
        unsetOffset();
      } else {
        setOffset((Long)value);
      }
      break;

    case BLOCKS:
      if (value == null) {
        unsetBlocks();
      } else {
        setBlocks((List<ch.inf.paxosfs.rpc.DBlock>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PATH:
      return getPath();

    case FILE_HANDLE:
      return Integer.valueOf(getFileHandle());

    case OFFSET:
      return Long.valueOf(getOffset());

    case BLOCKS:
      return getBlocks();

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
    case FILE_HANDLE:
      return isSetFileHandle();
    case OFFSET:
      return isSetOffset();
    case BLOCKS:
      return isSetBlocks();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof WriteBlocksCmd)
      return this.equals((WriteBlocksCmd)that);
    return false;
  }

  public boolean equals(WriteBlocksCmd that) {
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

    boolean this_present_fileHandle = true;
    boolean that_present_fileHandle = true;
    if (this_present_fileHandle || that_present_fileHandle) {
      if (!(this_present_fileHandle && that_present_fileHandle))
        return false;
      if (this.fileHandle != that.fileHandle)
        return false;
    }

    boolean this_present_offset = true;
    boolean that_present_offset = true;
    if (this_present_offset || that_present_offset) {
      if (!(this_present_offset && that_present_offset))
        return false;
      if (this.offset != that.offset)
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

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(WriteBlocksCmd other) {
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
    lastComparison = Boolean.valueOf(isSetFileHandle()).compareTo(other.isSetFileHandle());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFileHandle()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.fileHandle, other.fileHandle);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetOffset()).compareTo(other.isSetOffset());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOffset()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.offset, other.offset);
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
    StringBuilder sb = new StringBuilder("WriteBlocksCmd(");
    boolean first = true;

    sb.append("path:");
    if (this.path == null) {
      sb.append("null");
    } else {
      sb.append(this.path);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("fileHandle:");
    sb.append(this.fileHandle);
    first = false;
    if (!first) sb.append(", ");
    sb.append("offset:");
    sb.append(this.offset);
    first = false;
    if (!first) sb.append(", ");
    sb.append("blocks:");
    if (this.blocks == null) {
      sb.append("null");
    } else {
      sb.append(this.blocks);
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

  private static class WriteBlocksCmdStandardSchemeFactory implements SchemeFactory {
    public WriteBlocksCmdStandardScheme getScheme() {
      return new WriteBlocksCmdStandardScheme();
    }
  }

  private static class WriteBlocksCmdStandardScheme extends StandardScheme<WriteBlocksCmd> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, WriteBlocksCmd struct) throws org.apache.thrift.TException {
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
          case 4: // FILE_HANDLE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.fileHandle = iprot.readI32();
              struct.setFileHandleIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // OFFSET
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.offset = iprot.readI64();
              struct.setOffsetIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // BLOCKS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.blocks = new ArrayList<ch.inf.paxosfs.rpc.DBlock>(_list0.size);
                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                {
                  ch.inf.paxosfs.rpc.DBlock _elem2;
                  _elem2 = new ch.inf.paxosfs.rpc.DBlock();
                  _elem2.read(iprot);
                  struct.blocks.add(_elem2);
                }
                iprot.readListEnd();
              }
              struct.setBlocksIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, WriteBlocksCmd struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.path != null) {
        oprot.writeFieldBegin(PATH_FIELD_DESC);
        oprot.writeString(struct.path);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(FILE_HANDLE_FIELD_DESC);
      oprot.writeI32(struct.fileHandle);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(OFFSET_FIELD_DESC);
      oprot.writeI64(struct.offset);
      oprot.writeFieldEnd();
      if (struct.blocks != null) {
        oprot.writeFieldBegin(BLOCKS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.blocks.size()));
          for (ch.inf.paxosfs.rpc.DBlock _iter3 : struct.blocks)
          {
            _iter3.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class WriteBlocksCmdTupleSchemeFactory implements SchemeFactory {
    public WriteBlocksCmdTupleScheme getScheme() {
      return new WriteBlocksCmdTupleScheme();
    }
  }

  private static class WriteBlocksCmdTupleScheme extends TupleScheme<WriteBlocksCmd> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, WriteBlocksCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPath()) {
        optionals.set(0);
      }
      if (struct.isSetFileHandle()) {
        optionals.set(1);
      }
      if (struct.isSetOffset()) {
        optionals.set(2);
      }
      if (struct.isSetBlocks()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetPath()) {
        oprot.writeString(struct.path);
      }
      if (struct.isSetFileHandle()) {
        oprot.writeI32(struct.fileHandle);
      }
      if (struct.isSetOffset()) {
        oprot.writeI64(struct.offset);
      }
      if (struct.isSetBlocks()) {
        {
          oprot.writeI32(struct.blocks.size());
          for (ch.inf.paxosfs.rpc.DBlock _iter4 : struct.blocks)
          {
            _iter4.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, WriteBlocksCmd struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.path = iprot.readString();
        struct.setPathIsSet(true);
      }
      if (incoming.get(1)) {
        struct.fileHandle = iprot.readI32();
        struct.setFileHandleIsSet(true);
      }
      if (incoming.get(2)) {
        struct.offset = iprot.readI64();
        struct.setOffsetIsSet(true);
      }
      if (incoming.get(3)) {
        {
          org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.blocks = new ArrayList<ch.inf.paxosfs.rpc.DBlock>(_list5.size);
          for (int _i6 = 0; _i6 < _list5.size; ++_i6)
          {
            ch.inf.paxosfs.rpc.DBlock _elem7;
            _elem7 = new ch.inf.paxosfs.rpc.DBlock();
            _elem7.read(iprot);
            struct.blocks.add(_elem7);
          }
        }
        struct.setBlocksIsSet(true);
      }
    }
  }

}

