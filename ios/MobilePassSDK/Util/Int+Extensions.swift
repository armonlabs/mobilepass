//
//  Int+Extensions.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 26.02.2021.
//

import Foundation

extension Int {
    
    func hex() -> String {
        var result = String(self, radix: 16, uppercase: true)
        
        if (result.count % 2 != 0) {
            result = "0" + result
        }
        
        return result
    }
    
    func data(_ length: Int, repeating: UInt8, fillLeading: Bool) -> Data {
        let hexValue: String = self.hex()
        
        var fillArray = Array<UInt8>(repeating: repeating, count: length)
        let dataArray = [UInt8](hexValue.data(using: .hexadecimal)!)
        
        let startIndex = fillLeading ? fillArray.count - dataArray.count : 0

        fillArray[startIndex...] = dataArray[0...]
        return Data(fillArray)
    }
    
    func mergeToData(_ direction: Int, _ relay: Int) -> Data? {
        let deviceValue     = ("00" + String(self, radix: 2)).suffix(2)
        let directionValue  = ("00" + String(direction, radix: 2)).suffix(2)
        let relayValue      = ("0000" + String(relay, radix: 2)).suffix(4)

        let merged = deviceValue + directionValue + relayValue
        guard let num = UInt64(merged, radix: 2) else { return nil }
        
        return String(("0" + String(num, radix: 16, uppercase: true)).suffix(2)).data(using: .hexadecimal)!
    }
}
