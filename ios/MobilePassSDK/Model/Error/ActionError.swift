//
//  ActionError.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

struct ActionError: Error {
    
    enum ManagerType {
        case bluetooth
        case crypto
        case storage
    }
    
    var message:    String
    var code:       Int
    var manager:    ManagerType
    
    private var managerTag: String {
        switch self.manager {
        case .bluetooth:
            return "Bluetooth Manager"
        case .crypto:
            return "Crypto Manager"
        case .storage:
            return "Storage Manager"
        }
    }
    
    var localizedDescription: String {
        let logPrefix = self.managerTag.isEmpty ? self.code.description : self.managerTag + " / " + self.code.description
        return logPrefix + " | " + self.message
    }
    
    init(message: String, manager: ManagerType, code: Int?) {
        self.code       = code ?? -1000
        self.message    = message
        self.manager    = manager
        
        LogManager.shared.error(message: self.localizedDescription)
    }
        
}
