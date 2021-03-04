//
//  LogManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

enum LogType: Int, Codable {
    case Info   = 1
    case Warn   = 2
    case Error  = 3
    case Debug  = 4
}

class LogManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = LogManager()
    private override init() {
        super.init()
        self.info(message: "Setting up Log Manager instance")
    }
    
    
    // MARK: Constants
    
    private let LOG_TAG: String = "MobilePass";
    
    
    // MARK: Public Functions
    
    func info(message: String) -> Void {
        log(type: .Info, message: message)
    }
    
    func warn(message: String) -> Void {
        log(type: .Warn, message: message)
    }
    
    func error(message: String) -> Void {
        log(type: .Error, message: message)
    }
    
    func debug(message: String) -> Void {
        log(type: .Debug, message: message)
    }
    
    // MARK: Private Functions
    
    private func log(type: LogType, message: String) -> Void {
        var prefix = ""
        
        switch type {
        case .Info:
            prefix = "INFO"
            break
        case .Warn:
            prefix = "WARN"
            break
        case .Error:
            prefix = "ERROR"
            break
        case .Debug:
            prefix = "DEBUG"
            break
        }
        
        print(LOG_TAG + " - " + prefix + " | " + message)
    }
}
