//
//  Configuration.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

public class Configuration {
    /** Member id that will be used for validation to pass */
    var memberId: String
    
    /** URL of server that communicate between SDK, devices and validation server */
    var serverUrl: String
    
    /** Information message for QR Code reader that will be shown at top of screen */
    var qrCodeMessage: String?
    
    /** OAuth token value of current user's session to validate */
    var token: String?
    
    /** Language code to localize texts [tr | en] */
    var language: String?
    
    
    public init(memberId: String, serverUrl: String) {
        self.memberId   = memberId
        self.serverUrl  = serverUrl
    }
    
    public init(memberId: String, serverUrl: String, qrCodeMessage: String) {
        self.memberId       = memberId
        self.serverUrl      = serverUrl
        self.qrCodeMessage  = qrCodeMessage
    }
    
    public init(memberId: String, serverUrl: String, token: String, language: String) {
        self.memberId   = memberId
        self.serverUrl  = serverUrl
        self.token      = token
        self.language   = language
    }
    
    public init(memberId: String, serverUrl: String, qrCodeMessage: String, token: String, language: String) {
        self.memberId       = memberId
        self.serverUrl      = serverUrl
        self.qrCodeMessage  = qrCodeMessage
        self.token          = token
        self.language       = language
    }
}
