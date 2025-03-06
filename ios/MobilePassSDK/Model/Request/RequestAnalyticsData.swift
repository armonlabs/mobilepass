//
//  RequestAnalyticsData.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 4.03.2025.
//


struct RequestAnalyticsData: Codable {
    var accessTime: String
    var duration: Int64
    var result: AnalyticsResult
    var method: PassMethod?
    var clubId: String?
    var qrCodeId: String?
    var direction: Int?
    var os: AnalyticsOS
    var steps: [AnalyticsStep]
    
    var dictionary: [String: Any] {
        var dict: [String: Any] = [
            "accessTime": accessTime,
            "duration": duration,
            "result": result.rawValue,
            "os": os.rawValue,
            "steps": steps.map { [
                "c": $0.c,
                "t": $0.t,
                "m": $0.m
            ].compactMapValues { $0 }}
        ]
        
        if let method = method {
            dict["method"] = method.rawValue
        }
        if let clubId = clubId {
            dict["clubId"] = clubId
        }
        if let qrCodeId = qrCodeId {
            dict["qrCodeId"] = qrCodeId
        }
        if let direction = direction {
            dict["direction"] = direction
        }
        
        return dict
    }
}
