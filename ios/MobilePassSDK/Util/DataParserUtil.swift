//
//  DataParserUtil.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class DataParserUtil: NSObject {
    
    // MARK: Singleton
    
    static let shared = DataParserUtil()
    private override init() {
        super.init()
    }
        
    // MARK: Public Functions
    
    func parse(data: Data) -> BLEDataContent? {
        let protocolData = data.subdata(in: 0..<1)
        
        if (!protocolData.isEmpty) {
            
            switch protocolData.toInt() {
            case 1:
                return parseForProtocolV1(data: data.subdata(in: 1..<data.count))
            case 2:
                return parseForProtocolV2(data: data.subdata(in: 1..<data.count))
            default:
                // TODO Add message to unknown protocol
                LogManager.shared.warn(message: "Unknown data protocol received!")
                return nil
            }
            
        } else {
            LogManager.shared.warn(message: "Invalid data received to parse")
            return nil
        }
    }
    
    // MARK: Private Functions
    
    private func parseForProtocolV1(data: Data) -> BLEDataContent? {
        LogManager.shared.error(message: "Procotol 1 is ignored to process response!");
        return nil
    }
    
    private func parseForProtocolV2(data: Data) -> BLEDataContent? {
        if (data.count < 2) {
            LogManager.shared.warn(message: "Data received from device has invalid length!")
            return nil
        }
                
        switch Int(data[0]) {
        case PacketHeaders.PROTOCOLV2.GROUP.AUTH:
            let typeData = data.subdata(in: 2..<data.count)
            
            switch Int(data[1]) {
            case PacketHeaders.PROTOCOLV2.AUTH.PUBLICKEY_CHALLENGE:
                return parseAuthChallengeData(data: typeData)
            case PacketHeaders.PROTOCOLV2.AUTH.CHALLENGE_RESULT:
                return parseAuthChallengeResult(data: typeData)
            default:
                return nil
            }
        default:
            return nil
        }
    }

    // MARK: Private Functions
    
    private func parseAuthChallengeData(data: Data) -> BLEDataContent? {
        let dataFormat = [
            BLEDataParseFormat(fieldName: "deviceId", length: 32, type: .string),
            BLEDataParseFormat(fieldName: "challenge", length: 32, type: .data),
            BLEDataParseFormat(fieldName: "iv", length: 16, type: .data)
        ]
        
        return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeForPublicKey,
                              result: DataTypes.RESULT.Succeed,
                              data:   processData(data: data, format: dataFormat))
    }
    
    private func parseAuthChallengeResult(data: Data) -> BLEDataContent? {
        switch Int(data[0]) {
        case PacketHeaders.PROTOCOLV2.COMMON.SUCCESS:
            return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeResult,
                                  result: DataTypes.RESULT.Succeed,
                                  data:   nil)
        case PacketHeaders.PROTOCOLV2.COMMON.FAILURE:
            let dataFormat = [
                BLEDataParseFormat(fieldName: "reason", length: 1, type: .number),
            ]
            
            return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeResult,
                                  result: DataTypes.RESULT.Failed,
                                  data:   processData(data: data.subdata(in: 1..<data.count), format: dataFormat))
        default:
            return nil
        }
    }
    
    private func processData(data: Data, format: [BLEDataParseFormat]) -> Dictionary<String, Any> {
        var result:         [String: Any]   = [:]
        var currentIndex:   Int             = 0
        
        for formatItem in format {
            if (currentIndex >= data.count) {
                // Completed
                break
            }

            var removeLengthField = false
            var length = 0
            if (formatItem.useLeftData != nil && formatItem.useLeftData == true) {
                length = -1
            } else if (formatItem.useLengthFromField != nil && !formatItem.useLengthFromField!.isEmpty) {
                length = result[formatItem.useLengthFromField!] as? Int ?? 0
                removeLengthField = true
            } else {
                length = formatItem.dataLength!
            }
            
            let endIndex = length > 0 ? (currentIndex + length) : data.count
            let subData = data.subdata(in: currentIndex..<endIndex)
            
            if (formatItem.dataType == .string) {
                result[formatItem.fieldName] = subData.toString()
            } else if (formatItem.dataType == .number) {
                result[formatItem.fieldName] = subData.toInt()
            } else if (formatItem.dataType == .boolean) {
                result[formatItem.fieldName] = subData.toBool()
            } else if (formatItem.dataType == .data) {
                result[formatItem.fieldName] = subData
            } else {
                result[formatItem.fieldName] = ""
            }
            
            if (removeLengthField) {
                result.removeValue(forKey: formatItem.useLengthFromField!)
            }
            
            currentIndex = length > 0 ? (currentIndex + length) : data.count
        }

        return result
    }
}
