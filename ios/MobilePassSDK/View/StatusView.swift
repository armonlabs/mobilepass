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
    var currentAction:      String
    var devices:            [ResponseAccessPointItemDeviceInfo]
    var accessPointId:      String?
    var direction:          Direction?
    var deviceNumber:       Int?
    var relayNumber:        Int?
    var nextAction:         String?
}

class CurrentStatusModel: ObservableObject {
    @Published var icon:        String  = "waiting"
    @Published var message:     String  = "text_status_message_waiting"
    
    func update(message: String, icon: String) {
        self.message    = message
        self.icon       = icon
    }
}

struct StatusView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.locale) var locale
    
    private var currentConfig: ActionConfig?
    
    @State private var timerBluetooth:      Timer?
    @State private var lastConnectionState: DeviceConnectionStatus.ConnectionState?
    @State private var lastBluetoothState:  Bool = BluetoothManager.shared.getCurrentState().enabled
    
    @ObservedObject var viewModel: CurrentStatusModel
    
    init(config: ActionConfig?) {
        self.viewModel = CurrentStatusModel()
        
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
        }.edgesIgnoringSafeArea(.all)
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
        
        if (currentConfig?.accessPointId != nil && currentConfig?.direction != nil) {
            AccessPointService().remoteOpen(request: RequestAccess(accessPointId: currentConfig!.accessPointId!, clubMemberId: ConfigurationManager.shared.getMemberId(), direction: currentConfig!.direction!), completion: { (result) in
                if case .success(_) = result {
                    self.viewModel.update(message: "text_status_message_succeed", icon: "success")
                    DelegateManager.shared.onCompleted(succeed: true)
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
                }
            })
        }
    }
    
    private func runBluetooth() {
        if (DelegateManager.shared.isPassFlowCompleted) {
            return
        }
        
        BluetoothManager.shared.onScanningStarted = {[self] () -> () in
            LogManager.shared.debug(message: "Bluetooth scanning is started")
            self.startConnectionTimer()
        }
        
        BluetoothManager.shared.onConnectionStateChanged = {[self] (state: DeviceConnectionStatus) -> () in
            if (state.state != .connecting) {
                self.endConnectionTimer()
            }
            
            if (state.state == .connected) {
                self.viewModel.update(message: "text_status_message_succeed", icon: "success")
                DelegateManager.shared.onCompleted(succeed: true)
            } else if (state.state == .failed
                        || state.state == .notFound
                        || (self.lastConnectionState == DeviceConnectionStatus.ConnectionState.connecting && state.state == .disconnected)) {
                self.onBluetoothConnectionFailed()
            }
            
            self.lastConnectionState = state.state
        }
        
        BluetoothManager.shared.onBleStateChanged = {[self] (state: DeviceCapability) -> () in
            if (self.currentConfig != nil && self.currentConfig!.currentAction == PassFlowView.ACTION_BLUETOOTH && !self.lastBluetoothState && state.enabled) {
                self.lastBluetoothState = state.enabled
                self.runBluetooth()
            } else {
                self.lastBluetoothState = state.enabled
            }
        }
        
        if (BluetoothManager.shared.getCurrentState().enabled) {
            self.viewModel.update(message: "text_status_message_scanning", icon: "waiting")
            
            if (currentConfig != nil) {
                let config: BLEScanConfiguration = BLEScanConfiguration(devices: currentConfig!.devices,
                                                                        userId: ConfigurationManager.shared.getMemberId(),
                                                                        direction: currentConfig!.direction!.rawValue,
                                                                        deviceNumber: currentConfig!.deviceNumber!,
                                                                        relayNumber: currentConfig!.relayNumber!)
                
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
    
    private func startConnectionTimer() {
        timerBluetooth = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.bleConnectionTimeout()) , repeats: false, block: { timer in
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
    }
    
    private func endConnectionTimer() {
        if (timerBluetooth != nil) {
            timerBluetooth!.invalidate()
            timerBluetooth = nil
        }
    }
    
    private func onBluetoothConnectionFailed() {
        endConnectionTimer()
        BluetoothManager.shared.stopScan(disconnect: true)
        
        BluetoothManager.shared.onScanningStarted = nil
        BluetoothManager.shared.onConnectionStateChanged = nil
        BluetoothManager.shared.onBleStateChanged = nil
        
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
