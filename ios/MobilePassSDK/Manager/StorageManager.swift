//
//  StorageManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class StorageManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = StorageManager()
    private override init() {
        super.init()
    }
    
    // MARK: Constants
    
    private let MAX_LOG_COUNT: Int = 250
    
    // MARK: Public Functions
    
    func setValue(key: String, value: String, secure: Bool) throws -> Bool {
        if (secure) {
            try validateKey(key: key)
            return try storageSetValue(key: key, value: value)
        } else {
            let storage: UserDefaults = UserDefaults.standard
            storage.set(value, forKey: key)
            
            return true
        }
    }
    
    func getValue(key: String, secure: Bool) throws -> String {
        if (secure) {
            try validateKey(key: key)
            return try storageGetValue(key: key)
        } else {
            let storage: UserDefaults = UserDefaults.standard
            return storage.string(forKey: key) ?? ""
        }
    }
    
    func deleteValue(key: String, secure: Bool) throws -> Bool {
        if (secure) {
            try validateKey(key: key)
            storageDeleteValue(key: key)
            return true
        } else {
            return try setValue(key: "", value: key, secure: false)
        }
    }
    
    // MARK: Private Functions
    
    private func validateKey(key: String) throws -> Void {
        if (key.isEmpty) {
            throw ActionError(message: "Key is not provided for set operation", manager: ActionError.ManagerType.storage, code: nil)
        }
    }
    
    private func storageQueryWithKey(key: String) -> Dictionary<String, Any> {
        let service: String = "armon.storage.keychain"
        let encodedKey: Data = key.data(using: .utf8)!
        
        return [
            String(kSecClass): kSecClassGenericPassword,
            String(kSecAttrService): service,
            String(kSecAttrGeneric): encodedKey,
            String(kSecAttrAccount): encodedKey
        ]
    }
    
    private func storageSetValue(key: String, value: String) throws -> Bool {
        var storageDictionary = storageQueryWithKey(key: key)
        
        storageDictionary[String(kSecValueData)]       = value.data(using: .utf8)
        storageDictionary[String(kSecAttrAccessible)]  = kSecAttrAccessibleAfterFirstUnlock
        
        let status: OSStatus = SecItemAdd(storageDictionary as CFDictionary, nil)
        
        if (status == errSecSuccess) {
            return true
        } else if (status == errSecDuplicateItem) {
            return try storageUpdateValue(key: key, value: value)
        } else {
            throw ActionError(message: "Set value for " + key + " failed!", manager: ActionError.ManagerType.storage, code: getSecErrorCode(status: status))
        }
    }
    
    private func storageUpdateValue(key: String, value: String) throws -> Bool {
        let storageDictionary = storageQueryWithKey(key: key)
        let updateDictionary = [
            String(kSecValueData): value.data(using: .utf8)
        ]
        
        let status: OSStatus = SecItemUpdate(storageDictionary as CFDictionary, updateDictionary as CFDictionary)
        
        if (status == errSecSuccess) {
            return true
        } else {
            throw ActionError(message: "Update value for " + key + " failed", manager: ActionError.ManagerType.storage, code: getSecErrorCode(status: status))
        }
    }
    
    private func storageGetValue(key: String) throws -> String {
        let data: Data? = try searchKeychainWithKey(key: key)
        
        if (data != nil) {
            return String(data: data!, encoding: .utf8)!
        } else {
            LogManager.shared.info(message: "Empty data received from keychain for " + key)
            return ""
        }
    }
    
    private func storageDeleteValue(key: String) {
        let storageDictionary = storageQueryWithKey(key: key)
        SecItemDelete(storageDictionary as CFDictionary)
    }
    
    private func searchKeychainWithKey(key: String) throws -> Data? {
        var storageDictionary = storageQueryWithKey(key: key)
        
        storageDictionary[String(kSecMatchLimit)] = kSecMatchLimitOne
        storageDictionary[String(kSecReturnData)] = true
        
        var item: CFTypeRef?
        let status: OSStatus = SecItemCopyMatching(storageDictionary as CFDictionary, &item)
        
        if status == noErr {
            return item as! Data?
        } else if status == errSecItemNotFound {
            return nil
        } else {
            throw ActionError(message: "Search data in keychain failed for " + key, manager: ActionError.ManagerType.storage, code: getSecErrorCode(status: status))
        }
    }
    
    private func getSecErrorCode(status: OSStatus) -> Int {
        switch status {
        case errSecUnimplemented:
            // Function or operation not implemented
            return 10001
        case errSecIO:
            // I/O error
            return 10002
        case errSecOpWr:
            // File already open with with write permission
            return 10003
        case errSecParam:
            // One or more parameters passed to a function where not valid
            return 10004
        case errSecAllocate:
            // Failed to allocate memory
            return 10005
        case errSecUserCanceled:
            // User canceled the operation
            return 10006
        case errSecBadReq:
            // Bad parameter or invalid state for operation
            return 10007
        case errSecNotAvailable:
            // No keychain is available. You may need to restart your computer
            return 10008
        case errSecDuplicateItem:
            // The specified item already exists in the keychain
            return 10009
        case errSecItemNotFound:
            // The specified item could not be found in the keychain
            return 10010
        case errSecInteractionNotAllowed:
            // User interaction is not allowed
            return 10011
        case errSecDecode:
            // Unable to decode the provided data
            return 10012
        case errSecAuthFailed:
            // The user name or passphrase you entered is not correct
            return 10013
        default:
            return 10000
        }
    }
}
