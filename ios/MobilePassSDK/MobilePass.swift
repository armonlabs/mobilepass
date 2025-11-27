//
//  MobilePass.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 8.02.2021.
//

import Foundation

public class MobilePass {
    
    public var delegate: MobilePassDelegate?
    
    /**
     * Initializes new Mobile Pass instance
     */
    public init(config: Configuration) {
        do {
            if (config.delegate != nil) {
                self.delegate = config.delegate
                DelegateManager.shared.setMainDelegate(delegate: delegate)
            }
            
            ConfigurationManager.shared.setConfig(data: config)
            
            LogManager.shared.info(message: "SDK Version: \(LogManager.shared.getVersion())")
            LogManager.shared.info(message: "Configuration: \(config.getLog())")
            
            try ConfigurationManager.shared.setReady()
            
            // Initialize Bluetooth manager early so state is ready when QR code is scanned
            BluetoothManager.shared.setReady()
        } catch {
            LogManager.shared.error(message: "Set configuration with given parameters failed!")
        }
    }
    
    /**
     * Sync QR code list from server
     */
    public func sync() {
        ConfigurationManager.shared.refreshList()
    }
    
    /**
     * Process scanned QR code data from external scanner
     *
     * @param data QR code string data scanned by the app
     * @return QRCodeProcessResult with validation status and requirements
     */
    public func processQRCode(data: String) -> QRCodeProcessResult {
        PassFlowManager.shared.clearStates()
        
        return PassFlowManager.shared.processQRCode(data: data)
    }
    
    /**
     * Confirm that location has been verified by the app
     *
     * Call this after your app:
     * 1. Receives onLocationVerificationRequired callback with lat/lon/radius
     * 2. Gets user's location and calculates distance
     * 3. Shows UI and user confirms or is within radius
     *
     * The app is responsible for all location verification logic and UI
     * SDK will automatically continue the pass flow after this call
     */
    public func confirmLocationVerified() {
        PassFlowManager.shared.confirmLocationVerified()
    }
    
    /**
     * Cancel current flow - stops any active Bluetooth operations and resets state
     */
    public func cancelFlow() {
        PassFlowManager.shared.cancelFlow()
    }
    
}
