//
//  RequestPagination.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestPagination: Codable {
    /** Take */
    var t: Int?;
    /** Skip */
    var s: Int?;
    
    var dictionary: [String: Any] {
        return [
            "t": t ?? 100,
            "s": s ?? 0
        ]
    }
}
