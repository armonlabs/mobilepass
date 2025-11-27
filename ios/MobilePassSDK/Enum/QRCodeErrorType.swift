//
//  QRCodeErrorType.swift
//  MobilePassSDK
//
//  Created by Mobile Pass SDK
//

import Foundation

public enum QRCodeErrorType: Int, Codable {
    case invalidFormat = 1      // Malformed QR data (scanning issue)
    case notFound = 2           // Valid format but not in authorized list
    case expired = 3            // Valid but expired (reserved for future use)
    case unauthorized = 4       // Valid but user not authorized (reserved for future use)
}

