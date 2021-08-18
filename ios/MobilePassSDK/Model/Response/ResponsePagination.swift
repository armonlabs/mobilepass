//
//  ResponsePagination.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponsePagination: Codable {
    /** Take */
    var t: Int;
    /** Skip */
    var s: Int;
    /** Total */
    var c: Int;
}
