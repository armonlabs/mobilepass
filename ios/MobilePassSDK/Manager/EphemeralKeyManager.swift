//
//  EphemeralKeyManager.swift
//  MobilePassSDK
//
//  Manages ephemeral key lifecycle for request signing
//

import Foundation

/// Manages ephemeral keys for secure request signing
/// Ephemeral keys are short-lived (2 minutes) and automatically renewed
final class EphemeralKeyManager {
    
    // MARK: - Singleton
    
    static let shared = EphemeralKeyManager()
    private init() {}
    
    // MARK: - Properties
    
    /// Current ephemeral key (32 bytes)
    private var ephemeralKey: Data?
    
    /// Server nonce from handshake (tied to ephemeral key)
    private var serverNonce: String?
    
    /// Timestamp when handshake was completed
    private var handshakeTimestamp: TimeInterval?
    
    /// Ephemeral key TTL in seconds (from server, default 120s)
    private var ephemeralTTL: TimeInterval = 120
    
    /// Thread safety lock
    private let lock = NSLock()
    
    // MARK: - Public Methods
    
    /// Gets the current ephemeral key
    /// - Returns: Current ephemeral key or nil if not set
    func getEphemeralKey() -> Data? {
        lock.lock()
        defer { lock.unlock() }
        return ephemeralKey
    }
    
    /// Gets the server nonce
    func getServerNonce() -> String? {
        lock.lock()
        defer { lock.unlock() }
        return serverNonce
    }
    
    /// Sets a new ephemeral key and records the timestamp
    /// - Parameters:
    ///   - key: New ephemeral key (should be 32 bytes)
    ///   - nonce: Server nonce from handshake
    ///   - ttl: TTL in seconds from server (optional, defaults to 120)
    func setEphemeralKey(_ key: Data, serverNonce nonce: String, ttl: Int? = nil) {
        lock.lock()
        defer { lock.unlock() }
        
        self.ephemeralKey = key
        self.serverNonce = nonce
        self.handshakeTimestamp = Date().timeIntervalSince1970
        if let ttl = ttl {
            self.ephemeralTTL = TimeInterval(ttl)
        }
        
        LogManager.shared.debug(message: "EphemeralKey set (TTL: \(Int(ephemeralTTL))s)")
    }
    
    /// Checks if ephemeral key needs renewal
    /// - Returns: true if key missing or remaining time < 25% of TTL
    func needsRenewal() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        
        guard let timestamp = handshakeTimestamp, ephemeralKey != nil else {
            return true
        }
        
        let remaining = ephemeralTTL - (Date().timeIntervalSince1970 - timestamp)
        let renewalThreshold = ephemeralTTL * 0.25 // Renew when 25% of TTL remains
        return remaining < renewalThreshold
    }
    
    /// Checks if ephemeral key is still valid (not expired)
    /// - Returns: true if key exists and hasn't expired
    func isValid() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        
        guard let timestamp = handshakeTimestamp, ephemeralKey != nil else {
            return false
        }
        
        return (Date().timeIntervalSince1970 - timestamp) < ephemeralTTL
    }
    
    /// Gets remaining lifetime of current ephemeral key
    /// - Returns: Remaining seconds, or 0 if invalid
    func getRemainingLifetime() -> TimeInterval {
        lock.lock()
        defer { lock.unlock() }
        
        guard let timestamp = handshakeTimestamp,
              ephemeralKey != nil else {
            return 0
        }
        
        let elapsed = Date().timeIntervalSince1970 - timestamp
        let remaining = ephemeralTTL - elapsed
        
        return max(0, remaining)
    }
    
    /// Clears ephemeral key and securely zeros memory
    func clear() {
        lock.lock()
        defer { lock.unlock() }
        
        // Securely zero out memory before releasing
        if var key = ephemeralKey {
            key.withUnsafeMutableBytes { bytes in
                memset(bytes.baseAddress, 0, bytes.count)
            }
        }
        
        ephemeralKey = nil
        serverNonce = nil
        handshakeTimestamp = nil
        
        LogManager.shared.debug(message: "EphemeralKey cleared")
    }
    
    /// Forces immediate renewal by clearing current key
    func forceRenewal() {
        LogManager.shared.info(message: "EphemeralKey force renewal triggered")
        clear()
    }
}

// MARK: - Lifecycle Management

extension EphemeralKeyManager {
    
    /// Should be called when app enters background
    func handleAppDidEnterBackground() {
        let remaining = getRemainingLifetime()
        let threshold = ephemeralTTL * 0.25
        
        // Clear if less than 25% of TTL remaining - will renew on foreground
        if remaining < threshold {
            LogManager.shared.debug(message: "App backgrounded with low ephemeral lifetime (\(Int(remaining))s), clearing")
            clear()
        } else {
            LogManager.shared.debug(message: "App backgrounded with \(Int(remaining))s ephemeral lifetime remaining, keeping")
        }
    }
    
    /// Should be called when app enters foreground
    func handleAppWillEnterForeground() {
        if needsRenewal() {
            LogManager.shared.info(message: "App foregrounded, ephemeral key renewal needed")
        }
    }
    
    /// Should be called when app will terminate
    func handleAppWillTerminate() {
        LogManager.shared.info(message: "App terminating, clearing ephemeral key")
        clear()
    }
}
