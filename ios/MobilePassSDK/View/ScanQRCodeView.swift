//
//  ScanQRCodeView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 13.02.2021.
//

import SwiftUI
import AVFoundation

struct ScanQRCodeView: View {
    @Environment(\.locale) var locale
    
    fileprivate var delegate: PassFlowDelegate?
    
    private enum MaskSide {
        case top, bottom, left, right
    }
    
    init(delegate: PassFlowDelegate?) {
        self.delegate = delegate
        
        if (AVCaptureDevice.authorizationStatus(for: .video) != .authorized) {
            self.delegate?.needPermissionCamera()
        }
    }
    
    var body: some View {
        ZStack {
            QRCodeReaderView(
                completion: { result in
                    if case let .success(code) = result {
                        delegate?.onQRCodeFound(code: code)
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
                                       label: side == .top ? "text_qrcode_message" : ""))
    }
    
    private func renderMask(width: CGFloat, height: CGFloat, positionX: CGFloat, positionY: CGFloat, label: String) -> some View {
        return VStack {
            if label.count > 0 {
                Text(label.localized(locale.identifier)).padding(.horizontal, 16).foregroundColor(.white).font(.headline).multilineTextAlignment(.center)
            }
        }
        .frame(minWidth: 0, idealWidth: width, maxWidth: width, minHeight: 0, idealHeight: height, maxHeight: height, alignment: .center)
        .background(Color.black.opacity(0.6))
        .position(x: positionX, y: positionY)
    }
}

struct ScanQRCodeView_Previews: PreviewProvider {
    static var previews: some View {
        ScanQRCodeView(delegate: nil)
    }
}
