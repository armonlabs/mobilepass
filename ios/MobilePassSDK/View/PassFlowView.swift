//
//  PassFlowView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 14.02.2021.
//

import SwiftUI
import AVFoundation

public enum FlowViewType {
    case qr, map, action, permission
}

class ActiveActionModel: ObservableObject {
    @Published var currentView:         FlowViewType = FlowViewType.qr
    @Published var actionList:          [String] = []
    @Published var actionCurrent:       String = ""
    @Published var activeQRCodeContent: QRCodeContent? = nil
    @Published var needPermissionType:  Int = NeedPermissionType.NEED_ENABLE_BLE.rawValue
    
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
    
    func setNeedPermissionType(type: Int) {
        self.needPermissionType = type
        self.currentView = FlowViewType.permission
    }
}

struct PassFlowView: View, PassFlowDelegate {
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
                } else if viewModel.currentView == FlowViewType.action {
                    StatusView(config: ActionConfig(currentAction: viewModel.actionCurrent,
                                                    devices: viewModel.activeQRCodeContent?.accessPoint.deviceInfo ?? [],
                                                    accessPointId: viewModel.activeQRCodeContent?.accessPoint.id,
                                                    direction: viewModel.activeQRCodeContent?.action.config.direction,
                                                    deviceNumber: viewModel.activeQRCodeContent?.action.config.deviceNumber,
                                                    relayNumber: viewModel.activeQRCodeContent?.action.config.relayNumber,
                                                    nextAction: viewModel.actionList.count > 0 ? viewModel.actionList.first : nil))
                } else {
                    PermissionView(type: viewModel.needPermissionType)
                }
            }.navigationBarTitle(Text(""), displayMode: .inline)
            .navigationBarItems(leading: ZStack(alignment: .leading) {
                Image("poweredBy", bundle: Bundle(for: PassFlowController.self))
                    .frame(width: UIScreen.main.bounds.width).padding(.trailing, 8)
                HStack {
                    Spacer()
                    Button(action: {
                        DelegateManager.shared.onCancelled(dismiss: true)
                    }) {
                        Text("text_button_close".localized(ConfigurationManager.shared.getLanguage())).bold()
                    }.padding(.trailing, 10)
                }
                .frame(width: UIScreen.main.bounds.width)
            })
        }.environment(\.locale, Locale(identifier: ConfigurationManager.shared.getLanguage())).edgesIgnoringSafeArea(.all).onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
            if (DelegateManager.shared.isPassConnectionActive) {
                DelegateManager.shared.onCancelled(dismiss: true)
            }
        }
    }
    
    init(key: String) {
        self.key = key
        DelegateManager.shared.setPassFlowDelegate(delegate: self)
        
        self.checkCameraPermission()
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
    
    func onNeedPermissionMessage(type: Int) {
        viewModel.setNeedPermissionType(type: type)
    }
    
    private func checkCameraPermission() -> Void {
        let videoAuthStatus = AVCaptureDevice.authorizationStatus(for: .video)
        switch (videoAuthStatus) {
        case .authorized:
            LogManager.shared.info(message: "Camera Permission Status: Authorized")
            break
            
        case .denied, .restricted:
            LogManager.shared.info(message: "Camera Permission Status: Denied or Restricted, needs to be changed in settings to continue")
            DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_CAMERA, showMessage: true)
            break
            
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted) in
                
                guard granted else {
                    LogManager.shared.info(message: "Camera Permission Status: Denied, needs to be changed in settings to continue")
                    DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_CAMERA, showMessage: true)
                    return
                }
                
                LogManager.shared.info(message: "Camera Permission Status: Authorized")
            })
            break
        @unknown default:
            LogManager.shared.info(message: "Camera Permission Status: Unknown!")
            DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_CAMERA, showMessage: true)
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
