//
//  RequestSetUserData.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestSetUserData: Codable {
    var publicKey:      String;
    var clubMemberId:   String;
    
    var dictionary: [String: Any] {
        return [
            "publicKey": publicKey,
            "clubMemberId": clubMemberId
        ]
    }
}
