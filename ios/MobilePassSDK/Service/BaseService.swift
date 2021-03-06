//
//  BaseService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class BaseService: NSObject {
    
    // MARK: Singleton
    
    static let shared = BaseService()
    private override init() {
        super.init()
    }
    
    // MARK: Public Functions
    
    func requestGet<T: Decodable>(url: String, completion: @escaping (Result<T?, RequestError>) -> Void) {
        LogManager.shared.debug(message: "New GET request to \(url)")
        let urlRequest: URLRequest? = createRequest(url: url, body: nil, completion: completion)
        
        if (urlRequest != nil) {
            request(request: urlRequest!, completion: completion)
        } else {
            LogManager.shared.debug(message: "GET request to \(url) is cancelled")
        }
    }
    
    func requestPost<T: Decodable>(url: String, data: [String: Any], completion: @escaping (Result<T?, RequestError>) -> Void) {
        LogManager.shared.debug(message: "New POST request to \(url)")
        let urlRequest: URLRequest? = createRequest(url: url, body: data, completion: completion)
        
        if (urlRequest != nil) {
            request(request: urlRequest!, completion: completion)
        } else {
            LogManager.shared.debug(message: "POST request to \(url) is cancelled")
        }
    }
    
    // MARK: Private Functions
    
    private func createUrl<T: Decodable>(url: String, completion: @escaping (Result<T?, RequestError>) -> Void) -> URL {
        let serverAddress: String? = ConfigurationManager.shared.getServerURL()
        
        if (serverAddress == nil || serverAddress?.count == 0) {
            LogManager.shared.warn(message: "Server address is empty, request is cancelled")
            completion(.failure(RequestError(message: "Server address cannot be found!", reason: .invalidServer, code: nil)))
            return URL(string: "")!
        }
        
        LogManager.shared.debug(message: "Request will be sent to \(serverAddress!)")
        
        return URL(string: "\(serverAddress!)\(url)")!
    }
    
    private func createRequest<T: Decodable>(url: String, body: [String: Any]?, completion: @escaping (Result<T?, RequestError>) -> Void) -> URLRequest? {
        var request = URLRequest(url: createUrl(url: url, completion: completion))
        request.httpMethod = body == nil ? "GET" : "POST"
        
        if (body != nil) {
            let jsonData = try? JSONSerialization.data(withJSONObject: body!)
            request.httpBody = jsonData
        }
        
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("\(ConfigurationManager.shared.getToken())", forHTTPHeaderField: "Authorization")
        
        return request
    }
    
    private func request<T: Decodable>(request: URLRequest, completion: @escaping (Result<T?, RequestError>) -> Void) {
        URLSession.shared.dataTask(with: request) { data, response, error in
            let response = response as? HTTPURLResponse
            let statusCode = response?.statusCode
            
            guard let data = data, error == nil, statusCode != nil else {
                LogManager.shared.info(message: "Request failed: \(error?.localizedDescription ?? "No data")")
                completion(.failure(RequestError(message: error!.localizedDescription, reason: .other, code: statusCode ?? 0)))
                return
            }
            
            if (statusCode != 200) {
                LogManager.shared.info(message: "Request completed with status code: \(statusCode!)")
                completion(.failure(RequestError(message: "Request failed with status code: \(statusCode!)", reason: .other, code: statusCode!)))
                return
            }
            
            LogManager.shared.info(message: "Request completed successfully")
            
            let state: T? = self.getResponse(fromData: data)
            
            if (state == nil) {
                LogManager.shared.info(message: "Response data is empty")
                completion(.success(nil))
            } else {
                LogManager.shared.info(message: "Response data is valid")
                completion(.success(state!))
            }
        }.resume()
    }
    
    private func getResponse<T: Decodable>(fromData data: Data) -> T? {
        let stateData = try? JSONDecoder().decode(T.self, from: data)
        
        do {
            let decoder = JSONDecoder()
            let _ = try decoder.decode(T.self, from: data) // messages
            LogManager.shared.info(message: "Parse response succeed")
        } catch DecodingError.dataCorrupted(let context) {
            LogManager.shared.info(message: "Data corrupted: " + context.debugDescription)
        } catch DecodingError.keyNotFound(let key, let context) {
            LogManager.shared.info(message: "Key '\(key)' not found: " + context.debugDescription)
            LogManager.shared.info(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch DecodingError.valueNotFound(let value, let context) {
            LogManager.shared.info(message: "Value '\(value)' not found: " + context.debugDescription)
            LogManager.shared.info(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch DecodingError.typeMismatch(let type, let context) {
            LogManager.shared.info(message: "Type '\(type)' mismatch: " + context.debugDescription)
            LogManager.shared.info(message: "CodingPath: " + context.codingPath.debugDescription)
        } catch {
            LogManager.shared.info(message: "Parse response failed: " + error.localizedDescription)
        }
        
        if let result = stateData {
            return result
        }
        
        return nil
    }
}
