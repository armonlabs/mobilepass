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
            case PacketHeaders.PROTOCOLV2.AUTH.CHALLENGE_RESULT_WITH_CODE:
                return parseAuthChallengeResultWithCode(data: typeData)
            default:
                return nil
            }
        default:
            return nil
        }
    }

    // MARK: Private Functions
    
    private func parseAuthChallengeData(data: Data) -> BLEDataContent? {
        // The capabilities byte is only sent by newer firmware. Older devices end
        // the packet after the iv, in which case processData simply stops and the
        // field is absent - equivalent to the documented "length > 83" check on
        // the full packet.
        let dataFormat = [
            BLEDataParseFormat(fieldName: "deviceId", length: 32, type: .string),
            BLEDataParseFormat(fieldName: "challenge", length: 32, type: .data),
            BLEDataParseFormat(fieldName: "iv", length: 16, type: .data),
            BLEDataParseFormat(fieldName: "capabilities", length: 1, type: .number)
        ]

        return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeForPublicKey,
                              result: DataTypes.RESULT.Succeed,
                              data:   processData(data: data, format: dataFormat))
    }

    private func parseAuthChallengeResult(data: Data) -> BLEDataContent? {
        if (data.isEmpty) {
            LogManager.shared.warn(message: "Challenge result received from device has no content!")
            return nil
        }

        switch Int(data[0]) {
        case PacketHeaders.PROTOCOLV2.COMMON.SUCCESS:
            return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeResult,
                                  result: DataTypes.RESULT.Succeed,
                                  data:   nil)
        case PacketHeaders.PROTOCOLV2.COMMON.FAILURE:
            let dataFormat = [
                BLEDataParseFormat(fieldName: "reason", length: 1, type: .number),
                BLEDataParseFormat(fieldName: "message", type: .string)
            ]
            
            return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeResult,
                                  result: DataTypes.RESULT.Failed,
                                  data:   processData(data: data.subdata(in: 1..<data.count), format: dataFormat))
        default:
            return nil
        }
    }

    /**
     * Parse the extended challenge result, answered only to a request that
     * declared result code support.
     *
     * Layout differs from the plain challenge result in two ways, so it is
     * parsed as its own branch: `reason` is present on success too, and the code
     * comes before the message. That ordering is deliberate on the device side -
     * a small MTU truncates the message and never the code.
     */
    private func parseAuthChallengeResultWithCode(data: Data) -> BLEDataContent? {
        if (data.isEmpty) {
            LogManager.shared.warn(message: "Challenge result with code received from device has no content!")
            return nil
        }

        let isSucceed: Bool

        switch Int(data[0]) {
        case PacketHeaders.PROTOCOLV2.COMMON.SUCCESS:
            isSucceed = true
        case PacketHeaders.PROTOCOLV2.COMMON.FAILURE:
            isSucceed = false
        default:
            LogManager.shared.warn(message: "Challenge result with code has unknown state: \(data[0])")
            return nil
        }

        let dataFormat = [
            BLEDataParseFormat(fieldName: "reason", length: 1, type: .number),
            BLEDataParseFormat(fieldName: "codeLength", length: 1, type: .number),
            BLEDataParseFormat(fieldName: "code", length: "codeLength", type: .string),
            BLEDataParseFormat(fieldName: "message", type: .string)
        ]

        return BLEDataContent(type:   DataTypes.TYPE.AuthChallengeResult,
                              result: isSucceed ? DataTypes.RESULT.Succeed : DataTypes.RESULT.Failed,
                              data:   processData(data: data.subdata(in: 1..<data.count), format: dataFormat))
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
            
            // Lengths that come from the packet itself (codeLength) are clamped to
            // the buffer: a truncated or malformed packet must not read past it.
            // A negative length means "use whatever is left".
            let endIndex = length >= 0 ? min(currentIndex + length, data.count) : data.count
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

            // Must match endIndex above, otherwise a zero or malformed length
            // shifts every following field
            currentIndex = endIndex
        }

        return result
    }
}
