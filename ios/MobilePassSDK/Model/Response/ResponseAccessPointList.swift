//
//  ResponseAccessPointListV2.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.08.2021.
//

import Foundation

struct ResponseAccessPointList: Codable {
    /** Pagination */
    var p: ResponsePagination
    /** Items */
    var i: [ResponseAccessPointListItem]
    /** Deleted Access Point Ids */
    var d: [String]?
}
