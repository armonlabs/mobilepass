//
//  RequestAccess.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestAccess: Codable {
    var q: String;
    
    var dictionary: [String: Any] {
        return [
            "q": q
        ]
    }
}
