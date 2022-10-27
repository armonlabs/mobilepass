//
//  PassFailCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 27.10.2022.
//

import Foundation

enum PassFailCode: Int, Codable {
    case REMOTE_ACCESS_FAILED               = 1001;
    case REMOTE_ACCESS_UNAUTHORIZED         = 1002;
    case REMOTE_ACCESS_NOT_CONNECTED        = 1003;
    case REMOTE_ACCESS_TIMEOUT              = 1004;
    case REMOTE_ACCESS_COULD_NOT_BE_SENT    = 1005;
    case REMOTE_ACCESS_REQUEST_ERROR        = 1006;
    case REMOTE_ACCESS_INVALID_QR_CONTENT   = 1007;
    case REMOTE_ACCESS_INVALID_QR_CODE_ID   = 1008;
    case BLUETOOTH_CONNECTION_FAILED        = 2001;
    case BLUETOOTH_CONNECTION_TIMEOUT       = 2002;
    case BLUETOOTH_EMPTY_CONFIG             = 2003;
    case BLUETOOTH_INVALID_QR_CONTENT       = 2004;
    case BLUETOOTH_INVALID_DIRECTION        = 2005;
    case BLUETOOTH_INVALID_HARDWARE_ID      = 2006;
    case BLUETOOTH_INVALID_RELAY_NUMBER     = 2007;
    case BLUETOOTH_DISABLED                 = 2008;
    case BLUETOOTH_INVALID_NEXT_ACTION      = 2009;
}
