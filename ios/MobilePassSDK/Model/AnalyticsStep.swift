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
    
    init(code: Int, message: String?, timestamp: Date) {
        self.c = code
        self.m = message
        self.t = ISO8601DateFormatter().string(from: timestamp)
    }
}