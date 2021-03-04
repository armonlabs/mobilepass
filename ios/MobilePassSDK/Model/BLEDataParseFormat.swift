//
//  BLEDataParseFormat.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

struct BLEDataParseFormat {
    enum DataType {
        case string
        case number
        case boolean
        case data
    }
    
    var fieldName:          String
    var dataType:           DataType
    var dataLength:         Int?
    var useLengthFromField: String?
    var useLeftData:        Bool?
    
    /*!
    *  Initialize data format for packet's part
    *
    *  @param fieldName Field name that will be used for dictionary key.
    *  @param length    Data length to parse from packet
    *  @param type      Data type for conversion
    */
    init(fieldName: String, length: Int, type: DataType) {
        self.fieldName  = fieldName
        self.dataType   = type
        self.dataLength = length
    }
    
    /*!
    *  Initialize data format for packet's part
    *
    *  Uses given fieldName to determine length of data
    *
    *  @param fieldName Field name that will be used for dictionary key.
    *  @param length    Data length will be used from result dictionary with given field name
    *  @param type      Data type for conversion
    */
    init(fieldName: String, length: String, type: DataType) {
        self.fieldName          = fieldName
        self.dataType           = type
        self.useLengthFromField = length
    }
    
    /*!
    *  Initialize data format for packet's part
    *
    *  Uses total data length to parse data from packet
    *
    *  @param fieldName Field name that will be used for dictionary key.
    *  @param type      Data type for conversion
    */
    init(fieldName: String, type: DataType) {
        self.fieldName      = fieldName
        self.dataType       = type
        self.useLeftData    = true
    }
    
}
