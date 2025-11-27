//
//  QRCodeProcessResult.swift
//  MobilePassSDK
//
//  Created by Mobile Pass SDK
//

import Foundation

public struct QRCodeProcessResult {
    public var isValid: Bool
    public var errorType: QRCodeErrorType?
    
    init(isValid: Bool, errorType: QRCodeErrorType? = nil) {
        self.isValid = isValid
        self.errorType = errorType
    }
}

