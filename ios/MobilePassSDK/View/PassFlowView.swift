//
//  PassFlowView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 14.02.2021.
//

import SwiftUI
import AVFoundation

public enum FlowViewType {
    case qr, map, action
}

class ActiveActionModel: ObservableObject {
    @Published var currentView:         FlowViewType = FlowViewType.qr
    @Published var actionList:          [String] = []
    @Published var actionCurrent:       String = ""
    @Published var connectionActive:    Bool = false
    @Published var activeQRCodeContent: QRCodeContent? = nil
    
    func setView(viewType: FlowViewType) {
        self.currentView = viewType
    }
    
    func setAction(list: [String], current: String) {
        self.actionList     = list
        self.actionCurrent  = current
    }
    
    func setQRContent(content: QRCodeContent?) {
        self.activeQRCodeContent = content
    }
    
    func changeConnectionState(isActive: Bool) {
        self.connectionActive = isActive
    }
}

struct PassFlowView: View, PassFlowDelegate {
    // @State private var currentView = FlowViewType.qr
    
    @ObservedObject var viewModel: ActiveActionModel = ActiveActionModel()
    
    public static let ACTION_BLUETOOTH      = "bluetooth"
    public static let ACTION_REMOTEACCESS   = "remoteAccess"
    public static let ACTION_LOCATION       = "location"
    
    private var key: String = "?"
    
    var body: some View {
        NavigationView {
            Group {
                if viewModel.currentView == FlowViewType.qr {
                    ScanQRCodeView()
                } else if viewModel.currentView == FlowViewType.map {
                    MapView(checkPoint: viewModel.activeQRCodeContent?.accessPoint.geoLocation)
                } else {
                    StatusView(config: ActionConfig(currentAction: viewModel.actionCurrent,
                                                    devices: viewModel.activeQRCodeContent?.accessPoint.deviceInfo ?? [],
                                                    accessPointId: viewModel.activeQRCodeContent?.accessPoint.id,
                                                    direction: viewModel.activeQRCodeContent?.action.config.direction,
                                                    deviceNumber: viewModel.activeQRCodeContent?.action.config.deviceNumber,
                                                    relayNumber: viewModel.activeQRCodeContent?.action.config.relayNumber,
                                                    nextAction: viewModel.actionList.count > 0 ? viewModel.actionList.first : nil))
                }
            }.navigationBarTitle(Text(""), displayMode: .inline)
            .navigationBarItems(leading: ZStack(alignment: .leading) {
                Image("poweredBy", bundle: Bundle(for: PassFlowController.self))
                    .frame(width: UIScreen.main.bounds.width).padding(.trailing, 8)
                HStack {
                    Spacer()
                    Button(action: {
                        if (!viewModel.connectionActive) {
                            DelegateManager.shared.onCancelled(dismiss: true)
                        }
                    }) {
                        Text("text_button_close".localized(ConfigurationManager.shared.getLanguage())).bold()
                    }.padding(.trailing, 10)
                }
                .frame(width: UIScreen.main.bounds.width)
            })
        }.environment(\.locale, Locale(identifier: ConfigurationManager.shared.getLanguage()))
    }
    
    init(key: String) {
        self.checkCameraPermission()
        
        self.key = key
        DelegateManager.shared.setPassFlowDelegate(delegate: self)
    }
    
    
    func onQRCodeFound(code: String) {
        processQRCodeData(code: code)
    }
    
    func onLocationValidated() {
        checkNextAction()
    }
    
    func onNextActionRequired() {
        checkNextAction()
    }
    
    func onConnectionStateChanged(isActive: Bool) {
        viewModel.changeConnectionState(isActive: isActive)
        // TODO Call that from related points
    }
    
    private func checkCameraPermission() -> Void {
        let videoAuthStatus = AVCaptureDevice.authorizationStatus(for: .video)
        switch (videoAuthStatus) {
        case .authorized:
            LogManager.shared.info(message: "Camera Permission Status: Authorized")
            break
            
        case .denied, .restricted:
            LogManager.shared.info(message: "Camera Permission Status: Denied or Restricted, needs to be changed in settings to continue")
            DelegateManager.shared.needPermissionCamera()
            break
            
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted) in
                
                guard granted else {
                    LogManager.shared.info(message: "Camera Permission Status: Denied, needs to be changed in settings to continue")
                    DelegateManager.shared.needPermissionCamera()
                    return
                }
                
                LogManager.shared.info(message: "Camera Permission Status: Authorized")
            })
            break
        @unknown default:
            LogManager.shared.info(message: "Camera Permission Status: Unknown!")
            DelegateManager.shared.needPermissionCamera()
        }
    }
    
    private func checkNextAction() {
        if (viewModel.actionList.count > 0) {
            let nextOne: String = viewModel.actionList.removeFirst()
            
            viewModel.setAction(list: viewModel.actionList, current: nextOne)
            processAction()
        } else {
            DelegateManager.shared.onCancelled(dismiss: true)
        }
    }
    
    private func processQRCodeData(code: String) {
        viewModel.activeQRCodeContent = ConfigurationManager.shared.getQRCodeContent(qrCodeData: code)
        
        if (viewModel.activeQRCodeContent != nil) {
            let trigger: ResponseAccessPointItemQRCodeItemTrigger = viewModel.activeQRCodeContent!.action.config.trigger
            
            let needLocation: Bool = trigger.validateGeoLocation == true && viewModel.activeQRCodeContent!.accessPoint.geoLocation != nil
            LogManager.shared.debug(message: "Need location: \(needLocation)")
            
            var actionList: [String] = []
            var actionCurrent: String = ""
            
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
            
            viewModel.setAction(list: actionList, current: actionCurrent)
            processAction()
        }
    }
    
    private func processAction() {
        if (viewModel.actionCurrent.count == 0) {
            return;
        }
        
        LogManager.shared.debug(message: "Current action: \(viewModel.actionCurrent)");
        LogManager.shared.debug(message: "Action list: \(viewModel.actionList)");
        
        if (viewModel.actionCurrent == PassFlowView.ACTION_LOCATION) {
            viewModel.setView(viewType: FlowViewType.map)
        } else {
            viewModel.setView(viewType: FlowViewType.action)
        }
    }
}

struct PassFlowView_Previews: PreviewProvider {
    static var previews: some View {
        PassFlowView(key: "PREVIEW")
    }
}
