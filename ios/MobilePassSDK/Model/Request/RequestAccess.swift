//
//  RequestAccess.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestAccess: Codable {
    var qrCodeId:       String;
    var installationId: String?;
        
    var dictionary: [String: Any] {
        var dict: [String: Any] = [
            "q": qrCodeId
        ]
        
        if let installationId = installationId {
            dict["i"] = installationId
        }
        
        return dict
    }
}
