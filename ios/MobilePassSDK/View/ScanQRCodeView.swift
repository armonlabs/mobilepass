//
//  ScanQRCodeView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 13.02.2021.
//

import SwiftUI
import AVFoundation

@available(iOS 13.0, *)
class CurrentListStateModel: ObservableObject {
    @Published var state:           Int     = QRCodeListState.INITIALIZING.rawValue
    @Published var message:         String  = "text_qrcode_list_state_initializing"
    @Published var qrCode:          String  = ""
    @Published var backgroundColor: Color   = Color.black
    
    private var timerInstance: Timer? = nil
    
    init(state: Int) {
        self.update(state: state)
        PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_STARTED)
    }
    
    func update(state: Int) {
        self.state      = state
        self.message    = self.getQRCodeListStateLabel(state: state)
    }
    
    func setInvalid(isInvalidContent: Bool, isInvalidFormat: Bool, qrCodeContent: String) {
        if (timerInstance != nil) {
            timerInstance!.invalidate()
            timerInstance = nil
        }
        
        if (isInvalidContent) {
            message = "text_qrcode_invalid"
        } else if (isInvalidFormat) {
            message = "text_qrcode_unknown"
        } else {
            message = "text_qrcode_not_found"
        }
        
        // qrCode = "\(qrCodeContent) [\(ConfigurationManager.shared.getQRCodesCount())]"
        qrCode = "[\(ConfigurationManager.shared.getQRCodesCount())] / \(ConfigurationManager.shared.getMemberId())"
        backgroundColor = isInvalidContent ? Color.orange : Color.red
        
        self.timerInstance = Timer.scheduledTimer(withTimeInterval: Double(4), repeats: false, block: {_ in
            DispatchQueue.main.async {
                self.message            = self.getQRCodeListStateLabel(state: self.state)
                self.qrCode             = ""
                self.backgroundColor    = Color.black
            }
        })
    }
    
    private func getQRCodeListStateLabel(state: Int) -> String {
        switch state {
        case QRCodeListState.INITIALIZING.rawValue:
            return "text_qrcode_list_state_initializing"
        case QRCodeListState.SYNCING.rawValue:
            return "text_qrcode_list_state_syncing"
        case QRCodeListState.EMPTY.rawValue:
            return "text_qrcode_list_state_empty"
        case QRCodeListState.USING_STORED_DATA.rawValue:
            return "text_qrcode_list_state_stored_data"
        case QRCodeListState.USING_SYNCED_DATA.rawValue:
            return "text_qrcode_list_state_synced_data"
        default:
            return ""
        }
    }
}

@available(iOS 13.0, *)
struct ScanQRCodeView: View, QRCodeListStateDelegate {
    @Environment(\.locale) var locale
    
    @ObservedObject var stateModel: CurrentListStateModel
    
    private enum MaskSide {
        case top, bottom, left, right
    }
    
    init() {
        self.stateModel = CurrentListStateModel(state: DelegateManager.shared.qrCodeListState.rawValue)
        
        DelegateManager.shared.setQRCodeStateDelegate(delegate: self)
    }
    
    var body: some View {
        ZStack {
            QRCodeReaderView(
                completion: { result in
                    if (result.result == .success) {
                        DelegateManager.shared.flowQRCodeFound(code: result.code)
                    } else {
                        LogManager.shared.error(message: "ScanQRCodeView error: \(result.result)", code: LogCodes.UI_CAMERA_SETUP_FAILED)
                        switch result.result {
                        case .cameraUnavailable:
                            stateModel.message = "text_status_message_camera_unavailable"
                            stateModel.backgroundColor = Color.red
                        case .getCaptureDeviceFailed:
                            stateModel.message = "text_status_message_camera_not_found"
                            stateModel.backgroundColor = Color.red
                        case .createVideoInputFailed:
                            stateModel.message = "text_status_message_camera_input_failed"
                            stateModel.backgroundColor = Color.red
                        case .addInputFailed:
                            stateModel.message = "text_status_message_camera_input_add_failed"
                            stateModel.backgroundColor = Color.red
                        case .addOutputFailed:
                            stateModel.message = "text_status_message_camera_output_add_failed"
                            stateModel.backgroundColor = Color.red
                        case .missingSession:
                            stateModel.message = "text_status_message_camera_session_failed"
                            stateModel.backgroundColor = Color.red
                        case .createPreviewLayerFailed:
                            stateModel.message = "text_status_message_camera_preview_failed"
                            stateModel.backgroundColor = Color.red
                        case .startSessionFailed:
                            stateModel.message = "text_status_message_camera_start_failed"
                            stateModel.backgroundColor = Color.red
                        default:
                            if (ConfigurationManager.shared.closeWhenInvalidQRCode() && result.result == .invalidFormat) {
                                DelegateManager.shared.flowCloseWithInvalidQRCode(code: result.code)
                            } else {
                                stateModel.setInvalid(isInvalidContent: result.result == .invalidContent,
                                                      isInvalidFormat: result.result == .invalidFormat,
                                                      qrCodeContent: result.code)
                            }
                        }
                    }
                }
            )
            GeometryReader { (geometry) in
                createMask(geometry: geometry, side: .top)
                createMask(geometry: geometry, side: .left)
                createMask(geometry: geometry, side: .right)
                createMask(geometry: geometry, side: .bottom)
            }
        }.edgesIgnoringSafeArea(.bottom)
    }
    
    func onStateChanged(state: Int) {
        stateModel.update(state: state)
    }
    
    private func createMask(geometry: GeometryProxy, side: MaskSide) -> some View {
        let isPortrait = geometry.size.height > geometry.size.width;
        let qrCodeArea = isPortrait ? geometry.size.height / 3 : geometry.size.height / 2
        
        var maskWidth: CGFloat = 0
        var maskHeight: CGFloat = 0
        var maskPositionX: CGFloat = 0
        var maskPositionY: CGFloat = 0
        
        switch side {
        case .top:
            maskWidth = geometry.size.width
            maskHeight = (geometry.size.height - qrCodeArea) / 2
            maskPositionX = maskWidth / 2
            maskPositionY = maskHeight / 2
            break
        case .bottom:
            maskWidth = geometry.size.width
            maskHeight = (geometry.size.height - qrCodeArea) / 2
            maskPositionX = maskWidth / 2
            maskPositionY = geometry.size.height - (maskHeight / 2)
            break
        case .left:
            maskWidth = (geometry.size.width - qrCodeArea) / 2
            maskHeight = qrCodeArea
            maskPositionX = maskWidth / 2
            maskPositionY = geometry.size.height / 2
            break
        case .right:
            maskWidth = (geometry.size.width - qrCodeArea) / 2
            maskHeight = qrCodeArea
            maskPositionX = maskWidth * 1.5 + qrCodeArea
            maskPositionY = geometry.size.height / 2
            break
        }
        
        return AnyView(self.renderMask(width: maskWidth,
                                       height: maskHeight,
                                       positionX: maskPositionX,
                                       positionY: maskPositionY,
                                       hasSwitchCamera: side == .bottom,
                                       label: side == .top ? "text_qrcode_message" : (side == .bottom ? stateModel.message : ""),
                                       labelSize: side == .bottom ? 12 : nil,
                                       info: side == .bottom && DelegateManager.shared.isQRCodeListRefreshable() ? "text_qrcode_list_tap_to_refresh" : nil,
                                       infoSize: side == .bottom ? 11 : nil,
                                       qrCode: side == .bottom ? stateModel.qrCode : "",
                                       qrCodeSize: side == .bottom ? 11 : nil,
                                       onTap: side == .bottom && DelegateManager.shared.isQRCodeListRefreshable() ? {
                                        ConfigurationManager.shared.refreshList()
                                       } : nil))
    }
    
    private func renderMask(width: CGFloat, height: CGFloat, positionX: CGFloat, positionY: CGFloat, hasSwitchCamera: Bool, label: String, labelSize: CGFloat?, info: String?, infoSize: CGFloat?, qrCode: String?, qrCodeSize: CGFloat?, onTap: (() -> Void)?) -> some View {
        return VStack {
            if (hasSwitchCamera) {
                Image("cameraSwitch", bundle: Bundle(for: PassFlowController.self)).resizable()
                    .frame(width: 32, height: 32, alignment: .center).padding(.bottom, 16).onTapGesture {
                        DelegateManager.shared.qrCodeScannerSwitchCamera()
                    }
            }
            
            if label.count > 0 {
                Text(label.localized(locale.identifier)).padding(.horizontal, 16).foregroundColor(.white).font(labelSize != nil ? .system(size: labelSize!).bold() : .headline).multilineTextAlignment(.center).onTapGesture {
                    onTap?()
                }
                
                if (info != nil) {
                    Text(info!.localized(locale.identifier)).padding(.horizontal, 16).padding(.top, 2).foregroundColor(.white).font(infoSize != nil ? .system(size: infoSize!) : .headline).multilineTextAlignment(.center).onTapGesture {
                        onTap?()
                    }
                }
                
                if (qrCode != nil && !qrCode!.isEmpty) {
                    Text(qrCode!).padding(.horizontal, 16).padding(.top, 2).foregroundColor(.white).font(qrCodeSize != nil ? .system(size: qrCodeSize!) : .headline).multilineTextAlignment(.center).onTapGesture {
                        onTap?()
                    }
                }
            }
        }
        .frame(minWidth: 0, idealWidth: width, maxWidth: width, minHeight: 0, idealHeight: height, maxHeight: height, alignment: .center)
        .background(stateModel.backgroundColor.opacity(0.6))
        .position(x: positionX, y: positionY)
    }
    
    

}

@available(iOS 13.0, *)
struct ScanQRCodeView_Previews: PreviewProvider {
    static var previews: some View {
        ScanQRCodeView()
    }
}
