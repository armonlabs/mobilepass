//
//  ConfigurationManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

enum ConfigurationError: Error {
    case validationError(String)
}

class ConfigurationManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = ConfigurationManager()
    private override init() {
        super.init()
        LogManager.shared.info(message: "Setting up Configuration Manager instance")
    }
    
    // MARK: Private Fields
    
    private var mCurrentConfig:     Configuration?
    private var mCurrentKeyPair:    CryptoKeyPair?
    private var mCurrentQRCodes:    Dictionary<String, QRCodeContent> = [:]
    private var mUserKeyDetails:    [StorageDataUserDetails] = []
    
    // MARK: Public Methods
    
    public func setConfig(data: Configuration) throws -> Void {
        mCurrentConfig = data
        
        getStoredQRCodes()
        
        try validateConfig()
        try sendUserData()
    }
    
    public func setToken(token: String, language: String) throws -> Void {
        if (mCurrentConfig != nil) {
            mCurrentConfig!.token = token
            mCurrentConfig!.language = language
            
            try sendUserData();
        }
    }
    
    public func getQRCodeContent(qrCodeData: String) -> QRCodeContent? {
        return mCurrentQRCodes.index(forKey: qrCodeData) != nil ? mCurrentQRCodes[qrCodeData]! : nil
    }
    
    public func getMemberId() -> String {
        return mCurrentConfig?.memberId ?? ""
    }
    
    public func getPrivateKey() -> String {
        return mCurrentKeyPair?.privateKey ?? ""
    }
    
    public func getServerURL() -> String {
        var serverUrl: String = mCurrentConfig?.serverUrl ?? ""
        
        if (serverUrl.count > 0 && !serverUrl.hasSuffix("/")) {
            serverUrl += "/"
        }
        
        return serverUrl
    }
    
    public func getMessageQRCode() -> String {
        return mCurrentConfig?.qrCodeMessage ?? ""
    }
    
    public func getToken() -> String {
        return mCurrentConfig?.token ?? ""
    }
    
    public func getLanguage() -> String {
        return mCurrentConfig?.language ?? "en"
    }
    
    public func isMockLocationAllowed() -> Bool {
        return mCurrentConfig?.allowMockLocation ?? false
    }
    
    // MARK: Private Methods
    
    private func getStoredQRCodes() -> Void {
        let storageQRCodes: String? = try? StorageManager.shared.getValue(key: StorageKeys.QRCODES, secure: false)
        mCurrentQRCodes = (storageQRCodes != nil && storageQRCodes!.count > 0 ? try? JSONUtil.shared.decodeJSONData(jsonString: storageQRCodes!) : [:]) ?? [:]
    }
    
    private func validateConfig() throws -> Void {
        if (mCurrentConfig == nil) {
            throw ConfigurationError.validationError("Configuration is required for MobilePass");
        }
        
        if (mCurrentConfig?.memberId == nil || mCurrentConfig?.memberId.count == 0) {
            throw ConfigurationError.validationError("Provide valid Member Id to continue, received data is empty!");
        }
        
        if (mCurrentConfig?.serverUrl == nil || mCurrentConfig?.serverUrl.count == 0) {
            throw ConfigurationError.validationError("Provide valid Server URL to continue, received data is empty!");
        }
    }
    
    private func checkKeyPair() throws -> Void {
        let storedUserKeys: String = try StorageManager.shared.getValue(key: StorageKeys.USER_DETAILS, secure: true)
        
        if (storedUserKeys.count > 0) {
            mUserKeyDetails = try JSONUtil.shared.decodeJSONArray(jsonString: storedUserKeys)
            
            for user in mUserKeyDetails {
                if (user.userId == getMemberId()) {
                    mCurrentKeyPair = CryptoKeyPair(publicKey: user.publicKey, privateKey: user.privateKey)
                    break
                }
            }
        }
        
        if (mCurrentKeyPair == nil) {
            mCurrentKeyPair = CryptoManager.shared.generateKeyPair()
            
            mUserKeyDetails.append(StorageDataUserDetails(userId: getMemberId(), publicKey: mCurrentKeyPair!.publicKey, privateKey: mCurrentKeyPair!.privateKey))
            let jsonString = try JSONUtil.shared.encodeJSONArray(data: mUserKeyDetails)
            
            _ = try StorageManager.shared.setValue(key: StorageKeys.USER_DETAILS, value: jsonString, secure: true)
        }
    }
    
    private func sendUserData() throws -> Void {
        if (mCurrentKeyPair == nil) {
            try checkKeyPair();
        }
        
        DataService().sendUserInfo(request: RequestSetUserData(publicKey: mCurrentKeyPair!.publicKey, memberId: getMemberId()), completion: { (result) in
            if case .success(_) = result {
                self.getAccessPoints()
            } else {
                LogManager.shared.error(message: "Send user info to server failed!")
                self.getAccessPoints()
            }
        })
    }
    
    private func getAccessPoints() -> Void {
        self.mCurrentQRCodes.removeAll()
        
        DataService().getAccessList(request: RequestPagination(take: 100, skip: 0), completion: { (result) in
            if case .success(let receivedData) = result {
                self.mCurrentQRCodes = [:]
                
                for item in (receivedData?.items ?? []) {
                    for qrCode in item.qrCodeData {
                        var content = QRCodeContent(code: qrCode.qrCodeData, accessPoint: item, action: qrCode)
                        content.accessPoint.qrCodeData = []
                        
                        self.mCurrentQRCodes[qrCode.qrCodeData] = content
                    }
                }
                
                for qrCode in self.mCurrentQRCodes {
                    LogManager.shared.debug(message: "\(qrCode.key) > Type: \(qrCode.value.action.config.trigger.type) | Direction: \(qrCode.value.action.config.direction) | Validate Location: \(String(describing: qrCode.value.action.config.trigger.validateGeoLocation))")
                }
                
                do {
                    let valueQRCodesToStore: String? = try? JSONUtil.shared.encodeJSONData(data: self.mCurrentQRCodes)
                    _ = try StorageManager.shared.setValue(key: StorageKeys.QRCODES, value: valueQRCodesToStore ?? "", secure: false)
                } catch {
                    LogManager.shared.error(message: "Store received access list failed!")
                }
                
                DelegateManager.shared.qrCodeListChanged(state: self.mCurrentQRCodes.count > 0 ? QRCodeListState.USING_SYNCED_DATA : QRCodeListState.EMPTY)
            } else {
                LogManager.shared.error(message: "Get access list failed!")
                
                DelegateManager.shared.qrCodeListChanged(state: self.mCurrentQRCodes.count > 0 ? QRCodeListState.USING_STORED_DATA : QRCodeListState.EMPTY)
            }
        })
    }
    
}
