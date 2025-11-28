//
//  PassFlowManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

class PassFlowManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = PassFlowManager()
    private override init() {
        super.init()
        setupBluetoothStateListener()
    }
    
    // MARK: Setup
    
    /**
     * Setup Bluetooth state listener for iOS
     * This is iOS-specific: listens to CoreBluetooth state changes
     * Allows automatic retry when user enables Bluetooth after being prompted
     */
    private func setupBluetoothStateListener() {
        // Listen for Bluetooth state changes to retry when user enables BT
        BluetoothManager.shared.onBleStateChanged = { [weak self] capability in
            guard let self = self else { return }
            
            // If we were waiting for BLE to be enabled, and now it is enabled
            if self.isWaitingForBLEEnabled && capability.enabled {
                LogManager.shared.info(message: "Bluetooth enabled - retrying Bluetooth execution")
                self.isWaitingForBLEEnabled = false
                
                // Retry Bluetooth action
                self.executeBluetooth()
            }
        }
    }
    
    // MARK: Fields
    
    private var states: [PassFlowState] = []
    private var logStates: [PassFlowState] = []
    private var lastQRCodeId: String? = nil
    private var lastClubId: String? = nil
    
    // State machine
    private var currentState: SDKFlowState = .idle
    private var activeQRCodeContent: QRCodeContent? = nil
    private var actionList: [String] = []
    private var actionCurrent: String = ""
    private var bleTimeoutTimer: Timer? = nil
    private var locationTimeoutTimer: Timer? = nil
    private var bleScanSessionId: UUID? = nil
    private var isWaitingForBLEEnabled: Bool = false
    
    // All state codes are now user-facing - no ignore list needed
    
    // MARK: Public Functions
    
    func clearStates() {
        self.states.removeAll()
        self.logStates.removeAll()
        
        self.lastClubId = nil
        self.lastQRCodeId = nil
        self.currentState = .idle
        self.activeQRCodeContent = nil
        self.actionList.removeAll()
        self.actionCurrent = ""
        
        cancelBLETimeout()
        cancelLocationTimeout()
        bleScanSessionId = nil // Invalidate session
        isWaitingForBLEEnabled = false // Reset BLE waiting flag
    }
    
    func cancelFlow() {
        // Stop any active Bluetooth scan/connection
        BluetoothManager.shared.stopScan(disconnect: true)
        
        LogManager.shared.info(message: "Pass flow cancelled by app")
        
        // Capture current state info before clearing
        let isRemote: Bool
        switch currentState {
        case .executingRemoteAction, .awaitingLocationVerification:
            isRemote = true
        default:
            isRemote = false
        }
        
        let direction = activeQRCodeContent?.qrCode?.d
        let clubId = activeQRCodeContent?.clubInfo?.i
        let clubName = activeQRCodeContent?.clubInfo?.n
        
        // Clear all states and reset flow (also cancels timeouts)
        clearStates()
        
        // Notify delegate that flow was cancelled (with preserved info)
        DelegateManager.shared.onCompleted(
            resultCode: PassFlowResultCode.CANCEL.rawValue,
            isRemoteAccess: isRemote,
            direction: direction,
            clubId: clubId,
            clubName: clubName
        )
    }
    
    private func cancelBLETimeout() {
        bleTimeoutTimer?.invalidate()
        bleTimeoutTimer = nil
    }
    
    private func cancelLocationTimeout() {
        locationTimeoutTimer?.invalidate()
        locationTimeoutTimer = nil
    }
    
    func getStates() -> [PassFlowState] {
        return self.states
    }

    func getLogStates() -> [PassFlowState] {
        return self.logStates
    }
    
    func getClubId() -> String? {
        return self.lastClubId
    }
    
    func getQRCodeId() -> String? {
        return self.lastQRCodeId
    }
    
    func getCurrentState() -> SDKFlowState {
        return self.currentState
    }
    
    func getActiveQRContent() -> QRCodeContent? {
        return self.activeQRCodeContent
    }
    
    func addToStates(state: PassFlowStateCode, data: String? = nil) {
        // Prevent duplicate SCAN_QRCODE_NO_MATCH states
        if (state == PassFlowStateCode.SCAN_QRCODE_NO_MATCH
            && self.states.contains(where: { $0.state == PassFlowStateCode.SCAN_QRCODE_NO_MATCH.rawValue })) {
            LogManager.shared.debug(message: "No match QR Code state has been added before")
        } else {
            self.states.append(PassFlowState(state: state, data: data))
        }
        
        self.logStates.append(PassFlowState(state: state, data: data, datetime: Date()))
        
        // Notify delegate of state change
        DelegateManager.shared.notifyStateChanged(state: state, data: data)
    }
    
    func setQRData(qrId: String?, clubId: String?) {
        self.lastQRCodeId = qrId
        self.lastClubId = clubId
    }
    
    // MARK: State Machine Functions
    
    func processQRCode(data: String) -> QRCodeProcessResult {
        LogManager.shared.info(message: "Processing QR code data")
        
        // Step 1: Check if QR code format is valid (basic validation)
        guard !data.isEmpty else {
            addToStates(state: .SCAN_QRCODE_INVALID_FORMAT, data: "Empty QR code")
            LogManager.shared.warn(message: "QR code data is empty", code: LogCodes.PASSFLOW_QRCODE_READER_INVALID_CONTENT)
            
            // Complete flow with failure
            DelegateManager.shared.onCompleted(resultCode: PassFlowResultCode.FAIL.rawValue, isRemoteAccess: false, direction: nil, clubId: nil, clubName: nil)
            
            return QRCodeProcessResult(isValid: false, errorType: .invalidFormat)
        }
        
        // Step 2: Validate QR code format with regex
        let pattern = "https://(app|sdk)\\.armongate\\.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2]{1})?$"
        let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive)
        
        guard let match = regex?.firstMatch(in: data, options: [], range: NSRange(location: 0, length: data.utf16.count)) else {
            addToStates(state: .SCAN_QRCODE_INVALID_FORMAT, data: data)
            LogManager.shared.warn(message: "QR code has invalid format: \(data)", code: LogCodes.PASSFLOW_QRCODE_READER_INVALID_FORMAT)
            
            // Complete flow with failure
            DelegateManager.shared.onCompleted(resultCode: PassFlowResultCode.FAIL.rawValue, isRemoteAccess: false, direction: nil, clubId: nil, clubName: nil)
            
            return QRCodeProcessResult(isValid: false, errorType: .invalidFormat)
        }
        
        // Step 3: Extract QR code content parts from URL
        var prefix: Substring = ""
        var uuid: Substring = ""
        var direction: Substring = ""
        
        if let prefixRange = Range(match.range(at: 2), in: data) {
            prefix = data[prefixRange]
        }
        
        if let uuidRange = Range(match.range(at: 3), in: data) {
            uuid = data[uuidRange]
        }
        
        if let directionRange = Range(match.range(at: 4), in: data) {
            direction = data[directionRange]
        }
        
        let qrCodeContent = "\(prefix)/\(uuid)\(direction)"
        LogManager.shared.debug(message: "Extracted QR code content: \(qrCodeContent)")
        
        // Step 4: Try to get QR code content from authorized list
        let foundContent = ConfigurationManager.shared.getQRCodeContent(qrCodeData: qrCodeContent)
        
        // Step 5: Check if QR code was found in list
        guard let content = foundContent else {
            // QR code has valid format but not found in authorized list
            addToStates(state: .SCAN_QRCODE_NO_MATCH, data: qrCodeContent)
            LogManager.shared.warn(message: "QR code not found in authorized list", code: LogCodes.PASSFLOW_QRCODE_READER_NO_MATCHING)
            
            // Complete flow with failure
            DelegateManager.shared.onCompleted(resultCode: PassFlowResultCode.FAIL.rawValue, isRemoteAccess: false, direction: nil, clubId: nil, clubName: nil)
            
            return QRCodeProcessResult(isValid: false, errorType: .notFound)
        }
        
        // Step 6: Validate QR code content structure
        guard content.valid else {
            // QR code found but has invalid/incomplete configuration
            LogManager.shared.warn(message: "QR code configuration is invalid or incomplete", code: LogCodes.PASSFLOW_EMPTY_QRCODE_CONTENT)
            
            // Complete flow with failure
            DelegateManager.shared.onCompleted(resultCode: PassFlowResultCode.FAIL.rawValue, isRemoteAccess: false, direction: content.qrCode?.d, clubId: content.clubInfo?.i, clubName: content.clubInfo?.n)
            
            return QRCodeProcessResult(isValid: false, errorType: .invalidFormat)
        }
        
        LogManager.shared.info(message: "QR code content validated successfully")
        addToStates(state: .SCAN_QRCODE_FOUND)
        
        // Store QR data
        setQRData(qrId: content.qrCode?.i, clubId: content.clubInfo?.i)
        
        // Store active content
        self.activeQRCodeContent = content
        
        // Build action list based on trigger type
        buildActionList(qrCodeContent: content)
        
        // Update state and start execution
        self.currentState = .qrProcessed(content)
        executeNextAction()
        
        return QRCodeProcessResult(isValid: true, errorType: nil)
    }
    
    func confirmLocationVerified() {
        guard case .awaitingLocationVerification = currentState else {
            LogManager.shared.warn(message: "confirmLocationVerified called but not awaiting location verification - ignoring")
            return
        }
        
        // Cancel timeout since location was verified in time
        cancelLocationTimeout()
        
        addToStates(state: .RUN_ACTION_LOCATION_VALIDATED)
        
        LogManager.shared.info(message: "Location verified by app, proceeding with remote access")
        
        // Location verified - now execute remote access
        currentState = .executingRemoteAction
        executeRemoteAccess()
    }
    
    func startPassFlow() {
        executeNextAction()
    }
    
    func executeNextAction() {
        LogManager.shared.debug(message: "Executing next action")
        
        if actionList.isEmpty {
            LogManager.shared.error(message: "No actions in queue")
            return
        }
        
        let nextAction = actionList.removeFirst()
        actionCurrent = nextAction
        
        LogManager.shared.info(message: "Current action: \(actionCurrent), Remaining: \(actionList)")
        
        processAction(action: actionCurrent)
    }
    
    func getNextAction() -> String? {
        return actionList.first
    }
    
    // MARK: Private Functions
    
    private func buildActionList(qrCodeContent: QRCodeContent) {
        actionList.removeAll()
        actionCurrent = ""
        
        guard let triggerType = qrCodeContent.qrCode?.t else {
            LogManager.shared.warn(message: "QR code has empty trigger type", code: LogCodes.PASSFLOW_PROCESS_QRCODE_TRIGGERTYPE)
            return
        }
        
        // Check if location verification is needed
        let needLocation = qrCodeContent.qrCode?.v == true
            && qrCodeContent.geoLocation?.la != nil
            && qrCodeContent.geoLocation?.lo != nil
            && qrCodeContent.geoLocation?.r != nil
        
        LogManager.shared.info(message: "QR code trigger type: \(triggerType), needs location: \(needLocation)")
        
        switch triggerType {
        case QRTriggerType.Bluetooth:
            actionList.append("bluetooth")
            break
            
        case QRTriggerType.BluetoothThenRemote:
            actionList.append("bluetooth")
            if needLocation {
                actionList.append("location")
            }
            actionList.append("remoteAccess")
            break
            
        case QRTriggerType.Remote, QRTriggerType.RemoteThenBluetooth:
            if needLocation {
                actionList.append("location")
            }
            actionList.append("remoteAccess")
            if triggerType == QRTriggerType.RemoteThenBluetooth {
                actionList.append("bluetooth")
            }
            break
        }
        
        LogManager.shared.info(message: "Action list built: \(actionList)")
    }
    
    private func processAction(action: String) {
        if action.isEmpty {
            return
        }
        
        switch action {
        case "bluetooth":
            addToStates(state: .PROCESS_ACTION_BLUETOOTH)
            currentState = .executingBluetoothAction
            executeBluetooth()
            break
            
        case "location":
            // Location handled as part of remoteAccess
            executeNextAction()
            break
            
        case "remoteAccess":
            addToStates(state: .PROCESS_ACTION_REMOTE_ACCESS)
            
            // Check if location verification is required for remote access
            if let content = activeQRCodeContent,
               content.qrCode?.v == true,
               let geoLoc = content.geoLocation,
               let lat = geoLoc.la,
               let lon = geoLoc.lo,
               let radius = geoLoc.r {
                // Location required - notify app and wait for confirmation
                let requirement = LocationRequirement(latitude: lat, longitude: lon, radius: radius)
                currentState = .awaitingLocationVerification(requirement)
                addToStates(state: .RUN_ACTION_LOCATION_WAITING)
                
                // Start location verification timeout (configurable, default 30 seconds)
                let timeout = TimeInterval(ConfigurationManager.shared.locationVerificationTimeout())
                LogManager.shared.info(message: "Waiting for location verification (timeout: \(Int(timeout)) seconds)")
                
                locationTimeoutTimer = Timer.scheduledTimer(withTimeInterval: timeout, repeats: false) { [weak self] _ in
                    guard let self = self else { return }
                    
                    // Check if still waiting for location
                    if case .awaitingLocationVerification = self.currentState {
                        LogManager.shared.warn(message: "Location verification timeout (\(Int(timeout))s) - user did not verify location")
                        
                        // Clear delegate to stop receiving BLE callbacks (scan may not be active)
                        BluetoothManager.shared.onConnectionStateChanged = nil
                        self.bleScanSessionId = nil
                        
                        // Complete flow as failed
                        DelegateManager.shared.onCompleted(
                            resultCode: PassFlowResultCode.FAIL_LOCATION_TIMEOUT.rawValue,
                            isRemoteAccess: true,
                            direction: self.activeQRCodeContent?.qrCode?.d,
                            clubId: self.activeQRCodeContent?.clubInfo?.i,
                            clubName: self.activeQRCodeContent?.clubInfo?.n
                        )
                    }
                }
                
                DelegateManager.shared.notifyLocationRequired(requirement: requirement)
            } else {
                // No location required - proceed directly with remote access
                currentState = .executingRemoteAction
                executeRemoteAccess()
            }
            break
            
        default:
            LogManager.shared.error(message: "Unknown action type: \(action)")
        }
    }
    
    private func executeBluetooth() {
        guard let content = activeQRCodeContent,
              let terminals = content.terminals,
              !terminals.isEmpty else {
            LogManager.shared.error(message: "Missing required data for Bluetooth execution")
            fallbackOrFail()
            return
        }
        
        guard let qrCode = content.qrCode else {
            LogManager.shared.error(message: "QR code data is missing")
            fallbackOrFail()
            return
        }
        
        // Validate QR code ID
        guard let qrCodeId = qrCode.i, !qrCodeId.isEmpty else {
            LogManager.shared.warn(message: "QR code ID is empty")
            fallbackOrFail()
            return
        }
        
        // Validate direction
        guard let direction = qrCode.d else {
            LogManager.shared.warn(message: "Direction is empty")
            fallbackOrFail()
            return
        }
        
        // Validate hardware ID
        guard let hardwareId = qrCode.h, !hardwareId.isEmpty else {
            LogManager.shared.warn(message: "Hardware ID is empty")
            fallbackOrFail()
            return
        }
        
        // Validate relay number
        guard let relayNumber = qrCode.r else {
            LogManager.shared.warn(message: "Relay number is empty")
            fallbackOrFail()
            return
        }
        
        // Check Bluetooth authorization and enabled state
        let bleState = BluetoothManager.shared.getCurrentState()
        
        // Check authorization first
        if bleState.needAuthorize {
            LogManager.shared.warn(message: "Bluetooth permission not granted")
            
            // Check configuration: should we continue without BLE, or wait for permission?
            let shouldContinue = ConfigurationManager.shared.continueWithoutBLE()
            let hasNoFallback = actionList.isEmpty
            
            if shouldContinue && !hasNoFallback {
                // Continue to next action (e.g., remote access) without waiting
                // No callback needed - user chose to continue without BLE
                LogManager.shared.info(message: "Bluetooth permission missing and continueWithoutBLE=true - skipping to next action silently")
                
                executeNextAction()
            } else {
                // Wait for user to grant permission - but flow cannot auto-resume
                // User must grant permission and scan QR code again
                addToStates(state: .RUN_ACTION_BLUETOOTH_OFF_WAITING)
                LogManager.shared.warn(message: "Bluetooth permission missing and continueWithoutBLE=false - completing flow, user must grant permission and retry")
                DelegateManager.shared.needPermission(type: .NEED_PERMISSION_BLUETOOTH)
                
                // Complete flow as failed - user needs to grant permission and scan QR again
                DelegateManager.shared.onCompleted(
                    resultCode: PassFlowResultCode.FAIL_PERMISSION.rawValue,
                    isRemoteAccess: false,
                    direction: content.qrCode?.d,
                    clubId: content.clubInfo?.i,
                    clubName: content.clubInfo?.n
                )
            }
            return
        }
        
        // Check if Bluetooth is powered on
        if !bleState.enabled {
            LogManager.shared.warn(message: "Bluetooth is not enabled")
            
            // Check configuration: should we continue without BLE, or wait?
            let shouldContinue = ConfigurationManager.shared.continueWithoutBLE()
            let hasNoFallback = actionList.isEmpty
            
            if shouldContinue && !hasNoFallback {
                // Continue to next action - skip Bluetooth
                // No callback needed - user chose to continue without BLE
                LogManager.shared.info(message: "Bluetooth disabled and continueWithoutBLE=true - skipping to next action silently")
                
                executeNextAction()
            } else {
                // Wait for user to enable Bluetooth
                addToStates(state: .RUN_ACTION_BLUETOOTH_OFF_WAITING)
                LogManager.shared.warn(message: "Waiting for Bluetooth to be enabled (continueWithoutBLE=\(shouldContinue), hasNoFallback=\(hasNoFallback))")
                isWaitingForBLEEnabled = true
                DelegateManager.shared.needPermission(type: .NEED_ENABLE_BLE)
                // App should show "Please enable Bluetooth" UI
                // When user enables BT, onBleStateChanged callback will be triggered
            }
            return
        }
        
        let memberId = ConfigurationManager.shared.getMemberId()
        let barcodeId = ConfigurationManager.shared.getBarcodeId()
        let language = ConfigurationManager.shared.getLanguage()
        
        let configuration = BLEScanConfiguration(
            devices: terminals,
            userId: memberId,
            userBarcode: barcodeId,
            qrCodeId: qrCodeId,
            direction: direction.rawValue,
            hardwareId: hardwareId,
            relayNumber: relayNumber,
            language: Language(rawValue: language) ?? .TR
        )
        
        // Create unique session ID for this scan to prevent race conditions
        let sessionId = UUID()
        self.bleScanSessionId = sessionId
        
        // Set up callback to handle Bluetooth results
        BluetoothManager.shared.onConnectionStateChanged = { [weak self] status in
            guard let self = self else { return }
            
            // Guard: Check if this callback is for the current scan session
            guard self.bleScanSessionId == sessionId else {
                LogManager.shared.warn(message: "Ignoring BLE event from old scan session (session mismatch)")
                return
            }
            
            switch status.state {
            case .connected:
                // Invalidate session FIRST to prevent duplicate terminal events
                self.bleScanSessionId = nil
                BluetoothManager.shared.onConnectionStateChanged = nil
                
                // Bluetooth success! Cancel timeout and stop scan
                self.cancelBLETimeout()
                BluetoothManager.shared.stopScan(disconnect: false) // Don't disconnect - already connected
                
                LogManager.shared.info(message: "Bluetooth connection successful")
                addToStates(state: .RUN_ACTION_BLUETOOTH_PASS_SUCCEED)
                
                DelegateManager.shared.onCompleted(
                    resultCode: PassFlowResultCode.SUCCESS.rawValue,
                    isRemoteAccess: false,
                    direction: content.qrCode?.d != nil ? content.qrCode!.d! : nil,
                    clubId: content.clubInfo?.i,
                    clubName: content.clubInfo?.n
                )
                
            case .failed, .notFound:
                // Invalidate session FIRST to prevent duplicate terminal events
                self.bleScanSessionId = nil
                BluetoothManager.shared.onConnectionStateChanged = nil
                
                // Bluetooth failed - cancel timeout and stop scan immediately
                self.cancelBLETimeout()
                BluetoothManager.shared.stopScan(disconnect: true)
                
                LogManager.shared.warn(message: "Bluetooth connection failed: \(status.failMessage ?? "Unknown error")")
                self.addToStates(state: .RUN_ACTION_BLUETOOTH_CONNECTION_FAILED, data: status.failMessage)
                
                // Try next action or fail
                self.fallbackOrFail()
                
            case .connecting:
                LogManager.shared.debug(message: "Bluetooth connecting...")
                addToStates(state: .RUN_ACTION_BLUETOOTH_CONNECTING)
                
            case .disconnected:
                LogManager.shared.debug(message: "Bluetooth disconnected")
            }
        }
        
        // Start scanning timeout timer
        let timeout = ConfigurationManager.shared.bleConnectionTimeout()
        LogManager.shared.info(message: "Starting Bluetooth scan (timeout: \(timeout) seconds)")
        
        bleTimeoutTimer = Timer.scheduledTimer(withTimeInterval: TimeInterval(timeout), repeats: false) { [weak self] _ in
            guard let self = self else { return }
            
            // Guard: Check if timeout is for current scan session
            guard self.bleScanSessionId == sessionId else {
                LogManager.shared.warn(message: "Ignoring BLE timeout from old scan session")
                return
            }
            
            // Timeout reached - no device found
            LogManager.shared.warn(message: "Bluetooth scan timeout (\(timeout)s) - no device found")
            self.addToStates(state: .RUN_ACTION_BLUETOOTH_TIMEOUT)
            
            // Stop scanning
            BluetoothManager.shared.stopScan(disconnect: true)
            
            // Try next action or fail
            self.fallbackOrFail()
        }
        
        BluetoothManager.shared.startScan(configuration: configuration)
    }
    
    // MARK: Helper Methods
    
    /**
     * Try fallback action or complete with failure
     * Matches Android implementation but adapted for iOS
     */
    private func fallbackOrFail() {
        if !actionList.isEmpty {
            LogManager.shared.info(message: "Trying next action")
            executeNextAction()
        } else {
            LogManager.shared.info(message: "No fallback action available")
            
            // Clear BLE delegate to stop receiving callbacks
            BluetoothManager.shared.onConnectionStateChanged = nil
            bleScanSessionId = nil
            
            let isRemote = actionCurrent == "remoteAccess"
            
            DelegateManager.shared.onCompleted(
                resultCode: PassFlowResultCode.FAIL.rawValue,
                isRemoteAccess: isRemote,
                direction: activeQRCodeContent?.qrCode?.d,
                clubId: activeQRCodeContent?.clubInfo?.i,
                clubName: activeQRCodeContent?.clubInfo?.n
            )
        }
    }
    
    private func executeRemoteAccess() {
        guard let content = activeQRCodeContent,
              let qrCodeData = content.qrCode?.q else {
            LogManager.shared.error(message: "Missing required data for remote access")
            // Critical error: Complete flow as failed
            DelegateManager.shared.onCompleted(
                resultCode: PassFlowResultCode.FAIL.rawValue,
                isRemoteAccess: true,
                direction: activeQRCodeContent?.qrCode?.d,
                clubId: activeQRCodeContent?.clubInfo?.i,
                clubName: activeQRCodeContent?.clubInfo?.n
            )
            return
        }
        
        let request = RequestAccess(q: qrCodeData)
        
        LogManager.shared.info(message: "Executing remote access")
        AccessPointService().remoteOpen(request: request) { result in
            switch result {
            case .success(_):
                LogManager.shared.info(message: "Remote access successful")
                self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED)
                
                // Clear delegate to stop receiving BLE callbacks (scan may not be active)
                BluetoothManager.shared.onConnectionStateChanged = nil
                self.bleScanSessionId = nil
                
                DelegateManager.shared.onCompleted(
                    resultCode: PassFlowResultCode.SUCCESS.rawValue,
                    isRemoteAccess: true,
                    direction: content.qrCode?.d != nil ? content.qrCode!.d! : nil,
                    clubId: content.clubInfo?.i,
                    clubName: content.clubInfo?.n
                )
                
            case .failure(let error):
                LogManager.shared.error(message: "Remote access failed: \(error.localizedDescription)")
                
                // Add specific error state based on status code (matching Android implementation)
                let statusCode = error.code
                if statusCode == 401 {
                    self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED, data: error.message)
                } else if statusCode == 404 {
                    self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED, data: error.message)
                } else if statusCode == 408 {
                    self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT, data: error.message)
                } else if statusCode == 0 || statusCode == -1000 {
                    self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_NO_NETWORK, data: error.message)
                } else {
                    self.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED, data: error.message)
                }
                
                // Important: Don't fallback to Bluetooth if unauthorized (401)
                // User is not authorized - Bluetooth won't help either!
                let shouldTryFallback = statusCode != 401 && !self.actionList.isEmpty
                
                if shouldTryFallback {
                    LogManager.shared.info(message: "Remote access failed (code: \(statusCode)), trying next action (Bluetooth fallback)")
                    self.executeNextAction()
                } else {
                    if statusCode == 401 {
                        LogManager.shared.info(message: "Remote access failed: Unauthorized (401) - no fallback")
                    } else {
                        LogManager.shared.info(message: "Remote access failed - no fallback action available")
                    }
                    
                    DelegateManager.shared.onCompleted(
                        resultCode: PassFlowResultCode.FAIL.rawValue,
                        isRemoteAccess: true,
                        direction: content.qrCode?.d != nil ? content.qrCode!.d! : nil,
                        clubId: content.clubInfo?.i,
                        clubName: content.clubInfo?.n
                    )
                }
            }
        }
    }
    
}
