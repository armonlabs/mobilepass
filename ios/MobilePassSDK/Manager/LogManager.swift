//
//  LogManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

class LogManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = LogManager()
    private override init() {
        super.init()
    }
    
    
    // MARK: Constants
    
    private let LOG_TAG: String = "MobilePass";
    
    
    // MARK: Public Functions
    
    func info(message: String) -> Void {
        log(level: .Info, message: message, code: nil)
    }
    
    func warn(message: String, code: LogCodes? = nil) -> Void {
        log(level: .Warn, message: message, code: code)
    }
    
    func error(message: String, code: LogCodes? = nil) -> Void {
        log(level: .Error, message: message, code: code)
    }
    
    func debug(message: String) -> Void {
        log(level: .Debug, message: message, code: nil)
    }
    
    func getVersion() -> String {
        return "1.6.2"
    }
    
    // MARK: Private Functions
    
    private func log(level: LogLevel, message: String, code: LogCodes?) -> Void {
        var prefix = ""
        
        switch level {
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
        
        print("\(LOG_TAG) [\(Date().getFormattedDate(format: "HH:mm:ss:SSS"))] - \(prefix) | \(message)")
        
        if (level.rawValue >= ConfigurationManager.shared.getLogLevel()) {
            DelegateManager.shared.onLogItemCreated(log: LogItem(level: level.rawValue, code: code?.rawValue, message: message))
        }
    }
}
