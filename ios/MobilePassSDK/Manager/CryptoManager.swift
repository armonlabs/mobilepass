//
//  CryptoManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 8.02.2021.
//

import Foundation
import CommonCrypto

class CryptoManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = CryptoManager()
    private override init() {
        super.init()
    }
    
    // MARK: Constants
    
    private let KEY_TAG_PUBLIC: String = "com.armongate.mobilepasssdk.public"
    private let KEY_TAG_PRIVATE: String = "com.armongate.mobilepasssdk.private"
    
    // MARK: Public Functions
    
    func generateKeyPair() -> CryptoKeyPair! {
        let keyTagPublic:   Data? = KEY_TAG_PUBLIC.data(using: .utf8)
        let keyTagPrivate:  Data? = KEY_TAG_PRIVATE.data(using: .utf8)
        
        let privateKeyAttributes = [
            String(kSecAttrIsPermanent):    true,
            String(kSecAttrApplicationTag): keyTagPrivate as Any,
            String(kSecAttrAccessible):     kSecAttrAccessibleAfterFirstUnlock
        ] as [String: Any]
        
        let publicKeyAttributes = [
            String(kSecAttrIsPermanent):    true,
            String(kSecAttrApplicationTag): keyTagPublic as Any,
            String(kSecAttrAccessible):     kSecAttrAccessibleAfterFirstUnlock
        ] as [String: Any]
        
        
        let keyPairAttributes = [
            String(kSecAttrKeyType):        kSecAttrKeyTypeECSECPrimeRandom,
            String(kSecAttrKeySizeInBits):  256,
            String(kSecPrivateKeyAttrs):    privateKeyAttributes,
            String(kSecPublicKeyAttrs):     publicKeyAttributes
        ] as [String: Any]
        
        var publicKey, privateKey: SecKey?;
        let status = SecKeyGeneratePair(keyPairAttributes as CFDictionary, &publicKey, &privateKey)
        
        if (status == noErr) {
            var error:Unmanaged<CFError>?
            if let publicKeyData = SecKeyCopyExternalRepresentation(publicKey!, &error) {
               let dataPubKey:Data = publicKeyData as Data
               let publicKeyBase64 = dataPubKey.base64EncodedString()
                
                if let privateKeyData = SecKeyCopyExternalRepresentation(privateKey!, &error) {
                    let dataPriKey:Data = privateKeyData as Data
                    let privateKeyBase64 = dataPriKey.base64EncodedString()
         
                    return CryptoKeyPair(publicKey: publicKeyBase64, privateKey: privateKeyBase64)
                } else {
                    LogManager.shared.debug(message: "Convert private key to base64 encoded string is failed!")
                }
            } else {
                LogManager.shared.debug(message: "Convert public key to base64 encoded string is failed!")
            }
        } else {
            LogManager.shared.debug(message: "Generate key pair failed!")
        }
        
        return nil;
    }
    
    func getSecret(privateKey: String, publicKey: String) -> Data? {
        let privateKeyRef:  SecKey? = getPrivateKey(keyBase64: privateKey)
        let publicKeyRef:   SecKey? = getPublicKey(keyBase64: publicKey)
        
        if (privateKeyRef == nil || publicKeyRef == nil) {
            return nil
        }
        
        let dict: [String: Any] = [:]
        
        var error:Unmanaged<CFError>?
        let secretKeyDataRef: CFData? = SecKeyCopyKeyExchangeResult(privateKeyRef!, SecKeyAlgorithm.ecdhKeyExchangeStandard, publicKeyRef!, dict as CFDictionary, &error);
        
        if (error != nil) {
            return nil;
        } else {
            return secretKeyDataRef as Data?
        }
    }
    
    func encodeWithSHA256(plainData: Data) -> Data {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        plainData.withUnsafeBytes({
            _ = CC_SHA256($0, CC_LONG(plainData.count), &hash)
        })
        
        return Data(bytes: hash)
    }
    
    func encodeWithSHA(key: Data, iv: Data, data: Data) throws -> Data? {
        
            var outLength = Int(0)
            var outBytes = [UInt8](repeating: 0, count: data.count + kCCBlockSizeAES128)
            var status: CCCryptorStatus = CCCryptorStatus(kCCSuccess)
        
            data.withUnsafeBytes { rawBufferPointer in
                let encryptedBytes = rawBufferPointer.baseAddress!
                
                iv.withUnsafeBytes { rawBufferPointer in
                    let ivBytes = rawBufferPointer.baseAddress!
                    
                    key.withUnsafeBytes { rawBufferPointer in
                        let keyBytes = rawBufferPointer.baseAddress!
                        
                        status = CCCrypt(CCOperation(kCCEncrypt),
                                         CCAlgorithm(kCCAlgorithmAES),            // algorithm
                                         CCOptions(kCCOptionPKCS7Padding),           // options
                                         keyBytes,                                   // key
                                         key.count,                                  // keylength
                                         ivBytes,                                    // iv
                                         encryptedBytes,                             // dataIn
                                         data.count,                                 // dataInLength
                                         &outBytes,                                  // dataOut
                                         outBytes.count,                             // dataOutAvailable
                                         &outLength)                                 // dataOutMoved
                    }
                }
            }
            
            guard status == kCCSuccess else {
                return nil
            }
                    
            return Data(bytes: outBytes, count: outLength)
    }
    
    func hmacSHA256(key: Data, message: String) -> Data {
        guard let messageData = message.data(using: .utf8) else {
            LogManager.shared.error(message: "Failed to convert message to data for HMAC")
            return Data()
        }
        return hmacSHA256(key: key, data: messageData)
    }
    
    func hmacSHA256(key: Data, data: Data) -> Data {
        var hmac = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        key.withUnsafeBytes { keyBytes in
            data.withUnsafeBytes { dataBytes in
                CCHmac(CCHmacAlgorithm(kCCHmacAlgSHA256),
                       keyBytes.baseAddress, key.count,
                       dataBytes.baseAddress, data.count,
                       &hmac)
            }
        }
        return Data(hmac)
    }
    
    func generateRandomBytes(count: Int) -> Data {
        var bytes = [UInt8](repeating: 0, count: count)
        let status = SecRandomCopyBytes(kSecRandomDefault, count, &bytes)
        if status != errSecSuccess {
            LogManager.shared.error(message: "Failed to generate random bytes")
            for i in 0..<count {
                bytes[i] = UInt8.random(in: 0...255)
            }
        }
        return Data(bytes)
    }
    
    func sha256Hex(_ data: Data) -> String {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return hash.map { String(format: "%02x", $0) }.joined()
    }
    
    func sha256Hex(_ string: String) -> String {
        guard let data = string.data(using: .utf8) else {
            LogManager.shared.error(message: "Failed to convert string to UTF-8 data for hashing")
            return ""
        }
        return sha256Hex(data)
    }
    
    // MARK: Private Functions
    
    private func getKeyRef(keyBase64: String, keyOptions: Dictionary<String, Any>) -> SecKey? {
        let keyData = Data(base64Encoded: keyBase64, options: [])
        
        var error:Unmanaged<CFError>?
        let createdKeyRef: SecKey = SecKeyCreateWithData(keyData! as CFData, keyOptions as CFDictionary, &error)!
        
        return error != nil ? nil : createdKeyRef
    }
    
    private func getPrivateKey(keyBase64: String) -> SecKey? {
        let privateKeyOptions = [
            String(kSecAttrKeyType):        kSecAttrKeyTypeECSECPrimeRandom,
            String(kSecAttrKeyClass):       kSecAttrKeyClassPrivate,
            String(kSecAttrKeySizeInBits):  256
        ] as [String: Any]
        
        return getKeyRef(keyBase64: keyBase64, keyOptions: privateKeyOptions)
    }
    
    private func getPublicKey(keyBase64: String) -> SecKey? {
        let publicKeyOptions = [
            String(kSecAttrKeyType):            kSecAttrKeyTypeECSECPrimeRandom,
            String(kSecAttrKeyClass):           kSecAttrKeyClassPublic,
            String(kSecAttrKeySizeInBits):      256,
            String(kSecReturnPersistentRef):    true
        ] as [String: Any]
        
        return getKeyRef(keyBase64: keyBase64, keyOptions: publicKeyOptions)
    }
}
