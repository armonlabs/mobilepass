//
//  LogItem.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 19.08.2021.
//

import Foundation

public struct LogItem {
    public var level:      Int
    public var code:       Int?
    public var message:    String
    public var time:       Date
    
    init(level: Int, code: Int?, message: String) {
        self.level      = level
        self.code       = code
        self.message    = message
        self.time       = Date()
    }
}
