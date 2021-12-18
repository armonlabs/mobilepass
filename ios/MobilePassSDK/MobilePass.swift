//
//  MobilePass.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 8.02.2021.
//

import Foundation
import UIKit

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
        } catch {
            LogManager.shared.error(message: "Set configuration with given parameters failed!")
        }
    }
    
    /**
     * To set or update OAuth token of user and language code
     *
     * @param token OAuth token value of current user's session to validate
     * @param language Language code to localize texts [tr | en]
     */
    public func updateToken(token: String, language: String) {
        do {
            try ConfigurationManager.shared.setToken(token: token, language: language)
        } catch {
            LogManager.shared.error(message: "Update token with given parameters failed!")
        }
    }
    
    /**
     * Starts qr code reading session and related flow
     *
     * ! Don't forget to set token before this call
     */
    public func triggerQRCodeRead() -> UIViewController {
        DelegateManager.shared.clearFlags()
        var controller = UIViewController()
        
        if #available(iOS 13.0, *) {
            controller = PassFlowController()
            DelegateManager.shared.setMainDelegate(delegate: delegate, viewController: controller)
        }
        
        BluetoothManager.shared.setReady()
        
        return controller;
    }
    
}
