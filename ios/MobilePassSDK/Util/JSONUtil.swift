//
//  JSONUtil.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class JSONUtil: NSObject {
    
    // MARK: Singleton
    
    static let shared = JSONUtil()
    private override init() {
        super.init()
    }
        
    // MARK: Public Functions
    
    public func encodeJSONArray<T: Encodable>(data: T) throws -> String {
        let jsonData     = try JSONEncoder().encode(data)
        let jsonString   = String(data: jsonData, encoding: .utf8)!
        
        return jsonString
    }
    
    public func decodeJSONArray<T: Decodable>(jsonString: String) throws -> [T]  {
        let jsonData = jsonString.data(using: .utf8)!
        return try JSONDecoder().decode([T].self, from: jsonData)
    }
    
    public func encodeJSONData<T: Encodable>(data: T) throws -> String {
        let jsonData     = try JSONEncoder().encode(data)
        let jsonString   = String(data: jsonData, encoding: .utf8)!
        
        return jsonString
    }
    
    public func decodeJSONData<T: Decodable>(jsonString: String) throws -> T  {
        let jsonData = jsonString.data(using: .utf8)!
        return try JSONDecoder().decode(T.self, from: jsonData)
    }
    
    public func arrayToJSON(array: [Any]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: array, options: []) else {
            return ""
        }
        return String(data: data, encoding: String.Encoding.utf8) ?? ""
    }
}
