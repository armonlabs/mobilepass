//
//  BaseService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation
import UIKit

class BaseService: NSObject {
    
    // MARK: Singleton
    
    static let shared = BaseService()
    private override init() {
        super.init()
    }
    
    // MARK: - User-Agent
    
    private lazy var cachedUserAgent: String = {
        let sdkVersion = LogManager.shared.getVersion()
        var osVersion = ProcessInfo.processInfo.operatingSystemVersionString
            .replacingOccurrences(of: "Version ", with: "")
            .replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        var deviceModel = UIDevice.current.model.replacingOccurrences(of: "\n", with: " ")
        var locale = Locale.current.identifier
        
        osVersion = osVersion.components(separatedBy: .controlCharacters).joined().trimmingCharacters(in: .whitespaces)
        deviceModel = deviceModel.components(separatedBy: .controlCharacters).joined().trimmingCharacters(in: .whitespaces)
        locale = locale.components(separatedBy: .controlCharacters).joined().trimmingCharacters(in: .whitespaces)
        
        if osVersion.isEmpty { osVersion = "Unknown" }
        if deviceModel.isEmpty { deviceModel = "Unknown" }
        if locale.isEmpty { locale = "en_US" }
        
        return "MobilePassSDK/\(sdkVersion) (iOS \(osVersion); \(deviceModel); \(locale))"
    }()
    
    // MARK: Public Functions
    
    func requestGet<T: Decodable>(url: String, completion: @escaping (Result<T?, RequestError>) -> Void) {
        LogManager.shared.debug(message: "New GET request to \(url)")
        
        if !url.contains("sdk/handshake") {
            ensureHandshakeAndRequest(url: url, body: nil, completion: completion)
        } else {
            let urlRequest: URLRequest? = createRequest(url: url, body: nil, completion: completion)
            
            if (urlRequest != nil) {
                request(request: urlRequest!, completion: completion)
            } else {
                LogManager.shared.debug(message: "GET request to \(url) is cancelled")
            }
        }
    }
    
    func requestPost<T: Decodable>(url: String, data: [String: Any], completion: @escaping (Result<T?, RequestError>) -> Void) {
        LogManager.shared.debug(message: "New POST request to \(url)")
        
        if !url.contains("sdk/handshake") {
            ensureHandshakeAndRequest(url: url, body: data, completion: completion)
        } else {
            let urlRequest: URLRequest? = createRequest(url: url, body: data, completion: completion)
            
            if (urlRequest != nil) {
                request(request: urlRequest!, completion: completion)
            } else {
                LogManager.shared.debug(message: "POST request to \(url) is cancelled")
            }
        }
    }
    
    private func ensureHandshakeAndRequest<T: Decodable>(url: String, body: [String: Any]?, completion: @escaping (Result<T?, RequestError>) -> Void) {
        HandshakeManager.shared.ensureHandshake { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success():
                let urlRequest: URLRequest? = self.createRequest(url: url, body: body, completion: completion)
                
                if (urlRequest != nil) {
                    self.request(request: urlRequest!, completion: completion)
                } else {
                    LogManager.shared.debug(message: "Request to \(url) is cancelled")
                }
                
            case .failure(let error):
                LogManager.shared.error(message: "Handshake failed before request: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    completion(.failure(RequestError(message: "Authentication failed", reason: .other, code: 403)))
                }
            }
        }
    }
    
    // MARK: Private Functions
    
    private func createUrl<T: Decodable>(url: String, completion: @escaping (Result<T?, RequestError>) -> Void) -> URL {
        let serverAddress: String? = ConfigurationManager.shared.getServerURL()
        
        if (serverAddress == nil || serverAddress?.count == 0) {
            LogManager.shared.info(message: "Server address is empty, request is cancelled")
            completion(.failure(RequestError(message: "", reason: .invalidServer, code: nil)))
            return URL(string: "")!
        }
        
        LogManager.shared.debug(message: "Request will be sent to \(serverAddress!)")
        
        return URL(string: "\(serverAddress!)\(url)")!
    }
    
    private func createRequest<T: Decodable>(url: String, body: [String: Any]?, completion: @escaping (Result<T?, RequestError>) -> Void) -> URLRequest? {
        var request = URLRequest(url: createUrl(url: url, completion: completion))
        request.httpMethod = body == nil ? "GET" : "POST"
        
        var jsonData: Data?
        if (body != nil) {
            if #available(iOS 13.0, *) {
                jsonData = try? JSONSerialization.data(withJSONObject: body!, options: [.withoutEscapingSlashes])
            } else {
                jsonData = try? JSONSerialization.data(withJSONObject: body!, options: [])
            }
            request.httpBody = jsonData
        }
        
        request.timeoutInterval = Double(30)
        
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Mon, 26 Jul 1997 05:00:00 GMT", forHTTPHeaderField: "If-Modified-Since")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.setValue(cachedUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("\(ConfigurationManager.shared.getLanguage())", forHTTPHeaderField: "accept-language")
        request.setValue("\(LogManager.shared.getVersion())", forHTTPHeaderField: "mobilepass-version")
        request.setValue("\(ConfigurationManager.shared.getMemberId())", forHTTPHeaderField: "mobilepass-memberid")
        request.setValue("\(ConfigurationManager.shared.getBarcodeId())", forHTTPHeaderField: "mobilepass-barcode")
        request.setValue("\(ConfigurationManager.shared.getConfigurationLog())", forHTTPHeaderField: "mobilepass-config")
        
        if !url.contains("sdk/handshake") {
            if !addSignatureHeaders(to: &request, url: url, jsonData: jsonData) {
                LogManager.shared.error(message: "Request cancelled: signature generation failed")
                completion(.failure(RequestError(message: "Authentication required", reason: .other, code: 403)))
                return nil
            }
        }
        
        return request
    }
    
    private func addSignatureHeaders(to request: inout URLRequest, url: String, jsonData: Data?) -> Bool {
        let timestamp = Int64(Date().timeIntervalSince1970)
        
        var bodyHash = ""
        if let jsonData = jsonData {
            bodyHash = CryptoManager.shared.sha256Hex(jsonData)
        } else {
            bodyHash = CryptoManager.shared.sha256Hex("")
        }
        
        let method = request.httpMethod ?? "GET"
        var path = url
        
        if path.hasPrefix("api/v1/") {
            path = String(path.dropFirst(7))
        } else if path.hasPrefix("api/v2/") {
            path = String(path.dropFirst(7))
        }
        
        guard let signature = HandshakeManager.shared.signRequest(
            method: method,
            path: path,
            timestamp: timestamp,
            bodyHash: bodyHash
        ) else {
            LogManager.shared.error(message: "Cannot sign request - ephemeral key not available")
            return false
        }
        
        request.setValue("\(timestamp)", forHTTPHeaderField: "mobilepass-timestamp")
        request.setValue(signature, forHTTPHeaderField: "mobilepass-signature")
        
        LogManager.shared.debug(message: "Request signed: \(method) \(path)")
        return true
    }
    
    private func request<T: Decodable>(request: URLRequest, completion: @escaping (Result<T?, RequestError>) -> Void) {
        let task = URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let self = self else { return }
            
            let response = response as? HTTPURLResponse
            let statusCode = response?.statusCode
            
            guard let data = data, error == nil, statusCode != nil else {
                LogManager.shared.debug(message: "Request failed: \(error?.localizedDescription ?? "No data")")
                DispatchQueue.main.async {
                    completion(.failure(RequestError(message: "", reason: .other, code: statusCode ?? 0)))
                }
                return
            }
            
            if (statusCode != 200) {
                var message = "";

                let responseMsg: ResponseMessage? = self.getResponse(fromData: data)
                message = responseMsg?.message ?? ""
                
                LogManager.shared.debug(message: "Request completed with message: \(message)")
                LogManager.shared.debug(message: "Request completed with status code: \(statusCode!)")
                
                DispatchQueue.main.async {
                    completion(.failure(RequestError(message: message, reason: .errorCode, code: statusCode!)))
                }
                return
            }
            
            LogManager.shared.info(message: "Request completed successfully")
            
            let state: T? = self.getResponse(fromData: data)
            
            DispatchQueue.main.async {
                if (state == nil) {
                    LogManager.shared.debug(message: "Response data is empty")
                    completion(.failure(RequestError(message: "", reason: .other, code: -1)))
                } else {
                    LogManager.shared.debug(message: "Response data is valid")
                    completion(.success(state!))
                }
            }
        }
        
        task.resume()
    }
    
    private func getResponse<T: Decodable>(fromData data: Data) -> T? {
        let stateData = try? JSONDecoder().decode(T.self, from: data)
        
        do {
            let decoder = JSONDecoder()
            let _ = try decoder.decode(T.self, from: data) // messages
            LogManager.shared.debug(message: "Parse response succeed")
        } catch DecodingError.dataCorrupted(let context) {
            LogManager.shared.debug(message: "Data corrupted: " + context.debugDescription)
        } catch DecodingError.keyNotFound(let key, let context) {
            LogManager.shared.debug(message: "Key '\(key)' not found: " + context.debugDescription)
            LogManager.shared.debug(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch DecodingError.valueNotFound(let value, let context) {
            LogManager.shared.debug(message: "Value '\(value)' not found: " + context.debugDescription)
            LogManager.shared.debug(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch DecodingError.typeMismatch(let type, let context) {
            LogManager.shared.debug(message: "Type '\(type)' mismatch: " + context.debugDescription)
            LogManager.shared.debug(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch {
            LogManager.shared.debug(message: "Parse response failed: " + error.localizedDescription)
        }
        
        if let result = stateData {
            return result
        }
        
        return nil
    }
}
