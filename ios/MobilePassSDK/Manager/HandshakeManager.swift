//
//  HandshakeManager.swift
//  MobilePassSDK
//
//  Manages challenge/handshake flow with backend for ephemeral key registration
//

import Foundation

final class HandshakeManager {
    
    // MARK: - Singleton
    
    static let shared = HandshakeManager()
    private init() {}
    
    // MARK: - Properties
    
    private let lock = NSLock()
    private var isHandshaking = false
    private var pendingCompletions: [(Result<Void, Error>) -> Void] = []
    private var cachedStaticKey: Data?
    
    // MARK: - Public Methods
    
    func performHandshake(completion: @escaping (Result<Void, Error>) -> Void) {
        lock.lock()
        
        // If handshake in progress, queue this completion
        if isHandshaking {
            pendingCompletions.append(completion)
            lock.unlock()
            LogManager.shared.debug(message: "Handshake in progress, queued request (\(pendingCompletions.count) waiting)")
            return
        }
        
        isHandshaking = true
        pendingCompletions.append(completion)
        lock.unlock()
        
        LogManager.shared.info(message: "Starting handshake flow")
        
        do {
            let ephemeralKey = CryptoManager.shared.generateRandomBytes(count: 32)
            let ephemeralKeyBase64 = ephemeralKey.base64EncodedString()
            let staticKey = try getStaticKey()
            let memberId = ConfigurationManager.shared.getMemberId()
            let timestamp = Int64(Date().timeIntervalSince1970)
            let payloadToSign = "\(memberId)|\(timestamp)|\(ephemeralKeyBase64)"
            let signatureData = CryptoManager.shared.hmacSHA256(key: staticKey, message: payloadToSign)
            let signatureBase64 = signatureData.base64EncodedString()
            
            let request = RequestHandshake(
                memberId: memberId,
                timestamp: timestamp,
                ephemeralKey: ephemeralKeyBase64,
                signature: signatureBase64
            )
            
            LogManager.shared.debug(message: "Sending handshake request")
            
            BaseService.shared.requestPost(url: "api/v2/sdk/handshake", data: request.dictionary) { [weak self] (result: Result<ResponseHandshake?, RequestError>) in
                guard let self = self else { return }
                
                self.lock.lock()
                let completions = self.pendingCompletions
                self.pendingCompletions = []
                self.isHandshaking = false
                self.lock.unlock()
                
                let finalResult: Result<Void, Error>
                
                switch result {
                case .success(let response):
                    if let handshakeResponse = response, handshakeResponse.ok, let serverNonce = handshakeResponse.serverNonce {
                        EphemeralKeyManager.shared.setEphemeralKey(ephemeralKey, serverNonce: serverNonce, ttl: handshakeResponse.ephemeralTTL)
                        
                        LogManager.shared.info(message: "Handshake successful (TTL: \(handshakeResponse.ephemeralTTL ?? 120)s, \(completions.count) queued requests)")
                        finalResult = .success(())
                    } else {
                        let errorMsg = response?.error ?? "Unknown error"
                        LogManager.shared.error(message: "Handshake failed: \(errorMsg)")
                        finalResult = .failure(HandshakeError.handshakeFailed(errorMsg))
                    }
                    
                case .failure(let error):
                    LogManager.shared.error(message: "Handshake request failed: \(error)")
                    finalResult = .failure(error)
                }
                
                completions.forEach { $0(finalResult) }
            }
            
        } catch {
            lock.lock()
            let completions = pendingCompletions
            pendingCompletions = []
            isHandshaking = false
            lock.unlock()
            
            LogManager.shared.error(message: "Handshake preparation failed: \(error.localizedDescription)")
            completions.forEach { $0(.failure(error)) }
        }
    }
    
    func ensureHandshake(completion: @escaping (Result<Void, Error>) -> Void) {
        if EphemeralKeyManager.shared.needsRenewal() {
            performHandshake(completion: completion)
        } else {
            completion(.success(()))
        }
    }
    
    func getStaticKey() throws -> Data {
        if let cached = cachedStaticKey {
            return cached
        }
        
        guard let apiKey = ConfigurationManager.shared.getApiKey(),
              let staticKey = apiKey.data(using: .utf8) else {
            LogManager.shared.error(message: "API key not available in configuration")
            throw HandshakeError.missingApiKey
        }
        
        cachedStaticKey = staticKey
        
        return staticKey
    }
    
    func clearCache() {
        lock.lock()
        defer { lock.unlock() }
        
        if var key = cachedStaticKey {
            key.withUnsafeMutableBytes { bytes in
                memset(bytes.baseAddress, 0, bytes.count)
            }
        }
        cachedStaticKey = nil
        
        LogManager.shared.debug(message: "HandshakeManager cache cleared")
    }
}

// MARK: - Request Signing

extension HandshakeManager {
    
    func signRequest(method: String, path: String, timestamp: Int64, bodyHash: String) -> String? {
        do {
            guard let ephemeralKey = EphemeralKeyManager.shared.getEphemeralKey() else {
                LogManager.shared.warn(message: "Cannot sign request: ephemeral key not available")
                return nil
            }
            
            guard let serverNonce = EphemeralKeyManager.shared.getServerNonce() else {
                LogManager.shared.warn(message: "Cannot sign request: server nonce not available")
                return nil
            }
            
            let staticKey = try getStaticKey()
            let combinedKey = CryptoManager.shared.hmacSHA256(key: staticKey, data: ephemeralKey)
            let canonicalString = "\(method)|\(path)|\(timestamp)|\(serverNonce)|\(bodyHash)"
            let signatureData = CryptoManager.shared.hmacSHA256(key: combinedKey, message: canonicalString)
            let signatureBase64 = signatureData.base64EncodedString()
            
            return signatureBase64            
        } catch {
            LogManager.shared.error(message: "Request signing failed: \(error.localizedDescription)")
            return nil
        }
    }
}

// MARK: - Error Types

enum HandshakeError: LocalizedError {
    case missingApiKey
    case handshakeFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .missingApiKey:
            return "API key not configured"
        case .handshakeFailed(let message):
            return "Handshake failed: \(message)"
        }
    }
}
