//
//  String+Extensions.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

public enum ExtendedEncoding {
    case hexadecimal
}

extension String {
    func data(using encoding: ExtendedEncoding) -> Data? {
        let hexStr = self.dropFirst(self.hasPrefix("0x") ? 2 : 0)
        
        guard hexStr.count % 2 == 0 else { return nil }
        
        var newData = Data(capacity: hexStr.count/2)
        
        var indexIsEven = true
        for i in hexStr.indices {
            if indexIsEven {
                let byteRange = i...hexStr.index(after: i)
                guard let byte = UInt8(hexStr[byteRange], radix: 16) else { return nil }
                newData.append(byte)
            }
            indexIsEven.toggle()
        }
        
        return newData
    }
    
    func localized(withComment comment: String = "") -> String {
        return Bundle(for: PassFlowController.self).localizedString(forKey: self, value: "\(self)", table: nil)
    }
    
    func localized(_ language: String) -> String {
        let path = Bundle(for: PassFlowController.self).path(forResource: language, ofType: "lproj")
        let bundle: Bundle
        if let path = path {
            bundle = Bundle(path: path) ?? Bundle(for: PassFlowController.self)
        } else {
            bundle = Bundle(for: PassFlowController.self)
        }
        
        return bundle.localizedString(forKey: self, value: "\(self)", table: nil)
    }
}
