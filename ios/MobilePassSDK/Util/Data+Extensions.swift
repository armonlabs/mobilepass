//
//  Data+Extensions.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

extension Data {
    struct HexEncodingOptions: OptionSet {
        let rawValue: Int
        static let upperCase = HexEncodingOptions(rawValue: 1 << 0)
    }

    func hexEncodedString(options: HexEncodingOptions = []) -> String {
        let format = options.contains(.upperCase) ? "%02hhX" : "%02hhx"
        return map { String(format: format, $0) }.joined()
    }
    
    func toString() -> String {
        return String(decoding: self, as: UTF8.self)
    }
    
    func toInt() -> Int {
        var value: Int = 0
        let _ = Swift.withUnsafeMutableBytes(of: &value, { self.reversed().copyBytes(to: $0)} )
        
        return value
    }
    
    func toBool() -> Bool {
        let value: Int = self.toInt()
        return value != 0
    }
    
    func fill(length: Int, repeating: UInt8) -> Data {
        var fillArray = Array<UInt8>(repeating: repeating, count: length)
        let dataArray = [UInt8](self)
        
        for (index, element) in dataArray.enumerated() {
           fillArray[index] = element
        }
        
        return Data(fillArray)
    }
    
}
