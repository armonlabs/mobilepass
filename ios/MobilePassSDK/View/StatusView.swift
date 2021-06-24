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
    var isRemoteAccess:     Bool
    var devices:            [ResponseAccessPointItemDeviceInfo]
    var accessPointId:      String?
    var direction:          Direction?
    var deviceNumber:       Int?
    var relayNumber:        Int?
    var nextAction:         String?
}

class CurrentStatusModel: ObservableObject {
    @Published var color:       Color   = Color.gray
    @Published var icon:        String  = "checkmark.circle"
    @Published var showSpinner: Bool    = true
    @Published var message:     String  = "text_status_message_waiting"
    
    func update(color: Color, message: String, showSpinner: Bool, icon: String) {
        self.color          = color
        self.message        = message
        self.showSpinner    = showSpinner
        self.icon           = icon
    }
}

struct StatusView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.locale) var locale
    
    private var currentConfig: ActionConfig?
    
    @State private var timerBluetooth:      Timer?
    @State private var lastConnectionState: DeviceConnectionStatus.ConnectionState?
    
    @ObservedObject var viewModel: CurrentStatusModel
    
    init(config: ActionConfig?) {
        self.viewModel = CurrentStatusModel()
        
        if (config != nil) {
            self.currentConfig = config
            self.startAction()
        }
        
        BluetoothManager.shared.onScanningStarted = {[self] () -> () in
            self.startBluetoothTimer()
        }
        
        BluetoothManager.shared.onConnectionStateChanged = {[self] (state: DeviceConnectionStatus) -> () in
            if (state.state != .connecting) {
                endBluetoothTimer()
            }
            
            if (state.state == .connected) {
                self.viewModel.update(color: .green, message: "text_status_message_succeed", showSpinner: false, icon: "checkmark.circle")
                DelegateManager.shared.onCompleted(succeed: true)
            } else if (state.state == .failed
                        || state.state == .notFound
                        || (lastConnectionState == DeviceConnectionStatus.ConnectionState.connecting && state.state == .disconnected)) {
                self.onBluetoothConnectionFailed()
            }
            
            lastConnectionState = state.state
        }
    }
    
    var body: some View {
        GeometryReader { (geometry) in
            VStack(alignment: .center) {
                if self.viewModel.showSpinner {
                    LoadingView(size: .constant(geometry.size.width * 0.22))
                } else {
                    Image(systemName: self.viewModel.icon).resizable().frame(width: geometry.size.width * 0.25, height: geometry.size.width * 0.25, alignment: .center).foregroundColor(self.viewModel.color)
                }
                Text(self.viewModel.message.localized(locale.identifier)).padding(.top, 48).padding(.bottom, geometry.size.height * 0.25).multilineTextAlignment(.center)
            }.padding(.horizontal, 14)
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
        }.background(LinearGradient(gradient: Gradient(colors: [self.viewModel.color, colorScheme == .dark ? Color.black.opacity(0.0) : Color.white.opacity(0.0)]), startPoint: .top, endPoint: .center)).edgesIgnoringSafeArea(.all)
    }
    
    private func startAction() {
        if (currentConfig != nil) {
            if (currentConfig!.isRemoteAccess) {
                runRemoteAccess()
            } else {
                runBluetooth()
            }
        }
    }
    
    private func runRemoteAccess() {
        if (currentConfig?.accessPointId != nil && currentConfig?.direction != nil) {
            AccessPointService().remoteOpen(request: RequestAccess(accessPointId: currentConfig!.accessPointId!, clubMemberId: ConfigurationManager.shared.getMemberId(), direction: currentConfig!.direction!), completion: { (result) in
                if case .success(_) = result {
                    self.viewModel.update(color: .green, message: "text_status_message_succeed", showSpinner: false, icon: "checkmark.circle")
                    DelegateManager.shared.onCompleted(succeed: true)
                } else if case .failure(let error) = result {
                    if (error.code != 401 && currentConfig?.nextAction != nil && currentConfig?.nextAction! == PassFlowView.ACTION_BLUETOOTH) {
                        self.viewModel.update(color: .gray, message: "text_status_message_scanning", showSpinner: true, icon: "")
                        runBluetooth()
                    } else {
                        var failMessage = "text_status_message_failed"
                        
                        if (error.code == 401) {
                            failMessage = "text_status_message_unauthorized"
                        } else if (error.code == 404) {
                            failMessage = "text_status_message_not_connected"
                        }
                        
                        self.viewModel.update(color: .red, message: failMessage, showSpinner: false, icon: "multiply.circle.fill")
                        DelegateManager.shared.onCompleted(succeed: false)
                    }
                }
            })
        }
    }
    
    private func runBluetooth() {
        self.viewModel.update(color: .gray, message: "text_status_message_scanning", showSpinner: true, icon: "")
        
        if (currentConfig != nil) {
            let config: BLEScanConfiguration = BLEScanConfiguration(devices: currentConfig!.devices,
                                                                    userId: ConfigurationManager.shared.getMemberId(),
                                                                    direction: currentConfig!.direction!.rawValue,
                                                                    deviceNumber: currentConfig!.deviceNumber!,
                                                                    relayNumber: currentConfig!.relayNumber!)
            
            BluetoothManager.shared.startScan(configuration: config)
        }
    }
    
    private func startBluetoothTimer() {
        timerBluetooth = Timer.scheduledTimer(withTimeInterval: Double(ConfigurationManager.shared.bleConnectionTimeout()) , repeats: false, block: { timer in
            onBluetoothConnectionFailed()
        })
    }
    
    private func endBluetoothTimer() {
        if (timerBluetooth != nil) {
            timerBluetooth!.invalidate()
            timerBluetooth = nil
        }
    }
    
    private func onBluetoothConnectionFailed() {
        BluetoothManager.shared.stopScan(disconnect: true)
        
        if (currentConfig?.nextAction != nil) {
            if (currentConfig!.nextAction! == PassFlowView.ACTION_LOCATION) {
                DelegateManager.shared.flowNextActionRequired()
            } else if (currentConfig!.nextAction! == PassFlowView.ACTION_REMOTEACCESS) {
                self.viewModel.update(color: .gray, message: "text_status_message_waiting", showSpinner: true, icon: "")
                runRemoteAccess()
            }
        } else {
            self.viewModel.update(color: .red, message: "text_status_message_failed", showSpinner: false, icon: "multiply.circle.fill")
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
