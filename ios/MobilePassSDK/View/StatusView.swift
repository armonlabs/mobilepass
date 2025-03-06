//
//  TestView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 10.02.2021.
//

import Foundation
import SwiftUI

public enum ActionState {
    case scanning, connecting, succeed, failed
}

public struct ActionConfig: Codable {
    var currentAction:  String
    var devices:        [ResponseAccessPointListTerminal]
    var clubInfo:       ResponseAccessPointListClubInfo?
    var qrCode:         ResponseAccessPointListQRCode?
    var nextAction:     String?
}

@available(iOS 13.0, *)
class CurrentStatusModel: ObservableObject {
    @Published var icon:        String  = "waiting"
    @Published var message:     String  = "text_status_message_waiting"
    @Published var suggestion:  Bool    = false
    
    func update(message: String, icon: String) {
        self.message    = message
        self.icon       = icon
        self.suggestion = false
    }
    
    func update(message: String, icon: String, suggest: Bool) {
        self.message    = message
        self.icon       = icon
        self.suggestion = suggest
    }
}

@available(iOS 13.0, *)
class StateModel: ObservableObject {
    @Published var timerConnection:     Timer?
    @Published var lastConnectionState: DeviceConnectionStatus.ConnectionState?
    @Published var lastBluetoothState:  Bool = BluetoothManager.shared.getCurrentState().enabled
    
    func setTimerConnection(timer: Timer?) {
        self.timerConnection = timer
    }
    
    func setConnectionState(state: DeviceConnectionStatus.ConnectionState) {
        self.lastConnectionState = state
    }
    
    func setBluetoothState(state: Bool) {
        self.lastBluetoothState = state
    }
}

@available(iOS 13.0, *)
struct StatusView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.locale) var locale
    
    private var currentConfig: ActionConfig?
    
    @ObservedObject var viewModel: CurrentStatusModel
    @ObservedObject var stateModel: StateModel
    
    init(config: ActionConfig?) {
        self.viewModel = CurrentStatusModel()
        self.stateModel = StateModel()
        
        if (config != nil) {
            self.currentConfig = config
            self.startAction()
        }
    }
    
    var body: some View {
        GeometryReader { (geometry) in
            VStack(alignment: .center) {
                VStack {
                    Image(self.viewModel.icon, bundle: Bundle(for: PassFlowController.self)).resizable().frame(width: geometry.size.width * 0.5, height: geometry.size.width * 0.5, alignment: .center)
                }.frame(width: geometry.size.width * 0.9, height: geometry.size.height * 0.5, alignment: .bottom)
                VStack {
                    Text(self.viewModel.message.localized(locale.identifier)).fontWeight(.medium).multilineTextAlignment(.center)
                    
                    if (self.viewModel.suggestion) {
                        VStack(alignment: .center) {
                            // VStack(alignment: .center) {
                                // Image(systemName: "exclamationmark.triangle").foregroundColor(.orange)
                                Text("text_status_message_suggestion".localized(locale.identifier)).fontWeight(.bold).foregroundColor(.orange).multilineTextAlignment(.center).padding(.top, 4)
                                
                            if (BluetoothManager.shared.getCurrentState().needAuthorize) {
                                Text("text_status_message_suggestion_authorize".localized(locale.identifier)).fontWeight(.medium).foregroundColor(.orange).multilineTextAlignment(.center).padding(.top, 2)
                                } else {
                                    Text("text_status_message_suggestion_enable".localized(locale.identifier)).fontWeight(.medium).foregroundColor(.orange).multilineTextAlignment(.center).padding(.top, 2)
                                    
                                }
                            // }
                            if (BluetoothManager.shared.getCurrentState().needAuthorize) {
                                Button(action: {
                                    DelegateManager.shared.goToSettings()
                                    UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
                                }) {
                                    Text("text_button_app_permissions".localized(locale.identifier)).bold()
                                }.padding(.top, 16)
                            }
                        }.padding(.top, 12)
                    }
                }.frame(width: geometry.size.width * 0.9, height: geometry.size.height * 0.3, alignment: .top).padding(.top, 48)
            }.padding(.horizontal, 14)
                .frame(width: geometry.size.width, height: geometry.size.height, alignment: .center)
        }.edgesIgnoringSafeArea(.all).onDisappear() {
            self.endBluetoothFlow(disconnect: true)
        }
    }
    
    private func startAction() {
        if (currentConfig != nil) {
            if (currentConfig!.currentAction == PassFlowView.ACTION_REMOTEACCESS) {
                PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_REMOTE_ACCESS)
                runRemoteAccess()
            } else if (currentConfig!.currentAction == PassFlowView.ACTION_BLUETOOTH) {
                PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_BLUETOOTH)
                runBluetooth()
            } else {
                PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_ACTION_TYPE)
                LogManager.shared.error(message: "Starting pass action has been cancelled due to invalid type", code: LogCodes.PASSFLOW_ACTION_INVALID_TYPE)
                
                viewModel.update(message: "text_status_message_error_invalid_action", icon: "error")
            }
        } else {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_ACTION_TYPE)
            LogManager.shared.error(message: "Starting pass action has been cancelled due to empty config", code: LogCodes.PASSFLOW_ACTION_EMPTY_CONFIG)
            
            viewModel.update(message: "text_status_message_error_empty_config", icon: "error")
        }
    }
    
    private func runRemoteAccess() {
        if (DelegateManager.shared.isPassFlowCompleted) {
            LogManager.shared.info(message: "Run remote access action has been cancelled due to completed pass flow")
            return
        }
        
        PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED)
        
        if (currentConfig?.qrCode == nil) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_DATA)
            LogManager.shared.warn(message: "Run remote access action has been terminated due to invalid qr code content", code: LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT)
            
            onRemoteAccessFailed(errorCode: nil, message: "text_status_message_error_invalid_qrcode_content")
            
            return
        }
        
        if (currentConfig!.qrCode!.i == nil || currentConfig!.qrCode!.i!.isEmpty) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_ID)
            LogManager.shared.warn(message: "Run remote access action has been terminated due to invalid qr code id", code: LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_ID)
            
            onRemoteAccessFailed(errorCode: nil, message: "text_status_message_error_invalid_qrcode_id")
            
            return
        }
        
        DelegateManager.shared.flowConnectionStateChanged(isActive: true)
        
        PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST)
           
        AccessPointService().remoteOpen(request: RequestAccess(q: currentConfig!.qrCode!.i!), completion: { (result) in
            DispatchQueue.main.async {
                if case .success(_) = result {
                    PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED)
                    
                    self.viewModel.update(message: "text_status_message_succeed", icon: "success")
                    onPassCompleted(success: true, isRemoteAccess: true)
                    
                    DelegateManager.shared.flowConnectionStateChanged(isActive: false)
                } else if case .failure(let error) = result {
                    onRemoteAccessFailed(errorCode: error.code, message: error.message)
                    DelegateManager.shared.flowConnectionStateChanged(isActive: false)
                }
            }
        })
        
    }
    
    private func runBluetooth() {
        if (DelegateManager.shared.isPassFlowCompleted) {
            LogManager.shared.info(message: "Run bluetooth action has been cancelled due to completed pass flow")
            return
        }
        
        PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_BLUETOOTH_STARTED)
        
        if (currentConfig == nil) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_BLUETOOTH_QRCODE_DATA)
            LogManager.shared.error(message: "Run bluetooth action has been cancelled due to empty config", code: LogCodes.PASSFLOW_ACTION_EMPTY_CONFIG)
            
            onBluetoothConnectionFailed(message: "text_status_message_error_empty_config")
            
            return
        }
        
        if (currentConfig!.qrCode == nil) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_BLUETOOTH_QRCODE_DATA)
            LogManager.shared.warn(message: "Run bluetooth action has been terminated due to invalid qr code content", code: LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT)
            
            onBluetoothConnectionFailed(message: "text_status_message_error_invalid_qrcode_content")
            
            return
        }
        
        if (currentConfig!.qrCode!.d == nil) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_BLUETOOTH_DIRECTION)
            LogManager.shared.warn(message: "Run bluetooth action has been terminated due to invalid direction", code: LogCodes.PASSFLOW_ACTION_EMPTY_DIRECTION)
            
            onBluetoothConnectionFailed(message: "text_status_message_error_invalid_direction")
            
            return
        }
        
        if (currentConfig!.qrCode!.h == nil || currentConfig!.qrCode!.h!.isEmpty) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_BLUETOOTH_HARDWARE_ID)
            LogManager.shared.warn(message: "Run bluetooth action has been terminated due to invalid hardware id", code: LogCodes.PASSFLOW_ACTION_EMPTY_HARDWAREID)
            
            onBluetoothConnectionFailed(message: "text_status_message_error_invalid_hardware_id")
            
            return
        }
        
        if (currentConfig!.qrCode!.r == nil) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.INVALID_BLUETOOTH_RELAY_NUMBER)
            LogManager.shared.warn(message: "Run bluetooth action has been terminated due to invalid relay number", code: LogCodes.PASSFLOW_ACTION_EMPTY_RELAYNUMBER)
            
            onBluetoothConnectionFailed(message: "text_status_message_error_invalid_relay_number")
            
            return
        }
        
        BluetoothManager.shared.onScanningStarted           = onBleScanningStarted
        BluetoothManager.shared.onConnectionStateChanged    = onConnectionStateChanged(state:)
        BluetoothManager.shared.onBleStateChanged           = onBleStateChanged(state:)
        
        if (BluetoothManager.shared.getCurrentState().enabled) {
            DelegateManager.shared.flowConnectionStateChanged(isActive: true)
            
            self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
            
            let config: BLEScanConfiguration = BLEScanConfiguration(devices: currentConfig!.devices,
                                                                    userId: ConfigurationManager.shared.getMemberId(),
                                                                    direction: currentConfig!.qrCode!.d!.rawValue,
                                                                    hardwareId: currentConfig!.qrCode!.h!,
                                                                    relayNumber: currentConfig!.qrCode!.r!)
                
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_BLUETOOTH_START_SCAN)
            BluetoothManager.shared.startScan(configuration: config)
        } else {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_ENABLED)
            
            if (ConfigurationManager.shared.waitForBLEEnabled() || currentConfig?.nextAction == nil) {
                if (BluetoothManager.shared.getCurrentState().needAuthorize) {
                    PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION)
                    DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_BLUETOOTH, showMessage: true)
                } else {
                    PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING)
                    DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_ENABLE_BLE, showMessage: false)
                    self.viewModel.update(message: "text_status_message_need_ble_enabled", icon: "warning")
                }
            } else {
                PassFlowManager.shared.addToStates(state: PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_NO_WAIT)
                LogManager.shared.info(message: "Bluetooth disabled now and SDK configuration says no need to wait for it. Ignore Bluetooth scanning and continue to next action.")
                self.onBluetoothConnectionFailed(message: nil)
            }
        }
    }
    
    private func onBleScanningStarted() {
        LogManager.shared.debug(message: "Bluetooth scanning is started, start connection timer")
        self.startConnectionTimer()
    }
    
    private func onBleStateChanged(state: DeviceCapability) {
        self.stateModel.setBluetoothState(state: state.enabled)
        
        // Run Bluetooth if current action is matched and Bluetooth enabled newly
        if (self.currentConfig != nil && self.currentConfig!.currentAction == PassFlowView.ACTION_BLUETOOTH && !self.stateModel.lastBluetoothState && state.enabled) {
            self.runBluetooth()
        }
    }
    
    private func onConnectionStateChanged(state: DeviceConnectionStatus) {
        if (state.state != .connecting) {
            PassFlowManager.shared.addToStates(state: .RUN_ACTION_BLUETOOTH_CONNECTING)
            self.endConnectionTimer()
        }
        
        if (state.state == .connected) {
            PassFlowManager.shared.addToStates(state: .RUN_ACTION_BLUETOOTH_CONNECTED)
            
            self.viewModel.update(message: "text_status_message_succeed", icon: "success")
            onPassCompleted(success: true, isRemoteAccess: false)
            
            self.onBluetoothConnectionSucceed()
        } else if (state.state == .failed
                    || state.state == .notFound
                    || (self.stateModel.lastConnectionState == DeviceConnectionStatus.ConnectionState.connecting && state.state == .disconnected)) {
            PassFlowManager.shared.addToStates(state: .RUN_ACTION_BLUETOOTH_CONNECTION_FAILED)
            self.onBluetoothConnectionFailed(message: nil)
        }
        
        self.stateModel.setConnectionState(state: state.state)
    }
    
    private func startConnectionTimer() {
        let timerConnection = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.bleConnectionTimeout()) , repeats: false, block: { timer in
            LogManager.shared.info(message: "Bluetooth connection timer elapsed");
            DelegateManager.shared.flowConnectionStateChanged(isActive: false)
            
            PassFlowManager.shared.addToStates(state: .RUN_ACTION_BLUETOOTH_TIMEOUT)
            onBluetoothConnectionFailed(message: nil)
        })
        
        self.stateModel.setTimerConnection(timer: timerConnection)
    }
    
    private func endConnectionTimer() {
        if (self.stateModel.timerConnection != nil) {
            self.stateModel.timerConnection!.invalidate()
            self.stateModel.setTimerConnection(timer: nil)
        }
    }
    
    private func onBluetoothConnectionSucceed() {
        endBluetoothFlow(disconnect: true)
    }
    
    private func onBluetoothConnectionFailed(message: String?) {
        endBluetoothFlow(disconnect: true)
        
        if (currentConfig?.nextAction != nil) {
            if (currentConfig!.nextAction! == PassFlowView.ACTION_LOCATION) {
                LogManager.shared.info(message: "Bluetooth connection failed and now validate user location to continue remote access")
                DelegateManager.shared.flowNextActionRequired()
            } else if (currentConfig!.nextAction! == PassFlowView.ACTION_REMOTEACCESS) {
                PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_REMOTE_ACCESS)
                LogManager.shared.info(message: "Bluetooth connection failed and now continue for remote access request")
                
                self.viewModel.update(message: "text_status_message_waiting", icon: "waiting")
                runRemoteAccess()
            } else {
                PassFlowManager.shared.addToStates(state: .INVALID_ACTION_TYPE)
                LogManager.shared.warn(message: "Bluetooth connection failed and next action has invalid value", code: LogCodes.PASSFLOW_ACTION_INVALID_NEXT_ACTION)
                self.viewModel.update(message: "text_status_message_failed", icon: "error")
                onPassCompleted(success: false, isRemoteAccess: false)
            }
        } else {
            self.viewModel.update(message: message != nil ? message! : "text_status_message_failed", icon: "error")
            onPassCompleted(success: false, isRemoteAccess: false)
        }
    }
    
    private func endBluetoothFlow(disconnect: Bool) {
        DelegateManager.shared.flowConnectionStateChanged(isActive: false)
        
        endConnectionTimer()
        BluetoothManager.shared.stopScan(disconnect: disconnect)
        
        BluetoothManager.shared.onScanningStarted = nil
        BluetoothManager.shared.onConnectionStateChanged = nil
        BluetoothManager.shared.onBleStateChanged = nil
    }
    
    private func onRemoteAccessFailed(errorCode: Int?, message: String?) {
        if (errorCode != nil) {
            if (errorCode == 401) {
                PassFlowManager.shared.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED, data: message);
            } else if (errorCode == 404) {
                PassFlowManager.shared.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED, data: message);
            } else if (errorCode == 408) {
                PassFlowManager.shared.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT, data: message);
            } else if (errorCode == 0) {
                PassFlowManager.shared.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_NO_NETWORK, data: message);
            } else {
                PassFlowManager.shared.addToStates(state: .RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED, data: message);
            }
        }
        
        if ((errorCode == nil || errorCode! != 401) && currentConfig?.nextAction != nil && currentConfig?.nextAction! == PassFlowView.ACTION_BLUETOOTH) {
            PassFlowManager.shared.addToStates(state: PassFlowStateCode.PROCESS_ACTION_BLUETOOTH)
            LogManager.shared.info(message: "Remote access request failed and now continue to Bluetooth scanning")
            
            self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
            runBluetooth()
        } else if (errorCode != nil) {
            var failMessage = "text_status_message_failed"
            
            if (errorCode == 401) {
                failMessage = "text_status_message_unauthorized"
            } else if (errorCode == 404) {
                failMessage = "text_status_message_not_connected"
            } else if (errorCode == 408) {
                failMessage = "test_status_message_remoteaccess_timeout"
            }
            
            self.viewModel.update(
                message: (message == nil || message!.isEmpty) ? failMessage : message!,
                icon: "error",
                suggest: errorCode != 401 && (!BluetoothManager.shared.getCurrentState().enabled || BluetoothManager.shared.getCurrentState().needAuthorize))
            
            onPassCompleted(success: false, isRemoteAccess: true)
        } else {
            self.viewModel.update(
                message: (message == nil || message!.isEmpty) ? "text_status_message_failed" : message!,
                icon: "error",
                suggest: !BluetoothManager.shared.getCurrentState().enabled || BluetoothManager.shared.getCurrentState().needAuthorize)
            
            onPassCompleted(success: false, isRemoteAccess: true)
        }
    }
    
    private func onPassCompleted(success: Bool, isRemoteAccess: Bool) {
        if (isRemoteAccess) {
            PassFlowManager.shared.addToStates(state: success
                                               ? .RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED
                                               : .RUN_ACTION_REMOTE_ACCESS_PASS_FAILED)
        } else {
            PassFlowManager.shared.addToStates(state: success
                                               ? .RUN_ACTION_BLUETOOTH_PASS_SUCCEED
                                               : .RUN_ACTION_BLUETOOTH_PASS_FAILED)
        }
        
        DelegateManager.shared.onCompleted(success: success,
                                           isRemoteAccess: isRemoteAccess,
                                           direction: currentConfig?.qrCode?.d,
                                           clubId: currentConfig?.clubInfo?.i,
                                           clubName: currentConfig?.clubInfo?.n)
    }
}

@available(iOS 13.0.0, *)
struct StatusView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            StatusView(config: nil).preferredColorScheme(.dark).environment(\.locale, Locale(identifier: "tr"))
        }
        
    }
}

@available(iOS 13.0, *)
struct LoadingView: View {
    
    @Binding var size: CGFloat
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            
            Circle()
                .stroke(Color(.systemGray3), lineWidth: 14)
                .frame(width: size, height: size)
            
            Circle()
                .trim(from: 0, to: 0.2)
                .stroke(style: StrokeStyle(lineWidth: 7, lineCap: .round, lineJoin: .round))
                .stroke(Color.blue, lineWidth: 7)
                .frame(width: size, height: size)
                .rotationEffect(Angle(degrees: isLoading ? 360 : 0))
                .animation(Animation.linear(duration: 1).repeatForever(autoreverses: false))
                .onAppear() {
                    self.isLoading = true
                }
        }
    }
}
