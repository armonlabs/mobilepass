//
//  PassFlowView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 14.02.2021.
//

import SwiftUI

public enum FlowViewType {
    case qr, map, action
}

struct PassFlowView: View, PassFlowDelegate {
    @State private var currentView = FlowViewType.qr
    
    @State private var actionList:          [String] = []
    @State private var actionCurrent:       String = ""
    @State private var connectionActive:    Bool = false
    @State private var activeQRCodeContent: QRCodeContent? = nil
    
    public static let ACTION_BLUETOOTH      = "bluetooth"
    public static let ACTION_REMOTEACCESS   = "remoteAccess"
    public static let ACTION_LOCATION       = "location"
    
    var body: some View {
        NavigationView {
            GeometryReader { (geometry) in
                Group {
                    if currentView == FlowViewType.qr {
                        ScanQRCodeView(delegate: self)
                    } else if currentView == FlowViewType.map {
                        MapView(delegate: self, checkPoint: activeQRCodeContent?.accessPoint.geoLocation)
                    } else {
                        StatusView(delegate: self, config: ActionConfig(isRemoteAccess: actionCurrent == PassFlowView.ACTION_REMOTEACCESS,
                                                                        deviceId: activeQRCodeContent?.accessPoint.deviceInfo.id,
                                                                        accessPointId: activeQRCodeContent?.accessPoint.id,
                                                                        hardwareId: activeQRCodeContent?.action.config.hardwareId,
                                                                        direction: activeQRCodeContent?.action.config.direction,
                                                                        deviceNumber: activeQRCodeContent?.action.config.deviceNumber,
                                                                        relayNumber: activeQRCodeContent?.action.config.relayNumber,
                                                                        devicePublicKey: activeQRCodeContent?.accessPoint.deviceInfo.publicKey,
                                                                        nextAction: actionList.count > 0 ? actionList.first : nil))
                    }
                }.navigationBarTitle(Text(""), displayMode: .inline)
                .navigationBarItems(leading: ZStack(alignment: .leading) {
                    Image("poweredBy", bundle: Bundle(for: PassFlowController.self))
                        .frame(width: geometry.size.width).padding(.trailing, 8)
                    HStack {
                        Spacer()
                        Button(action: {
                            if (!connectionActive) {
                                DelegateManager.shared.onCancelled(dismiss: true)
                            }
                        }) {
                            Text("Close").bold()
                        }.padding(.trailing, 10)
                    }
                    .frame(width: geometry.size.width)
                }
                )
            }
        }.environment(\.locale, Locale(identifier: ConfigurationManager.shared.getLanguage()))
    }
    
    
    func onQRCodeFound(code: String) {
        processQRCodeData(code: code)
    }
    
    func onLocationValidated() {
        LogManager.shared.debug(message: "On Location Valid, action list: \(actionList)")
        
        checkNextAction()
    }
    
    func onPassCompleted(succeed: Bool) {
        DelegateManager.shared.onCompleted(succeed: succeed)
    }
    
    func onNextActionRequired() {
        checkNextAction()
    }
    
    func onConnectionStateChanged(isActive: Bool) {
        connectionActive = isActive
        // TODO Call that from related points
    }
    
    func needPermissionCamera() {
        DelegateManager.shared.needPermissionCamera()
    }
    
    func needPermissionLocation() {
        DelegateManager.shared.needPermissionLocation()
    }
    
    func needEnableBluetooth() {
        DelegateManager.shared.needBluetoothEnabled()
    }
    
    func onError() {
        DelegateManager.shared.errorOccurred()
    }
    
    
    private func checkNextAction() {
        if (actionList.count > 0) {
            actionCurrent = actionList.removeFirst()
            processAction()
        } else {
            DelegateManager.shared.onCancelled(dismiss: true)
        }
    }
    
    private func processQRCodeData(code: String) {
        activeQRCodeContent = ConfigurationManager.shared.getQRCodeContent(qrCodeData: code)
        
        if (activeQRCodeContent != nil) {
            let trigger: ResponseAccessPointItemQRCodeItemTrigger = self.activeQRCodeContent!.action.config.trigger
            
            let needLocation: Bool = trigger.validateGeoLocation == true && activeQRCodeContent!.accessPoint.geoLocation != nil
            LogManager.shared.debug(message: "Need location: \(needLocation)")
            
            actionList = []
            
            switch trigger.type {
            case QRTriggerType.Bluetooth:
                LogManager.shared.debug(message: "Trigger Type: Bluetooth");
                actionCurrent = PassFlowView.ACTION_BLUETOOTH
                break
            case QRTriggerType.BluetoothThenRemote:
                LogManager.shared.debug(message: "Trigger Type: Bluetooth Then Remote");
                actionCurrent = PassFlowView.ACTION_BLUETOOTH
                
                if (needLocation) {
                    actionList.append(PassFlowView.ACTION_LOCATION)
                }
                
                actionList.append(PassFlowView.ACTION_REMOTEACCESS)
                break
            case QRTriggerType.Remote, QRTriggerType.RemoteThenBluetooth:
                actionCurrent = needLocation ? PassFlowView.ACTION_LOCATION : PassFlowView.ACTION_REMOTEACCESS
                
                if (needLocation) {
                    actionList.append(PassFlowView.ACTION_REMOTEACCESS)
                }
                
                if (trigger.type == QRTriggerType.RemoteThenBluetooth) {
                    LogManager.shared.debug(message: "Trigger Type: Remote Then Bluetooth");
                    actionList.append(PassFlowView.ACTION_BLUETOOTH);
                } else {
                    LogManager.shared.debug(message: "Trigger Type: Remote");
                }
                break
            }
            
            processAction()
        }
    }
    
    private func processAction() {
        if (actionCurrent.count == 0) {
            return;
        }
        
        LogManager.shared.debug(message: "Current action: \(actionCurrent)");
        LogManager.shared.debug(message: "Action list: \(actionList)");
        
        if (actionCurrent == PassFlowView.ACTION_LOCATION) {
            currentView = FlowViewType.map
        } else {
            currentView = FlowViewType.action
        }
    }
}

struct PassFlowView_Previews: PreviewProvider {
    static var previews: some View {
        PassFlowView()
    }
}
