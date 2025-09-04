//
//  AnalyticsStep.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 4.03.2025.
//


struct AnalyticsStep: Codable {
    var c: Int
    var m: String?
    var t: String
    
    init(code: Int, message: String?, timestamp: String) {
        self.c = code
        self.m = message
        self.t = timestamp
    }
}
