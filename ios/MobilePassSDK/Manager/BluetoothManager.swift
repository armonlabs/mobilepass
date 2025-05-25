//
//  BluetoothManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 8.02.2021.
//

import Foundation
import CoreBluetooth

class BluetoothManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = BluetoothManager()
    private override init() {
        super.init()
    }
    
    // MARK: Fields
    
    public var onScanningStarted:           (() -> ())?
    public var onConnectionStateChanged:    ((DeviceConnectionStatus) -> ())?
    public var onBleStateChanged:           ((DeviceCapability) -> ())?
    
    private var bluetoothState:             DeviceCapability?
    
    private var filterServiceUUIDs:         [CBUUID] = []
    private var filterCharacteristicUUIDs:  [CBUUID] = []
    
    private var currentDevicesInRange:      Dictionary<String, DeviceInRange> = [:]
    private var currentConnectedDevices:    Dictionary<String, DeviceConnection> = [:]
    private var currentConfiguration:       BLEScanConfiguration?
    private var currentCentralManager:      CBCentralManager!
    
    private var isConnectionActive:         Bool = false
    private var lastConnectionTime:         Dictionary<String, TimeInterval> = [:]
    
    
    // MARK: Public Functions
    
    public func setReady() {
        if(self.currentCentralManager == nil) {
            self.readyCentralManager()
        }
    }
    
    public func getCurrentState() -> DeviceCapability {
        if (self.bluetoothState == nil) {
            self.bluetoothState = DeviceCapability(support: false, enabled: false, needAuthorize: false)
        }
        
        return self.bluetoothState!
    }
    
    public func startScan(configuration: BLEScanConfiguration) {
        LogManager.shared.info(message: "Bluetooth scanner is starting...")
        
        // Check current central manager instance
        setReady()
        
        // Ready for new scanning
        clearFieldsForNewScan(configuration: configuration)
        
        // Prepare service and characteristic UUID lists for new scan
        prepareUUIDLists()
        
        LogManager.shared.info(message: "Bluetooth scanner is ready for scanning")
        
        // Check active scanning status
        if(self.currentCentralManager.isScanning) {
            LogManager.shared.info(message: "Bluetooth scanner is already scanning, new starter is ignored!")
            self.onScanningStarted?()
            return
        }
        
        if #available(iOS 13.0, *) {
            let authorizationStatus: CBManagerAuthorization = self.currentCentralManager.authorization
        
            switch authorizationStatus {
            case .allowedAlways:
                LogManager.shared.info(message: "Bluetooth Authorization Status: Allowed Always")
                break
            case .denied:
                LogManager.shared.info(message: "Bluetooth Authorization Status: Denied")
                break
            case .restricted:
                LogManager.shared.info(message: "Bluetooth Authorization Status: Restricted")
                break
            case .notDetermined:
                LogManager.shared.info(message: "Bluetooth Authorization Status: Not Determined")
                break
            @unknown default:
                LogManager.shared.info(message: "Bluetooth Authorization Status: Unknown Default")
            }
            
            if (authorizationStatus == .allowedAlways) {
                if (self.currentCentralManager.state == .poweredOn) {
                    self.onScanningStarted?()
                    LogManager.shared.info(message: "Scanning nearby BLE devices now")
                    self.currentCentralManager.scanForPeripherals(withServices: filterServiceUUIDs,
                                                                  options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
                } else {
                    LogManager.shared.info(message: "Bluetooth authorization status is allowed, but state is not powered on yet")
                }
            } else if (authorizationStatus != .notDetermined) {
                DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_BLUETOOTH, showMessage: true)
            }
        } else {
            // Fallback on earlier versions
        };
    }
    
    public func stopScan(disconnect: Bool) {
        LogManager.shared.info(message: "Bluetooth scanner is stopping...")
        
        if(self.currentCentralManager == nil) {
            LogManager.shared.info(message: "Bluetooth scanner has not been initialized yet, so stop request has been ignored")
            return
        }
        
        self.currentCentralManager.stopScan()
        LogManager.shared.info(message: "Bluetooth scanner has been stopped successfully")
        
        if (disconnect) {
            self.disconnect()
        }
    }
    
    public func connectToDevice(deviceIdentifier: String) {
        LogManager.shared.debug(message: "Connect to device is requested for identifier: \(deviceIdentifier)");
        
        if (self.currentDevicesInRange.index(forKey: deviceIdentifier) == nil) {
            LogManager.shared.warn(message: "Selected device not found in list to connect, device identifier: \(deviceIdentifier)")
            onConnectionStateChanged(identifier: deviceIdentifier, connectionState: .notFound)
            return
        }
        
        if (isConnectionActive) {
            LogManager.shared.warn(message: "Another Bluetooth connection is active now, so new one has been ignored", code: LogCodes.BLUETOOTH_CONNECTION_DUPLICATE)
            return
        }
        
        isConnectionActive = true
        
        onConnectionStateChanged(identifier: deviceIdentifier, connectionState: .connecting)
        
        LogManager.shared.info(message: "Connecting to device...")
        let deviceInRange: DeviceInRange = self.currentDevicesInRange[deviceIdentifier]!
        
        self.currentCentralManager.connect(deviceInRange.device, options: nil)
    }
    
    public func disconnectFromDevice() {
        self.disconnect()
    }
    
    // MARK: Private Functions
    
    private func readyCentralManager() {
        self.currentCentralManager = CBCentralManager(delegate: self, queue: nil)
        LogManager.shared.info(message: "Bluetooth central manager is created for connection manager")
    }
    
    private func clearFieldsForNewScan(configuration: BLEScanConfiguration) {
        // Set fields for new scanning
        self.currentConfiguration = configuration
        
        // Clear devices that found before
        self.currentDevicesInRange.removeAll()
        
        // Clear devices that stay in connected devices list
        self.currentConnectedDevices.removeAll()
        
        // Clear connection limit flags
        self.isConnectionActive = false
        
        LogManager.shared.debug(message: "Related fields are cleared for new scanning session");
    }
    
    private func prepareUUIDLists() {
        if self.currentConfiguration == nil {
            return
        }
        
        self.filterServiceUUIDs = []
        self.filterCharacteristicUUIDs = []
        
        for uuid in self.currentConfiguration!.deviceList.keys {
            LogManager.shared.debug(message: "Filter services with uuid: \(uuid)")
            
            self.filterServiceUUIDs.append(CBUUID(string: uuid))
            self.filterCharacteristicUUIDs.append(CBUUID(string: UUIDs.CHARACTERISTIC))
        }
    }
    
    private func disconnect() {
        LogManager.shared.info(message: "Disconnect from connected devices is requested");
        
        if(self.currentConnectedDevices.count == 0) {
            LogManager.shared.info(message: "There is no connected device now, disconnection flow is not required")
            return
        }
        
        for key in self.currentConnectedDevices.keys {
            let connectedDevice = self.currentConnectedDevices[key]!
            
            if (connectedDevice.peripheral.state != .connected) {
                LogManager.shared.info(message: "Peripheral exists in connected devices list, but not connected now")
                break
            }
            
            LogManager.shared.info(message: "Disconnecting from peripheral / \(key)")
            self.currentCentralManager.cancelPeripheralConnection(connectedDevice.peripheral)
        }
    }
    
    private func onConnectionStateChanged(identifier: String, connectionState: DeviceConnectionStatus.ConnectionState, failReason: Int? = nil, failMessage: String? = nil) {
        LogManager.shared.info(message: "Bluetooth connection state changed for \(identifier) > \(self.getDescriptionOfConnectionState(state: connectionState))");
        self.onConnectionStateChanged?(DeviceConnectionStatus(id: identifier, state: connectionState, failReason: failReason, failMessage: failMessage))
        
        if (connectionState == .failed) {
            LogManager.shared.warn(message: "Bluetooth connection to device has failed", code: LogCodes.BLUETOOTH_CONNECTION_FAILED);
            disconnect()
        } else if (connectionState == .disconnected) {
            self.lastConnectionTime[identifier] = Date().timeIntervalSince1970
            self.isConnectionActive = false
            
            if (self.currentConnectedDevices.index(forKey: identifier) != nil) {
                LogManager.shared.info(message: "Device (\(identifier)) is removed from connected devices' list");
                self.currentConnectedDevices.removeValue(forKey: identifier)
            }
            
            if (self.currentDevicesInRange.index(forKey: identifier) != nil) {
                LogManager.shared.info(message: "Device (\(identifier)) is removed from list of devices in range");
                self.currentDevicesInRange.removeValue(forKey: identifier)
            }
        }
    }
    
    private func getDescriptionOfConnectionState(state: DeviceConnectionStatus.ConnectionState) -> String {
        switch state {
        case .connected:
            return "Connected"
        case .connecting:
            return "Connecting"
        case .disconnected:
            return "Disconnected"
        case .failed:
            return "Failed"
        case .notFound:
            return "NotFound"
        }
    }
    
}

// MARK: CBCentralManagerDelegate
extension BluetoothManager: CBCentralManagerDelegate {
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        var state: String = "";
        
        switch central.state {
        case .unsupported:
            state = "This device does not support Bluetooth Low Energy.";
            self.bluetoothState = DeviceCapability(support: false, enabled: false, needAuthorize: false)
            break;
        case .unauthorized:
            state = "This app is not authorized to use Bluetooth Low Energy.";
            self.bluetoothState = DeviceCapability(support: true, enabled: false, needAuthorize: true)
            break;
        case .poweredOff:
            state = "Bluetooth on this device is currently powered off.";
            self.bluetoothState = DeviceCapability(support: true, enabled: false, needAuthorize: false)
            break;
        case .resetting:
            state = "The BLE Manager is resetting; a state update is pending.";
            self.bluetoothState = DeviceCapability(support: true, enabled: false, needAuthorize: false)
            break;
        case .poweredOn:
            state = "Bluetooth LE is turned on and ready for communication.";
            self.bluetoothState = DeviceCapability(support: true, enabled: true, needAuthorize: false)
            break;
        case .unknown:
            state = "The state of the BLE Manager is unknown.";
            self.bluetoothState = DeviceCapability(support: false, enabled: false, needAuthorize: false)
            break;
        default:
            state = "The state of the BLE Manager is unknown.";
            self.bluetoothState = DeviceCapability(support: false, enabled: false, needAuthorize: false)
            break;
        }
        
        LogManager.shared.info(message: state)
        self.onBleStateChanged?(self.bluetoothState!)
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if (self.isConnectionActive) {
            return
        }
        
        let armonDeviceName = advertisementData[CBAdvertisementDataLocalNameKey] as? String ?? "EMPTY"
        LogManager.shared.debug(message: "Discovered peripheral (\(armonDeviceName)) at \(RSSI.stringValue)")
        
        if (self.currentConfiguration == nil) {
            LogManager.shared.debug(message: "Connection configuration is not defined yet!")
        } else {
            if(RSSI.intValue < 0 && RSSI.intValue > -95) {
                // Device in range for specified RSSI limit
                
                // Check device is added to list before
                if (self.currentDevicesInRange.index(forKey: peripheral.identifier.uuidString) == nil) {
                    if (self.lastConnectionTime.index(forKey: peripheral.identifier.uuidString) != nil) {
                        let lastConnection = self.lastConnectionTime[peripheral.identifier.uuidString]
                        if (lastConnection != nil) {
                            let elapsedTime = Date().timeIntervalSince1970 - lastConnection!
                            
                            if (elapsedTime < 5) {
                                LogManager.shared.debug(message: "Device is ignored because of reconnection timeout, elapsed time: \(elapsedTime.description)")
                                return
                            }
                        }
                    }
                    
                    self.currentDevicesInRange[peripheral.identifier.uuidString] = DeviceInRange(serviceUUID: "", bluetoothDevice: peripheral)
                    LogManager.shared.debug(message: "Peripheral is added to device list, current device count: \(self.currentDevicesInRange.count)")
                    
                    connectToDevice(deviceIdentifier: peripheral.identifier.uuidString)
                }
            } else if (RSSI.intValue != 127) {
                if (self.currentDevicesInRange.index(forKey: peripheral.identifier.uuidString) != nil) {
                    self.currentDevicesInRange.removeValue(forKey: peripheral.identifier.uuidString)
                    LogManager.shared.debug(message: "Peripheral is removed from device list, current device count: \(self.currentDevicesInRange.count)")
                }
            }
        }
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        LogManager.shared.info(message: "Connected to \(peripheral.getName())")
        
        // Set event listener
        peripheral.delegate = self;
        
        // Discover related services
        LogManager.shared.debug(message: "Looking for predefined services of peripheral...")
        peripheral.discoverServices(self.filterServiceUUIDs)
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        if (error != nil){
            LogManager.shared.warn(message: "Disconnected from \(peripheral.getName()) with error: \(error!.localizedDescription)", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
        } else {
            LogManager.shared.info(message: "Disconnected from \(peripheral.getName())")
        }
        
        onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .disconnected)
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Failed to connect, error: \(error!.localizedDescription)", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
        } else {
            LogManager.shared.error(message: "Failed to connect", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
        }
        
        onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
    }
    
}

// MARK: CBPeripheralDelegate
extension BluetoothManager: CBPeripheralDelegate {
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Error on discovering peripheral services: \(error!.localizedDescription)", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        } else {
            LogManager.shared.debug(message: "Discovering peripheral services completed")
        }
        
        if(peripheral.services == nil || peripheral.services!.isEmpty) {
            LogManager.shared.error(message: "List of peripheral services is empty for \(peripheral.getName())", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return;
        }
        
        for service: CBService in peripheral.services! {
            LogManager.shared.debug(message: "Service found for peripheral (\(peripheral.getName())), UUID: \(service.uuid.uuidString)")
            LogManager.shared.debug(message: "Discover characteristics for service")
            peripheral.discoverCharacteristics(self.filterCharacteristicUUIDs, for: service)
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Error on discovering service characteristics: \(error!.localizedDescription)", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        } else {
            LogManager.shared.debug(message: "Discovering service characteristics completed for service UUID: \(service.uuid.uuidString)")
        }
        
        if (service.characteristics == nil || service.characteristics!.isEmpty) {
            LogManager.shared.error(message: "Characteristics list is empty, service UUID: \(service.uuid.uuidString)", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        }
        
        self.currentConnectedDevices[peripheral.identifier.uuidString] = DeviceConnection(peripheral: peripheral, serviceUUID: service.uuid.uuidString)
        
        for characteristic: CBCharacteristic in service.characteristics! {
            LogManager.shared.debug(message: "Characteristic found for peripheral \(peripheral.getName()), UUID: \(characteristic.uuid.uuidString)")
            LogManager.shared.debug(message: "Store characteristic with UUID: \(characteristic.uuid.uuidString)")
            
            self.currentConnectedDevices[peripheral.identifier.uuidString]!.characteristics[characteristic.uuid.uuidString] = characteristic
        }
        
        if (self.currentConfiguration != nil) {
            LogManager.shared.debug(message: "Auto subscribe for modes other than passing with close phone")
            if #available(iOS 10.0, *) {
                changeCommunicationState(identifier: peripheral.identifier.uuidString, start: true, rssiValue: "0")
            } else {
                LogManager.shared.error(message: "iOS version is not valid to change communication state", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
                onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Sending value to characteristic failed, error: \(error!.localizedDescription)")
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
        } else {
            LogManager.shared.debug(message: "Sending value to characteristic completed")
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Receiving value from characteristic failed, error: \(error!.localizedDescription)")
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        } else {
            LogManager.shared.debug(message: "New value received from characteristic")
        }
        
        if (characteristic.value == nil || characteristic.value!.isEmpty) {
            LogManager.shared.warn(message: "Value that received from characteristic is empty, disconnect now!")
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        }
        
        if (self.currentConnectedDevices.index(forKey: peripheral.identifier.uuidString) == nil) {
            LogManager.shared.warn(message: "Value received but peripheral is not in connected devices' list, ignore data!")
            return
        }
        
        
        let receivedData: Data = characteristic.value!
        
        LogManager.shared.debug(message: "Received Data > \(receivedData.hexEncodedString(options: .upperCase))");
        
        let result: BLEDataContent? = DataParserUtil.shared.parse(data: receivedData)
        
        if #available(iOS 10.0, *) {
            if (result != nil) {
                if(result!.type == DataTypes.TYPE.AuthChallengeForPublicKey) {
                    
                    let deviceId = result!.data!["deviceId"] as? String ?? ""
                    LogManager.shared.debug(message: "Auth challenge received, device id: \(deviceId)");
                    
                    do {
                        let connectedDevice = self.currentConnectedDevices[peripheral.identifier.uuidString]!
                        let deviceConnectionInfo = self.currentConfiguration != nil ? self.currentConfiguration!.deviceList[connectedDevice.serviceUUID.lowercased()] : nil
                        
                        if (deviceConnectionInfo != nil) {
                            let resultData: Data? = try generateChallengeResponse(challengeType:   result!.type,
                                                                                  iv:              (result!.data!["iv"] as? Data)!,
                                                                                  deviceInfo:      deviceConnectionInfo!)
                            
                            peripheral.writeValue(resultData!, for: characteristic, type: .withResponse)
                        } else {
                            LogManager.shared.error(message: "Required device info cannot be found to generate challenge response!")
                            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
                        }
                    } catch {
                        LogManager.shared.error(message: "Generate challenge response failed!")
                        onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
                    }
                } else if (result!.type == DataTypes.TYPE.AuthChallengeResult) {
                    processChallengeResult(deviceIdentifier: peripheral.identifier.uuidString, result: result!)
                } else {
                    LogManager.shared.error(message: "Unknown data type received from device, disconnect now!")
                    onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
                }
            } else {
                LogManager.shared.error(message: "Unknown data received from device, disconnect now!")
                onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            }
        } else {
            LogManager.shared.error(message: "Process flow only available for iOS 10.0 and newer, disconnect now!")
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        if (error != nil) {
            LogManager.shared.error(message: "Error on changing notification state for characteristic (\(characteristic.uuid.uuidString)) > \(error!.localizedDescription)")
            onConnectionStateChanged(identifier: peripheral.identifier.uuidString, connectionState: .failed)
            return
        }
        
        if (characteristic.isNotifying) {
            LogManager.shared.debug(message: "Subscribed to characteristic: \(characteristic.uuid.uuidString)")
        } else {
            LogManager.shared.debug(message: "Unsubscribed from characteristic: \(characteristic.uuid.uuidString)")
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        LogManager.shared.warn(message: "Peripheral services are changed!", code: LogCodes.BLUETOOTH_CONNECTION_FLOW)
    }
}

@available(iOS 10.0, *)
extension BluetoothManager {
    private func changeCommunicationState(identifier: String, start: Bool, rssiValue: String) {
        if (self.currentConnectedDevices.index(forKey: identifier) != nil) {
            let communicationState: String = start ? "Starting" : "Ending"
            let subscriptionState:  String = start ? "Subscribe" : "Unsubscribe"
            
            LogManager.shared.debug(message: "\(communicationState) communication with \(identifier) at RSSI: \(rssiValue)")
            
            let connectedDevice = self.currentConnectedDevices[identifier]!
            
            for key in connectedDevice.characteristics.keys {
                LogManager.shared.debug(message: "\(subscriptionState) for characteristic: \(key)")
                connectedDevice.peripheral.setNotifyValue(start, for: connectedDevice.characteristics[key]!)
            }
        }
    }
    
    
    private func processChallengeResult(deviceIdentifier: String, result: BLEDataContent) {
        if (result.result == DataTypes.RESULT.Succeed) {
            self.onConnectionStateChanged?(DeviceConnectionStatus(id: deviceIdentifier, state: DeviceConnectionStatus.ConnectionState.connected))
            
            LogManager.shared.info(message: "Disconnect from device after successful process of passing!")
            self.disconnect()
        } else {
            onConnectionStateChanged(identifier: deviceIdentifier, connectionState: .failed, failReason: result.data!["reason"] as? Int, failMessage: result.data!["message"] as? String)
        }
    }
    
    private func generateChallengeResponse(challengeType: Int, iv: Data, deviceInfo: DeviceConnectionInfo) throws -> Data {
        if (currentConfiguration!.dataUserBarcode.isEmpty) {
            return try self.generateDirectionChallengeResponse(challengeType: challengeType, iv: iv, deviceInfo: deviceInfo)
        } else {
            return try self.generateMacfitChallengeResponse(challengeType: challengeType, iv: iv, deviceInfo: deviceInfo)
        }
    }
    
    private func generateDirectionChallengeResponse(challengeType: Int, iv: Data, deviceInfo: DeviceConnectionInfo) throws -> Data {
        // MARK: Create header data
        let resultDataArray: [UInt8] = [
            UInt8(PacketHeaders.PROTOCOLV2.GROUP.AUTH),
            UInt8(PacketHeaders.PROTOCOLV2.AUTH.DIRECTION_CHALLENGE),
            UInt8(PacketHeaders.PLATFORM_IOS)
        ]
        
        var resultData: Data = Data(resultDataArray)
        
        // MARK: Ready encrypted response
        var dataIV: Data = iv
        
        let sharedKey: Data? = CryptoManager.shared.getSecret(privateKey: ConfigurationManager.shared.getPrivateKey(), publicKey: deviceInfo.publicKey)
        dataIV.append(sharedKey!)
        
        let encryptedResponse: Data = CryptoManager.shared.encodeWithSHA256(plainData: dataIV)
        
        // MARK: Ready result data after encryption
        resultData.append(currentConfiguration!.dataUserId.data(using: .utf8)!.fill(length: 16, repeating: 0x00))
        resultData.append(currentConfiguration!.hardwareId.data(using: .utf8)!.fill(length: 16, repeating: 0x00))
        resultData.append(currentConfiguration!.deviceNumber.mergeToData(currentConfiguration!.dataDirection, currentConfiguration!.relayNumber)!)
        
        resultData.append(encryptedResponse)
        
        return resultData
    }
    
    private func generateMacfitChallengeResponse(challengeType: Int, iv: Data, deviceInfo: DeviceConnectionInfo) throws -> Data {
        // MARK: Create header data
        let resultDataArray: [UInt8] = [
            UInt8(PacketHeaders.PROTOCOLV2.GROUP.AUTH),
            UInt8(PacketHeaders.PROTOCOLV2.AUTH.MACFIT_CHALLENGE),
            UInt8(PacketHeaders.PLATFORM_IOS)
        ]
        
        var resultData: Data = Data(resultDataArray)
        
        // MARK: Ready encrypted response
        var dataIV: Data = iv
        
        let sharedKey: Data? = CryptoManager.shared.getSecret(privateKey: ConfigurationManager.shared.getPrivateKey(), publicKey: deviceInfo.publicKey)
        dataIV.append(sharedKey!)
        
        let encryptedResponse: Data = CryptoManager.shared.encodeWithSHA256(plainData: dataIV)
        
        // MARK: Ready result data after encryption
        resultData.append(currentConfiguration!.dataUserId.data(using: .utf8)!.fill(length: 16, repeating: 0x00))
        resultData.append(currentConfiguration!.dataUserBarcode.data(using: .utf8)!.fill(length: 16, repeating: 0x00))
        resultData.append(currentConfiguration!.qrCodeId.replacingOccurrences(of: "-", with: "").data(using: .hexadecimal)!)
        resultData.append(currentConfiguration!.language == Language.EN ? Data([0x01]) : Data([0x00]))
        
        resultData.append(encryptedResponse)
        
        return resultData
    }
}



extension CBPeripheral {
    func getName() -> String {
        return self.name ?? "UNKNOWN"
    }
}
