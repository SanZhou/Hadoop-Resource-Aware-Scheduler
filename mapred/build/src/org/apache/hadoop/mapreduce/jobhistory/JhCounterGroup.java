package org.apache.hadoop.mapreduce.jobhistory;

@SuppressWarnings("all")
public class JhCounterGroup extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"JhCounterGroup\",\"namespace\":\"org.apache.hadoop.mapreduce.jobhistory\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"displayName\",\"type\":\"string\"},{\"name\":\"counts\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"JhCounter\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"displayName\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"long\"}]}}}]}");
  public org.apache.avro.util.Utf8 name;
  public org.apache.avro.util.Utf8 displayName;
  public org.apache.avro.generic.GenericArray<org.apache.hadoop.mapreduce.jobhistory.JhCounter> counts;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return name;
    case 1: return displayName;
    case 2: return counts;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: name = (org.apache.avro.util.Utf8)value$; break;
    case 1: displayName = (org.apache.avro.util.Utf8)value$; break;
    case 2: counts = (org.apache.avro.generic.GenericArray<org.apache.hadoop.mapreduce.jobhistory.JhCounter>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
