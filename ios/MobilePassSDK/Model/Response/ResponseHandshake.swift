//
//  ResponseHandshake.swift
//  MobilePassSDK
//
//  Response model for handshake completion
//

import Foundation

struct ResponseHandshake: Codable {
    /// Success indicator
    let ok: Bool
    
    /// Server-generated nonce (tied to ephemeral key session)
    let serverNonce: String?
    
    /// Ephemeral key TTL in seconds (typically 120)
    let ephemeralTTL: Int?
    
    /// Optional error message
    let error: String?
}
