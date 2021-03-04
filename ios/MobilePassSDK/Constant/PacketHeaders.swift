//
//  PacketHeaders.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

struct PacketHeaders {
    
    static let PLATFORM_IOS = 0xF1
    
    enum PROTOCOLV2 {
        enum COMMON {
            static let FAILURE = 0x20
            static let SUCCESS = 0x21
        }
        enum GROUP {
            static let AUTH = 0x01
        }
        enum AUTH {
            static let PUBLICKEY_CHALLENGE  = 0x01
            static let CHALLENGE_RESULT     = 0x03
            static let DIRECTION_CHALLENGE  = 0x05
        }
        enum FAILURE_REASON {
            static let NOACCESSRIGHT                = 0x01
            static let UNKNOWN_CREDENTIAL_OWNER     = 0x02
            static let INVALID_CONFIGURATION        = 0x03
            static let UNHANDLED_FAILURE            = 0x04
            static let ANTIPASSBACK_REJECT          = 0x05
            static let ANTIPASSBACK_TIMEOUT         = 0x06
            static let CREDENTIAL_EXPIRED           = 0x08
            static let INSUFFICIENT_FUND            = 0x09
            static let RULE_REJECT                  = 0x0A
            static let STATE_OPENED                 = 0x0C
            static let STATE_DISABLED               = 0x0D
            static let MULTIFACTOR_REQUIRED         = 0x0E
            static let USER_FORBIDDEN               = 0x0F
            static let USER_DISABLED                = 0x10
            static let RELAY_NOT_AVAILABLE          = 0x11
            static let CHALLENGE_FAIL               = 0x12
            static let INVALID_CHALLENGE            = 0x13
            static let MIFARE_FINGERPRINT_NOT_MATCH = 0x14
            static let MIFARE_FINGERPRINT_TIMEOUT   = 0x15
            static let REGION_CAPACITY_FULL         = 0x16
        }
    }
}
