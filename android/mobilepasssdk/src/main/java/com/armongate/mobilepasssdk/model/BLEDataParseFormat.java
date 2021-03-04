package com.armongate.mobilepasssdk.model;

public class BLEDataParseFormat {

    public enum DataType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATA
    }

    public String   fieldName;
    public DataType dataType;
    public Integer  dataLength;
    public String   useLengthFromField;
    public Boolean  useLeftData;

    public BLEDataParseFormat(String fieldName, int length, DataType type) {
        this.fieldName  = fieldName;
        this.dataType   = type;
        this.dataLength = length;
    }

    public BLEDataParseFormat(String fieldName, String length, DataType type) {
        this.fieldName          = fieldName;
        this.dataType           = type;
        this.useLengthFromField = length;
    }

    public BLEDataParseFormat(String fieldName, DataType type) {
        this.fieldName      = fieldName;
        this.dataType       = type;
        this.useLeftData    = true;
    }

}
