//
//  RequestHandshake.swift
//  MobilePassSDK
//
//  Request model for handshake (ephemeral key registration)
//

import Foundation

struct RequestHandshake: Codable {
    /// Member ID from configuration
    let memberId: String
    
    /// Current timestamp (Unix seconds)
    let timestamp: Int64
    
    /// Ephemeral key (base64 encoded, 32 bytes)
    let ephemeralKey: String
    
    /// HMAC signature of the handshake payload
    let signature: String
    
    var dictionary: [String: Any] {
        return [
            "memberId": memberId,
            "timestamp": timestamp,
            "ephemeralKey": ephemeralKey,
            "signature": signature
        ]
    }
}
