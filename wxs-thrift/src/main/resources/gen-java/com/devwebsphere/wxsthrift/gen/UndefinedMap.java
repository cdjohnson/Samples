/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package com.devwebsphere.wxsthrift.gen;

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
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.thrift.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.protocol.*;

public class UndefinedMap extends Exception implements TBase<UndefinedMap, UndefinedMap._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("UndefinedMap");

  private static final TField MAP_NAME_FIELD_DESC = new TField("mapName", TType.STRING, (short)1);

  public String mapName;

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements TFieldIdEnum {
    MAP_NAME((short)1, "mapName");

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
        case 1: // MAP_NAME
          return MAP_NAME;
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

  public static final Map<_Fields, FieldMetaData> metaDataMap;
  static {
    Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.MAP_NAME, new FieldMetaData("mapName", TFieldRequirementType.DEFAULT, 
        new FieldValueMetaData(TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(UndefinedMap.class, metaDataMap);
  }

  public UndefinedMap() {
  }

  public UndefinedMap(
    String mapName)
  {
    this();
    this.mapName = mapName;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public UndefinedMap(UndefinedMap other) {
    if (other.isSetMapName()) {
      this.mapName = other.mapName;
    }
  }

  public UndefinedMap deepCopy() {
    return new UndefinedMap(this);
  }

  @Deprecated
  public UndefinedMap clone() {
    return new UndefinedMap(this);
  }

  public String getMapName() {
    return this.mapName;
  }

  public UndefinedMap setMapName(String mapName) {
    this.mapName = mapName;
    return this;
  }

  public void unsetMapName() {
    this.mapName = null;
  }

  /** Returns true if field mapName is set (has been asigned a value) and false otherwise */
  public boolean isSetMapName() {
    return this.mapName != null;
  }

  public void setMapNameIsSet(boolean value) {
    if (!value) {
      this.mapName = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case MAP_NAME:
      if (value == null) {
        unsetMapName();
      } else {
        setMapName((String)value);
      }
      break;

    }
  }

  public void setFieldValue(int fieldID, Object value) {
    setFieldValue(_Fields.findByThriftIdOrThrow(fieldID), value);
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MAP_NAME:
      return getMapName();

    }
    throw new IllegalStateException();
  }

  public Object getFieldValue(int fieldId) {
    return getFieldValue(_Fields.findByThriftIdOrThrow(fieldId));
  }

  /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    switch (field) {
    case MAP_NAME:
      return isSetMapName();
    }
    throw new IllegalStateException();
  }

  public boolean isSet(int fieldID) {
    return isSet(_Fields.findByThriftIdOrThrow(fieldID));
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof UndefinedMap)
      return this.equals((UndefinedMap)that);
    return false;
  }

  public boolean equals(UndefinedMap that) {
    if (that == null)
      return false;

    boolean this_present_mapName = true && this.isSetMapName();
    boolean that_present_mapName = true && that.isSetMapName();
    if (this_present_mapName || that_present_mapName) {
      if (!(this_present_mapName && that_present_mapName))
        return false;
      if (!this.mapName.equals(that.mapName))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(UndefinedMap other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    UndefinedMap typedOther = (UndefinedMap)other;

    lastComparison = Boolean.valueOf(isSetMapName()).compareTo(typedOther.isSetMapName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMapName()) {      lastComparison = TBaseHelper.compareTo(this.mapName, typedOther.mapName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public void read(TProtocol iprot) throws TException {
    TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 1: // MAP_NAME
          if (field.type == TType.STRING) {
            this.mapName = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();

    // check for required fields of primitive type, which can't be checked in the validate method
    validate();
  }

  public void write(TProtocol oprot) throws TException {
    validate();

    oprot.writeStructBegin(STRUCT_DESC);
    if (this.mapName != null) {
      oprot.writeFieldBegin(MAP_NAME_FIELD_DESC);
      oprot.writeString(this.mapName);
      oprot.writeFieldEnd();
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("UndefinedMap(");
    boolean first = true;

    sb.append("mapName:");
    if (this.mapName == null) {
      sb.append("null");
    } else {
      sb.append(this.mapName);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws TException {
    // check for required fields
  }

}

