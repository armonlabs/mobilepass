//
//  RequestAccess.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestAccess: Codable {
    var qrCodeId:       String;
    var clubMemberId:   String;
    
    var dictionary: [String: Any] {
        return [
            "qrCodeId": qrCodeId,
            "clubMemberId": clubMemberId
        ]
    }
}
