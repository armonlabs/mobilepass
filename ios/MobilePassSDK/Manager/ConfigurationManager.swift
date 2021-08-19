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
        return mCurrentConfig?.logLevel ?? LogLevel.Info.rawValue
    }
    
    public func getConfigurationLog() -> String {
        return mCurrentConfig?.getLog() ?? ""
    }
    
    public func refreshList() -> Void {
        if (DelegateManager.shared.isQRCodeListRefreshable()) {
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
    
    private func checkKeyPair() throws -> Bool {
        mCurrentKeyPair = nil
        
        var newlyCreated: Bool = false
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
            newlyCreated = true
            
            mCurrentKeyPair = CryptoManager.shared.generateKeyPair()
            
            mUserKeyDetails.append(StorageDataUserDetails(userId: getMemberId(), publicKey: mCurrentKeyPair!.publicKey, privateKey: mCurrentKeyPair!.privateKey))
            let jsonString = try JSONUtil.shared.encodeJSONArray(data: mUserKeyDetails)
            
            _ = try StorageManager.shared.setValue(key: StorageKeys.USER_DETAILS, value: jsonString, secure: true)
        }
        
        return newlyCreated
    }
    
    private func sendUserData() throws -> Void {
        var needUpdate: Bool = try checkKeyPair();
        
        let storedMemberId: String? = try? StorageManager.shared.getValue(key: StorageKeys.MEMBERID, secure: false)
        if (storedMemberId == nil || storedMemberId!.count == 0 || storedMemberId! != getMemberId()) {
            needUpdate = true
        }
        
        if (needUpdate) {
            DataService().sendUserInfo(request: RequestSetUserData(publicKey: mCurrentKeyPair!.publicKey, clubMemberId: getMemberId()), completion: { (result) in
                if case .success(_) = result {
                    do {
                        _ = try StorageManager.shared.setValue(key: StorageKeys.MEMBERID, value: self.getMemberId(), secure: false)
                        LogManager.shared.info(message: "Member id is stored successfully")
                    } catch {
                        LogManager.shared.error(message: "Error occurred while storing member id")
                    }
                    self.getAccessPoints(clear: false)
                } else {
                    LogManager.shared.error(message: "Send user info to server failed!")
                    self.getAccessPoints(clear: false)
                }
            })
        } else {
            LogManager.shared.info(message: "User info is already sent to server")
            self.getAccessPoints(clear: false)
        }
    }
    
    private func processQRCodesResponse(result: Result<ResponseAccessPointList?, RequestError>) {
        if case .success(let receivedData) = result {
            if (receivedData == nil) {
                LogManager.shared.error(message: "Empty data received for access points list")
                return
            }
            
            self.mReceivedItemCount += receivedData!.i.count
            
            for item in (receivedData?.i ?? []) {
                self.mTempList.append(item)
                
                // Open to show qr codes list
                do {
                    LogManager.shared.debug(message: "\(try JSONUtil.shared.encodeJSONData(data: item))")
                } catch { }
            }
            
            
            
            if (receivedData!.p.c > self.mReceivedItemCount) {
                self.mPagination?.s = self.mReceivedItemCount
                self.fetchAccessPoints()
            } else {
                if (mListClearFlag) {
                    self.mQRCodes       = [:]
                    self.mAccessPoints  = [:]
                }
                
                for var item in self.mTempList {
                    let storedAccessPoint = mAccessPoints.index(forKey: item.i) != nil ? mAccessPoints[item.i] : nil
                    
                    var storedQRCodeData = storedAccessPoint != nil ? storedAccessPoint!.d ?? [] : []
                    var newQRCodeData: [String] = []
                    
                    for qrCode in item.q {
                        if let index = storedQRCodeData.firstIndex(of: qrCode.q) {
                            storedQRCodeData.remove(at: index)
                        }
                        
                        newQRCodeData.append(qrCode.q)
                        self.mQRCodes[qrCode.q] = QRCodeMatch(accessPointId: item.i, qrCode: qrCode)
                    }
                    
                    for qrCodeData in storedQRCodeData {
                        mQRCodes[qrCodeData] = nil
                    }
                    
                    item.q = []
                    item.d = newQRCodeData
                    
                    self.mAccessPoints[item.i] = item
                }
                
                do {
                    let valueQRCodesToStore:        String? = try? JSONUtil.shared.encodeJSONData(data: self.mQRCodes)
                    let valueAccessPointsToStore:   String? = try? JSONUtil.shared.encodeJSONData(data: self.mAccessPoints)
                    
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_QRCODES, value: valueQRCodesToStore ?? "", secure: false)
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_ACCESSPOINTS, value: valueAccessPointsToStore ?? "", secure: false)
                    _ = try StorageManager.shared.setValue(key: StorageKeys.LIST_SYNC_DATE, value: (Int64(Date().timeIntervalSince1970 * 1000)).description, secure: false)
                } catch {
                    LogManager.shared.error(message: "Store received access list failed!")
                }
                
                DelegateManager.shared.qrCodeListChanged(state: self.mQRCodes.count > 0 ? QRCodeListState.USING_SYNCED_DATA : QRCodeListState.EMPTY)
            }
        } else if case .failure(let error) = result {
            if (error.code == 409) {
                LogManager.shared.error(message: "Sync error received, get list again");
                getAccessPoints(clear: true)
            } else {
                LogManager.shared.error(message: "Get access list failed!")
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
