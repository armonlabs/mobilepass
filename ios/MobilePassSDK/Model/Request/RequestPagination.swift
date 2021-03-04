//
//  RequestPagination.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestPagination: Codable {
    var take: Int?;
    var skip: Int?;
    
    var dictionary: [String: Any] {
        return [
            "take": take ?? 100,
            "skip": skip ?? 0
        ]
    }
}
