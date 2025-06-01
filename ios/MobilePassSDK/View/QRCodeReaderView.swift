//
//  QRCodeReaderView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 13.02.2021.
//

import SwiftUI
import AVFoundation

enum ScanResult: Int {
    case success                    = 1
    case addInputFailed             = 2
    case addOutputFailed            = 3
    case noMatching                 = 4
    case invalidFormat              = 5
    case invalidContent             = 6
    case missingInput               = 7
    case missingSession             = 8
    case getCaptureDeviceFailed     = 9
    case createVideoInputFailed     = 10
    case createPreviewLayerFailed   = 11
    case startSessionFailed         = 12
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
                        PassFlowManager.shared.setQRData(qrId: activeQRCodeContent?.qrCode?.i, clubId: activeQRCodeContent?.clubInfo?.i)
                        
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
        setupCamera()
    }

    private func setupCamera(retryCount: Int = 0) {
        captureSession = AVCaptureSession()

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            LogManager.shared.error(message: "Failed to get video capture device", code: LogCodes.UI_CAMERA_SETUP_FAILED)
            if retryCount < 3 {
                LogManager.shared.info(message: "Retrying camera setup... Attempt \(retryCount + 1)")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.setupCamera(retryCount: retryCount + 1)
                }
            } else {
                delegate?.didFail(result: .getCaptureDeviceFailed)
            }
            return
        }
        
        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            LogManager.shared.error(message: "Failed to create video input: \(error.localizedDescription)", code: LogCodes.UI_CAMERA_SETUP_FAILED)
            if retryCount < 3 {
                LogManager.shared.info(message: "Retrying camera setup... Attempt \(retryCount + 1)")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.setupCamera(retryCount: retryCount + 1)
                }
            } else {
                delegate?.didFail(result: .createVideoInputFailed)
            }
            return
        }

        if (captureSession != nil && captureSession!.canAddInput(videoInput)) {
            captureSession!.addInput(videoInput)
        } else {
            LogManager.shared.error(message: "Failed to add video input to capture session", code: LogCodes.UI_CAMERA_SETUP_FAILED)
            if retryCount < 3 {
                LogManager.shared.info(message: "Retrying camera setup... Attempt \(retryCount + 1)")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.setupCamera(retryCount: retryCount + 1)
                }
            } else {
                delegate?.didFail(result: .addInputFailed)
            }
            return
        }

        let metadataOutput = AVCaptureMetadataOutput()

        if (captureSession != nil && captureSession!.canAddOutput(metadataOutput)) {
            captureSession!.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(delegate, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            LogManager.shared.error(message: "Failed to add metadata output to capture session", code: LogCodes.UI_CAMERA_SETUP_FAILED)
            if retryCount < 3 {
                LogManager.shared.info(message: "Retrying camera setup... Attempt \(retryCount + 1)")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.setupCamera(retryCount: retryCount + 1)
                }
            } else {
                DispatchQueue.main.async {
                    self.delegate?.didFail(result: .addOutputFailed)
                }
            }
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
            LogManager.shared.error(message: "Capture session is nil", code: LogCodes.UI_CAMERA_SETUP_FAILED)
            delegate?.didFail(result: .missingSession)
            return
        }
        
        if previewLayer == nil {
            previewLayer = AVCaptureVideoPreviewLayer(session: captureSession!)
            if previewLayer == nil {
                LogManager.shared.error(message: "Failed to create preview layer", code: LogCodes.UI_CAMERA_SETUP_FAILED)
                delegate?.didFail(result: .createPreviewLayerFailed)
                return
            }
        }
        
        previewLayer!.frame = view.layer.bounds
        previewLayer!.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer!)

        if (captureSession!.isRunning == false) {
            LogManager.shared.debug(message: "Starting QR Code capture session")
            
            if (captureSession!.inputs.count > 0) {
                // Check if camera is available
                if AVCaptureDevice.authorizationStatus(for: .video) == .authorized {
                    DispatchQueue.global(qos: .userInitiated).async {
                        self.captureSession!.startRunning()
                        if !self.captureSession!.isRunning {
                            LogManager.shared.error(message: "Failed to start capture session", code: LogCodes.UI_CAMERA_SETUP_FAILED)
                            DispatchQueue.main.async {
                                self.delegate?.didFail(result: .startSessionFailed)
                            }
                        }
                    }
                } else {
                    LogManager.shared.error(message: "Camera not authorized", code: LogCodes.UI_CAMERA_SETUP_FAILED)
                    DispatchQueue.main.async {
                        self.delegate?.didFail(result: .startSessionFailed)
                    }
                }
            } else {
                LogManager.shared.error(message: "No inputs available for capture session", code: LogCodes.UI_CAMERA_SETUP_FAILED)
                DispatchQueue.main.async {
                    self.delegate?.didFail(result: .missingInput)
                }
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
