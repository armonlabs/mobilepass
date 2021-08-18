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
    var qrCode:         ResponseAccessPointListQRCode? // TODO Must be checked before use
    var nextAction:     String?
}

class CurrentStatusModel: ObservableObject {
    @Published var icon:        String  = "waiting"
    @Published var message:     String  = "text_status_message_waiting"
    
    func update(message: String, icon: String) {
        self.message    = message
        self.icon       = icon
    }
}

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
                Image(self.viewModel.icon, bundle: Bundle(for: PassFlowController.self)).resizable().frame(width: geometry.size.width * 0.5, height: geometry.size.width * 0.5, alignment: .center)
                Text(self.viewModel.message.localized(locale.identifier)).fontWeight(.medium).padding(.top, 48).padding(.bottom, geometry.size.height * 0.35).multilineTextAlignment(.center)
            }.padding(.horizontal, 14)
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
        }.edgesIgnoringSafeArea(.all).onDisappear() {
            self.endBluetoothFlow(disconnect: true)
        }
    }
    
    private func startAction() {
        if (currentConfig != nil) {
            if (currentConfig!.currentAction == PassFlowView.ACTION_REMOTEACCESS) {
                runRemoteAccess()
            } else if (currentConfig!.currentAction == PassFlowView.ACTION_BLUETOOTH) {
                runBluetooth()
            }
        }
    }
    
    private func runRemoteAccess() {
        if (DelegateManager.shared.isPassFlowCompleted) {
            return
        }
        
        if (currentConfig?.qrCode?.d != nil) {
            DelegateManager.shared.flowConnectionStateChanged(isActive: true)
           
            AccessPointService().remoteOpen(request: RequestAccess(q: currentConfig!.qrCode!.i, c: ConfigurationManager.shared.getMemberId()), completion: { (result) in
                if case .success(_) = result {
                    self.viewModel.update(message: "text_status_message_succeed", icon: "success")
                    DelegateManager.shared.onCompleted(succeed: true)
                    
                    DelegateManager.shared.flowConnectionStateChanged(isActive: false)
                } else if case .failure(let error) = result {
                    if (error.code != 401 && currentConfig?.nextAction != nil && currentConfig?.nextAction! == PassFlowView.ACTION_BLUETOOTH) {
                        self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
                        runBluetooth()
                    } else {
                        var failMessage = "text_status_message_failed"
                        
                        if (error.code == 401) {
                            failMessage = "text_status_message_unauthorized"
                        } else if (error.code == 404) {
                            failMessage = "text_status_message_not_connected"
                        }
                        
                        self.viewModel.update(message: error.message.isEmpty ? failMessage : error.message, icon: "error")
                        DelegateManager.shared.onCompleted(succeed: false)
                    }
                    
                    DelegateManager.shared.flowConnectionStateChanged(isActive: false)
                }
            })
        } else {
            // TODO Handle here
        }
    }
    
    private func runBluetooth() {
        if (DelegateManager.shared.isPassFlowCompleted) {
            return
        }
        
        BluetoothManager.shared.onScanningStarted = onBleScanningStarted
        BluetoothManager.shared.onConnectionStateChanged = onConnectionStateChanged(state:)
        BluetoothManager.shared.onBleStateChanged = onBleStateChanged(state:)
        
        if (BluetoothManager.shared.getCurrentState().enabled) {
            DelegateManager.shared.flowConnectionStateChanged(isActive: true)
            
            self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
            
            if (currentConfig != nil) {
                let config: BLEScanConfiguration = BLEScanConfiguration(devices: currentConfig!.devices,
                                                                        userId: ConfigurationManager.shared.getMemberId(),
                                                                        direction: currentConfig!.qrCode!.d.rawValue,
                                                                        hardwareId: currentConfig!.qrCode!.h, // TODO Check null possibility
                                                                        relayNumber: currentConfig!.qrCode!.r)
                
                BluetoothManager.shared.startScan(configuration: config)
            }
        } else {
            if (ConfigurationManager.shared.waitForBLEEnabled() || currentConfig?.nextAction == nil) {
                if (BluetoothManager.shared.getCurrentState().needAuthorize) {
                    DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_BLUETOOTH, showMessage: true)
                } else {
                    DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_ENABLE_BLE, showMessage: false)
                    self.viewModel.update(message: "text_status_message_need_ble_enabled", icon: "warning")
                }
            } else {
                self.onBluetoothConnectionFailed()
            }
        }
    }
    
    private func onBleScanningStarted() {
        LogManager.shared.debug(message: "Bluetooth scanning is started")
        self.startConnectionTimer()
    }
    
    private func onBleStateChanged(state: DeviceCapability) {
        self.stateModel.setBluetoothState(state: state.enabled)
        
        if (self.currentConfig != nil && self.currentConfig!.currentAction == PassFlowView.ACTION_BLUETOOTH && !self.stateModel.lastBluetoothState && state.enabled) {
            self.runBluetooth()
        }
    }
    
    private func onConnectionStateChanged(state: DeviceConnectionStatus) {
        if (state.state != .connecting) {
            self.endConnectionTimer()
        }
        
        if (state.state == .connected) {
            self.viewModel.update(message: "text_status_message_succeed", icon: "success")
            DelegateManager.shared.onCompleted(succeed: true)
            self.onBluetoothConnectionSucceed()
        } else if (state.state == .failed
                    || state.state == .notFound
                    || (self.stateModel.lastConnectionState == DeviceConnectionStatus.ConnectionState.connecting && state.state == .disconnected)) {
            self.onBluetoothConnectionFailed()
        }
        
        self.stateModel.setConnectionState(state: state.state)
    }
    
    private func startConnectionTimer() {
        let timerConnection = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.bleConnectionTimeout()) , repeats: false, block: { timer in
            DelegateManager.shared.flowConnectionStateChanged(isActive: false)
            
            if (currentConfig != nil && currentConfig!.currentAction == PassFlowView.ACTION_REMOTEACCESS) {
                if (currentConfig?.nextAction != nil && currentConfig?.nextAction! == PassFlowView.ACTION_BLUETOOTH) {
                    self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
                    runBluetooth()
                } else {
                    self.viewModel.update(message: "text_status_message_timeout", icon: "error")
                    DelegateManager.shared.onCompleted(succeed: false)
                }
            } else {
                onBluetoothConnectionFailed()
            }
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
    
    private func onBluetoothConnectionFailed() {
        endBluetoothFlow(disconnect: true)
        
        if (currentConfig?.nextAction != nil) {
            if (currentConfig!.nextAction! == PassFlowView.ACTION_LOCATION) {
                DelegateManager.shared.flowNextActionRequired()
            } else if (currentConfig!.nextAction! == PassFlowView.ACTION_REMOTEACCESS) {
                self.viewModel.update(message: "text_status_message_waiting", icon: "waiting")
                runRemoteAccess()
            }
        } else {
            self.viewModel.update(message: "text_status_message_failed", icon: "error")
            DelegateManager.shared.onCompleted(succeed: false)
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
    
    
}

struct StatusView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            StatusView(config: nil).preferredColorScheme(.dark).environment(\.locale, Locale(identifier: "tr"))
        }
        
    }
}

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
