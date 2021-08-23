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
    }
    
    // MARK: Private Fields
    
    private var mCurrentConfig:     Configuration?
    private var mCurrentKeyPair:    CryptoKeyPair?
    private var mQRCodes:           Dictionary<String, QRCodeMatch> = [:]
    private var mAccessPoints:      Dictionary<String, ResponseAccessPointListItem> = [:]
    private var mTempList:          [ResponseAccessPointListItem] = []
    private var mPagination:        RequestPagination? = nil
    private var mListSyncDate:      Int64? = nil
    private var mListClearFlag:     Bool = false
    private var mReceivedItemCount: Int = 0
    private var mUserKeyDetails:    [StorageDataUserDetails] = []
    
    // MARK: Public Methods
    
    public func setConfig(data: Configuration) -> Void {
        mCurrentConfig = data
    }
    
    public func setReady() throws -> Void {
        getStoredQRCodes()
        
        try validateConfig()
        try sendUserData()
    }
    
    public func setToken(token: String, language: String) throws -> Void {
        if (mCurrentConfig != nil) {
            mCurrentConfig!.token = token
            mCurrentConfig!.language = language
            
            try sendUserData()
        }
    }
    
    public func getQRCodeContent(qrCodeData: String) -> QRCodeContent? {
        let match = mQRCodes.index(forKey: qrCodeData) != nil ? mQRCodes[qrCodeData]! : nil
        
        if (match != nil) {
            let details = mAccessPoints.index(forKey: match!.accessPointId) != nil ? mAccessPoints[match!.accessPointId] : nil
            
            if (details != nil) {
                return QRCodeContent(accessPointId: match!.accessPointId, terminals: details!.t, qrCode: match!.qrCode, geoLocation: details!.g)
            }
        }
        
        return nil
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
        return mCurrentConfig?.token ?? "unknown"
    }
    
    public func getLanguage() -> String {
        return mCurrentConfig?.language ?? ConfigurationDefaults.Language
    }
    
    public func isMockLocationAllowed() -> Bool {
        return mCurrentConfig?.allowMockLocation ?? ConfigurationDefaults.AllowMockLocation
    }
    
    public func bleConnectionTimeout() -> Int {
        return mCurrentConfig?.connectionTimeout ?? ConfigurationDefaults.BLEConnectionTimeout
    }
    
    public func autoCloseTimeout() -> Int? {
        return mCurrentConfig?.autoCloseTimeout
    }
    
    public func waitForBLEEnabled() -> Bool {
        return mCurrentConfig?.waitBLEEnabled ?? ConfigurationDefaults.WaitBleEnabled
    }
    
    public func getLogLevel() -> Int {
        return mCurrentConfig?.logLevel?.rawValue ?? LogLevel.Info.rawValue
    }
    
    public func getConfigurationLog() -> String {
        return mCurrentConfig?.getLog() ?? ""
    }
    
    public func refreshList() -> Void {
        if (DelegateManager.shared.isQRCodeListRefreshable()) {
            LogManager.shared.info(message: "QR Code definition list will be retrieved again with user request")
            getAccessPoints(clear: true)
        }
    }
 
    // MARK: Private Methods
    
    private func getStoredQRCodes() -> Void {
        _ = try? StorageManager.shared.deleteValue(key: StorageKeys.QRCODES, secure: false)
        
        let storageQRCodes:         String? = try? StorageManager.shared.getValue(key: StorageKeys.LIST_QRCODES, secure: false)
        let storageAccessPoints:    String? = try? StorageManager.shared.getValue(key: StorageKeys.LIST_ACCESSPOINTS, secure: false)
        
        mQRCodes        = (storageQRCodes != nil && storageQRCodes!.count > 0 ? try? JSONUtil.shared.decodeJSONData(jsonString: storageQRCodes!) : [:]) ?? [:]
        mAccessPoints   = (storageAccessPoints != nil && storageAccessPoints!.count > 0 ? try? JSONUtil.shared.decodeJSONData(jsonString: storageAccessPoints!) : [:]) ?? [:]
        
        LogManager.shared.info(message: "Stored QR Code list is ready to use, total: \(mQRCodes.count)")
        LogManager.shared.info(message: "Stored Access Point list is ready to use, total: \(mAccessPoints.count)")
    }
    
    private func validateConfig() throws -> Void {
        if (mCurrentConfig == nil) {
            let message = "Configuration is required for MobilePass"
            LogManager.shared.error(message: message, code: LogCodes.CONFIGURATION_VALIDATION)
            throw ConfigurationError.validationError(message)
        }
        
        if (mCurrentConfig?.memberId == nil || mCurrentConfig?.memberId.count == 0) {
            let message = "Provide valid Member Id to continue, received data is empty!"
            LogManager.shared.error(message: message, code: LogCodes.CONFIGURATION_VALIDATION)
            throw ConfigurationError.validationError(message)
        }
        
        if (mCurrentConfig?.serverUrl == nil || mCurrentConfig?.serverUrl.count == 0) {
            let message = "Provide valid Server URL to continue, received data is empty!"
            LogManager.shared.error(message: message, code: LogCodes.CONFIGURATION_VALIDATION)
            throw ConfigurationError.validationError(message)
        }
    }
    
    private func checkKeyPair() throws -> Bool {
        mCurrentKeyPair = nil
        
        var newlyCreated: Bool = false
        let storedUserKeys: String = try StorageManager.shared.getValue(key: StorageKeys.USER_DETAILS, secure: true)
        
        if (storedUserKeys.count > 0) {
            mUserKeyDetails = try JSONUtil.shared.decodeJSONArray(jsonString: storedUserKeys)
            
            for user in mUserKeyDetails {
                if (user.userId == getMemberId()) {
                    LogManager.shared.info(message: "Key pair has already been generated for member id: \(getMemberId())")
                    mCurrentKeyPair = CryptoKeyPair(publicKey: user.publicKey, privateKey: user.privateKey)
                    break
                }
            }
        }
        
        if (mCurrentKeyPair == nil) {
            newlyCreated = true
            
            mCurrentKeyPair = CryptoManager.shared.generateKeyPair()
            LogManager.shared.info(message: "New key pair is generated for member id: \(getMemberId())")
            
            mUserKeyDetails.append(StorageDataUserDetails(userId: getMemberId(), publicKey: mCurrentKeyPair!.publicKey, privateKey: mCurrentKeyPair!.privateKey))
            let jsonString = try JSONUtil.shared.encodeJSONArray(data: mUserKeyDetails)
            
            _ = try StorageManager.shared.setValue(key: StorageKeys.USER_DETAILS, value: jsonString, secure: true)
        }
        
        return newlyCreated
    }
    
    private func sendUserData() throws -> Void {
        var needUpdate: Bool = try checkKeyPair()
        
        let storedMemberId: String? = try? StorageManager.shared.getValue(key: StorageKeys.MEMBERID, secure: false)
        if (storedMemberId == nil || storedMemberId!.count == 0 || storedMemberId! != getMemberId()) {
            needUpdate = true
        }
        
        if (needUpdate) {
            DataService().sendUserInfo(request: RequestSetUserData(publicKey: mCurrentKeyPair!.publicKey, clubMemberId: getMemberId()), completion: { (result) in
                if case .success(_) = result {
                    do {
                        _ = try StorageManager.shared.setValue(key: StorageKeys.MEMBERID, value: self.getMemberId(), secure: false)
                        LogManager.shared.info(message: "User info has been shared with server successfully and member id is ready now")
                    } catch {
                        LogManager.shared.warn(message: "Error occurred while storing member id after sharing with server", code: LogCodes.CONFIGURATION_SERVER_SYNC_INFO)
                    }
                    self.getAccessPoints(clear: false)
                } else {
                    LogManager.shared.error(message: "Sending user info to server has failed", code: LogCodes.CONFIGURATION_SERVER_SYNC_INFO)
                    self.getAccessPoints(clear: false)
                }
            })
        } else {
            LogManager.shared.info(message: "User info has already been sent to server for member id: \(getMemberId())")
            self.getAccessPoints(clear: false)
        }
    }
    
    private func processQRCodesResponse(result: Result<ResponseAccessPointList?, RequestError>) {
        if case .success(let receivedData) = result {
            if (receivedData == nil) {
                LogManager.shared.error(message: "Empty data received for access points list", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                return
            }
            
            if (receivedData!.i == nil || receivedData!.p == nil) {
                LogManager.shared.error(message: "Synchronization list response has invalid fields", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                return
            }
            
            self.mReceivedItemCount += receivedData!.i!.count
            
            for item in (receivedData?.i ?? []) {
                self.mTempList.append(item)
                
                // Open to show qr codes list
                /*
                do {
                    LogManager.shared.debug(message: "\(try JSONUtil.shared.encodeJSONData(data: item))")
                } catch { }
                */
            }
            
            
            
            if (receivedData!.p!.c > self.mReceivedItemCount) {
                self.mPagination?.s = self.mReceivedItemCount
                self.fetchAccessPoints()
            } else {
                if (mListClearFlag) {
                    self.mQRCodes       = [:]
                    self.mAccessPoints  = [:]
                    
                    LogManager.shared.debug(message: "Stored qr code and access points lists are cleared")
                }
                
                LogManager.shared.info(message: "Total \(mTempList.count) item(s) received from server for definition list of access points");
                
                for var item in self.mTempList {
                    if (item.i != nil && !item.i!.isEmpty) {
                        let storedAccessPoint = mAccessPoints.index(forKey: item.i!) != nil ? mAccessPoints[item.i!] : nil
                        
                        var storedQRCodeData:   [String] = storedAccessPoint != nil ? storedAccessPoint!.d ?? [] : []
                        var newQRCodeData:      [String] = []
                        
                        if (item.q != nil) {
                            for qrCode in item.q! {
                                if (qrCode.q != nil && !qrCode.q!.isEmpty) {
                                    if let index = storedQRCodeData.firstIndex(of: qrCode.q!) {
                                        storedQRCodeData.remove(at: index)
                                    }
                                    
                                    newQRCodeData.append(qrCode.q!)
                                    self.mQRCodes[qrCode.q!] = QRCodeMatch(accessPointId: item.i!, qrCode: qrCode)
                                } else {
                                    LogManager.shared.warn(message: "QR code data is empty that received in synchronization for access point id \(item.i!)", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                                }
                            }
                        } else {
                            LogManager.shared.warn(message: "QR code list received empty while synchronization for access point id \(item.i!)", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                        }
                        
                        for qrCodeData in storedQRCodeData {
                            mQRCodes[qrCodeData] = nil
                            LogManager.shared.info(message: "\(qrCodeData) is removed from definitions");
                        }
                        
                        item.q = []
                        item.d = newQRCodeData
                        
                        self.mAccessPoints[item.i!] = item
                    } else {
                        LogManager.shared.warn(message: "Updated or added item received from server but id is empty!", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                    }
                }
                
                do {
                    let valueQRCodesToStore:        String? = try? JSONUtil.shared.encodeJSONData(data: self.mQRCodes)
                    let valueAccessPointsToStore:   String? = try? JSONUtil.shared.encodeJSONData(data: self.mAccessPoints)
                    
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_QRCODES, value: valueQRCodesToStore ?? "", secure: false)
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_ACCESSPOINTS, value: valueAccessPointsToStore ?? "", secure: false)
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_SYNC_DATE, value: (Int64(Date().timeIntervalSince1970 * 1000)).description, secure: false)
                    
                    LogManager.shared.info(message: "Updated QR Code list is ready to use, total: \(mQRCodes.count)")
                    LogManager.shared.info(message: "Updated Access Point list is ready to use, total: \(mAccessPoints.count)")

                    if (mQRCodes.count > 0) {
                        LogManager.shared.info(message: "Up to date qr code list will be used for passing flow")
                    } else {
                        LogManager.shared.warn(message: "There is no qr code that retrieved from server to continue passing flow", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                    }
                } catch {
                    LogManager.shared.error(message: "Storing received access and qr codes list failed!", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                }
                
                DelegateManager.shared.qrCodeListChanged(state: self.mQRCodes.count > 0 ? QRCodeListState.USING_SYNCED_DATA : QRCodeListState.EMPTY)
            }
        } else if case .failure(let error) = result {
            if (error.code == 409) {
                LogManager.shared.warn(message: "Sync error received for qr codes and access points, definition list will be retrieved again", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                getAccessPoints(clear: true)
            } else {
                LogManager.shared.error(message: "Getting definition list of qr codes and access points has failed", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                LogManager.shared.warn(message: mQRCodes.count > 0 ? "Stored qr code list will be used for passing flow" : "There is no qr code that stored before to continue passing flow", code: LogCodes.CONFIGURATION_SERVER_SYNC_LIST)
                
                DelegateManager.shared.qrCodeListChanged(state: self.mQRCodes.count > 0 ? QRCodeListState.USING_STORED_DATA : QRCodeListState.EMPTY)
            }
        }
    }
    
    private func fetchAccessPoints() -> Void {
        DataService().getAccessList(pagination: self.mPagination!, syncDate: self.mListSyncDate, completion: { (result) in
            self.processQRCodesResponse(result: result)
        })
    }
    
    private var time: Int64 = 0
    
    private func getAccessPoints(clear: Bool) -> Void {
        LogManager.shared.info(message: "Syncing definition list with server has been started")
        DelegateManager.shared.qrCodeListChanged(state: .SYNCING)
        
        mListClearFlag = clear
        
        if (clear) {
            mListSyncDate = nil
        } else {
            let storedSyncDate: String? = try? StorageManager.shared.getValue(key: StorageKeys.LIST_SYNC_DATE, secure: false)
            
            if (storedSyncDate != nil) {
                self.mListSyncDate = Int64(storedSyncDate!)
            }
        }
        
        self.mPagination = RequestPagination(t: 100, s: 0)
        
        self.mTempList = []
        self.mReceivedItemCount = 0
        
        self.fetchAccessPoints()
    }
    
}
