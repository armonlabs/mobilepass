//
//  QRCodeReaderView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 13.02.2021.
//

import SwiftUI
import AVFoundation

enum ScanResult: Int {
    case success            = 1
    case addInputFailed     = 2
    case addOutputFailed    = 3
    case noMatching         = 4
    case invalidFormat      = 5
    case invalidContent     = 6
    case missingInput       = 7
    case missionSession     = 8
}

public struct QRCodeScanResult {
    var code:   String
    var result: ScanResult
    
    init(code: String, result: ScanResult) {
        self.code   = code
        self.result = result
    }
}

@available(iOS 13.0, *)
public struct QRCodeReaderView: UIViewControllerRepresentable {
    private let configScanInterval: Double = 2.0
    public var completion: (QRCodeScanResult) -> Void

    public init(completion: @escaping (QRCodeScanResult) -> Void) {
        self.completion = completion
    }

    public func makeCoordinator() -> QRCodeReaderCoordinator {
        return QRCodeReaderCoordinator(parent: self)
    }

    public func makeUIViewController(context: Context) -> QRCodeReaderViewController {
        let viewController = QRCodeReaderViewController()
        viewController.delegate = context.coordinator
        return viewController
    }

    public func updateUIViewController(_ uiViewController: QRCodeReaderViewController, context: Context) {

    }
}

@available(iOS 13.0, *)
struct QRCodeReaderView_Previews: PreviewProvider {
    static var previews: some View {
        QRCodeReaderView() { result in
            // do nothing
        }
    }
}

@available(iOS 13.0, *)
public class QRCodeReaderCoordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
    var parent: QRCodeReaderView
    var isFound = false
    var foundQRCodes: Dictionary<String, TimeInterval> = [:]
    let scanInterval: Double = 2.0

    init(parent: QRCodeReaderView) {
        self.parent = parent
    }

    public func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        if let metadataObject = metadataObjects.first {
            guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
            guard let stringValue = readableObject.stringValue else { return }
            
            if (foundQRCodes.index(forKey: stringValue) != nil && foundQRCodes[stringValue] != nil && Date().timeIntervalSince1970 - foundQRCodes[stringValue]! < 2.5) {
                return
            }
            
            guard isFound == false else { return }
            
            foundQRCodes[stringValue] = Date().timeIntervalSince1970;
            
            let pattern = "https://(app|sdk).armongate.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2]{1})?()?$"
            let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive)

            if let match = regex?.firstMatch(in: stringValue, options: [], range: NSRange(location: 0, length: stringValue.utf16.count)) {
                // Set found flag
                isFound = true
                
                var prefix:     Substring = ""
                var uuid:       Substring = ""
                var direction:  Substring = ""
                
                if let prefixRange = Range(match.range(at: 2), in: stringValue) {
                    prefix = stringValue[prefixRange]
                }

                if let uuidRange = Range(match.range(at: 3), in: stringValue) {
                    uuid = stringValue[uuidRange]
                }
                
                if let directionRange = Range(match.range(at: 4), in: stringValue) {
                    direction = stringValue[directionRange]
                }
                
                let qrCodeContent: String = "\(prefix)/\(uuid)\(direction)"

                let activeQRCodeContent = ConfigurationManager.shared.getQRCodeContent(qrCodeData: qrCodeContent)
                
                if (activeQRCodeContent == nil) {
                    PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_NO_MATCH, data: qrCodeContent)
                    
                    LogManager.shared.warn(message: "QR code reader could not find matching content for \(qrCodeContent)", code: LogCodes.PASSFLOW_QRCODE_READER_NO_MATCHING)
                    
                    isFound = false
                    parent.completion(QRCodeScanResult(code: stringValue, result: .noMatching))
                } else {
                    if (activeQRCodeContent!.valid) {
                        PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_FOUND, data: qrCodeContent)
                        
                        parent.completion(QRCodeScanResult(code: qrCodeContent, result: .success))
                    } else {
                        PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_INVALID_CONTENT, data: qrCodeContent)
                        LogManager.shared.warn(message: "QR code reader found content for \(qrCodeContent) but it is invalid", code: LogCodes.PASSFLOW_QRCODE_READER_INVALID_CONTENT)
                        
                        isFound = false
                        parent.completion(QRCodeScanResult(code: stringValue, result: .invalidContent))
                    }
                }
            } else {
                PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_INVALID_FORMAT, data: stringValue)
                LogManager.shared.warn(message: "QR code reader found unknown format > \(stringValue)", code: LogCodes.PASSFLOW_QRCODE_READER_INVALID_FORMAT)
                
                isFound = false
                parent.completion(QRCodeScanResult(code: stringValue, result: .invalidFormat))
            }
        }
    }

    func didFail(result: ScanResult) {
        PassFlowManager.shared.addToStates(state: .SCAN_QRCODE_ERROR, data: result.rawValue.description)
        parent.completion(QRCodeScanResult(code: "", result: result))
    }
}

@available(iOS 13.0, *)
public class QRCodeReaderViewController: UIViewController, QRCodeScannerDelegate {
    var captureSession: AVCaptureSession?
    var previewLayer: AVCaptureVideoPreviewLayer?
    var delegate: QRCodeReaderCoordinator?
    
    override public func viewDidLoad() {
        super.viewDidLoad()

        DelegateManager.shared.setQRCodeScannerDelegate(delegate: self)

        NotificationCenter.default.addObserver(self,
                                               selector: #selector(updateOrientation),
                                               name: Notification.Name("UIDeviceOrientationDidChangeNotification"),
                                               object: nil)

        view.backgroundColor = UIColor.black
        captureSession = AVCaptureSession()

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }
        
        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            return
        }

        if (captureSession != nil && captureSession!.canAddInput(videoInput)) {
            captureSession!.addInput(videoInput)
        } else {
            delegate?.didFail(result: .addInputFailed)
            return
        }

        let metadataOutput = AVCaptureMetadataOutput()

        if (captureSession != nil && captureSession!.canAddOutput(metadataOutput)) {
            captureSession!.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(delegate, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            delegate?.didFail(result: .addOutputFailed)
            return
        }
    }

    override public func viewWillLayoutSubviews() {
        if (previewLayer != nil) {
            previewLayer!.frame = view.layer.bounds
        }
    }

    @objc func updateOrientation() {
        if (captureSession != nil) {
            guard let orientation = UIApplication.shared.windows.first?.windowScene?.interfaceOrientation else { return }
            guard let connection = captureSession!.connections.last, connection.isVideoOrientationSupported else { return }
            connection.videoOrientation = AVCaptureVideoOrientation(rawValue: orientation.rawValue) ?? .portrait
        }
    }

    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        updateOrientation()
    }

    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        if (captureSession == nil) {
            delegate?.didFail(result: .missionSession)
            return
        }
        
        if previewLayer == nil {
            previewLayer = AVCaptureVideoPreviewLayer(session: captureSession!)
        }
        
        previewLayer!.frame = view.layer.bounds
        previewLayer!.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer!)

        if (captureSession!.isRunning == false) {
            LogManager.shared.debug(message: "Starting QR Code capture session")
            
            if (captureSession!.inputs.count > 0) {
                captureSession!.startRunning()
            } else {
                delegate?.didFail(result: .missingInput)
            }
        }
    }
    
    public override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        if (captureSession != nil && captureSession!.isRunning == true) {
            LogManager.shared.debug(message: "Stopping QR Code capture session")
            captureSession!.stopRunning()
        }

        NotificationCenter.default.removeObserver(self)
    }

    override public func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)

        /*
        if (captureSession?.isRunning == true) {
            captureSession.stopRunning()
        }

        NotificationCenter.default.removeObserver(self)
         */
    }
    
    override public var prefersStatusBarHidden: Bool {
        return true
    }

    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .all
    }
    
    public func onSwitchCamera() {
        if (captureSession == nil) {
            return
        }
        
        //Change camera source
        if let session = captureSession {
            
            //Remove existing input
            guard let currentCameraInput: AVCaptureInput = session.inputs.first else {
                return
            }

            //Indicate that some changes will be made to the session
            session.beginConfiguration()
            session.removeInput(currentCameraInput)

            //Get new input
            var newCamera: AVCaptureDevice? = nil
            if let input = currentCameraInput as? AVCaptureDeviceInput {
                if (input.device.position == .back) {
                    newCamera = cameraWithPosition(position: .front)
                } else {
                    newCamera = cameraWithPosition(position: .back)
                }
            }

            //Add input to session
            var err: NSError?
            var newVideoInput: AVCaptureDeviceInput?
            do {
                if (newCamera != nil) {
                    newVideoInput = try AVCaptureDeviceInput(device: newCamera!)
                }
            } catch let err1 as NSError {
                err = err1
                newVideoInput = nil
            }

            if newVideoInput == nil || err != nil {
                LogManager.shared.error(message: "Switch camera failed! Exception: \(err?.localizedDescription ?? "-")", code: LogCodes.UI_SWITCH_CAMERA_FAILED)
            } else {
                session.addInput(newVideoInput!)
            }

            //Commit all the configuration changes at once
            session.commitConfiguration()
        }
    }
    
    private func cameraWithPosition(position: AVCaptureDevice.Position) -> AVCaptureDevice? {
        let discoverySession = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInWideAngleCamera], mediaType: AVMediaType.video, position: .unspecified)
        for device in discoverySession.devices {
            if device.position == position {
                return device
            }
        }

        return nil
    }
}
