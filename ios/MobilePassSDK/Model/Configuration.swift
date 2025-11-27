//
//  Configuration.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

public class Configuration {
    /** API key for SDK authentication - Each authorized application receives a unique key */
    var apiKey: String
    
    /** Member id that will be used for validation to pass */
    var memberId: String
    
    /** Barcode id that received with Benefits System */
    var barcode: String?
    
    /** URL of server that communicate between SDK, devices and validation server */
    var serverUrl: String
    
    /** Language code to localize texts [tr | en] */
    var language: String?
        
    /** Bluetooth connection timeout in seconds, default 5 seconds */
    var connectionTimeout: Int?
    
    /** Location verification timeout in seconds (for remote access with location requirement), default 30 seconds */
    var locationVerificationTimeout: Int?
        
    /**
        Flag to decide action when BLE is unavailable (disabled, missing permissions, etc.)
        "true" means continue to next action (e.g., remote access) if BLE requirements not met
        "false" means wait for user to satisfy BLE requirements
    
        @default false
     */
    var continueWithoutBLE: Bool?
    
    /** Minimum level to be informed about logs, default LogLevel.INFO (2) */
    var logLevel: LogLevel?
    
    /** Optional listener instance for MobilePass SDK callbacks */
    var delegate: MobilePassDelegate?
    
    
    public init(apiKey: String,
                memberId: String,
                serverUrl: String,
                barcode: String?,
                language: String?,
                connectionTimeout: Int?,
                locationVerificationTimeout: Int?,
                continueWithoutBLE: Bool?,
                logLevel: LogLevel? = nil,
                delegate: MobilePassDelegate? = nil) {
        self.apiKey                 = apiKey
        self.memberId               = memberId
        self.barcode                = barcode
        self.serverUrl              = serverUrl
        self.language               = language
        self.connectionTimeout      = connectionTimeout
        self.locationVerificationTimeout = locationVerificationTimeout
        self.continueWithoutBLE     = continueWithoutBLE
        self.logLevel               = logLevel
        self.delegate               = delegate
    }
    
    public func getLog() -> String {
        let maskedKey = apiKey.count > 8 ? String(apiKey.prefix(8)) + "..." : apiKey
        return "ApiKey: \(maskedKey) | MemberId: \(memberId ) | Barcode: \(barcode ?? "-") | ContinueWithoutBLE: \((continueWithoutBLE ?? ConfigurationDefaults.ContinueWithoutBLE).description) | BLEConnectionTimeout: \((connectionTimeout ?? ConfigurationDefaults.BLEConnectionTimeout).description)";
    }
    
}
