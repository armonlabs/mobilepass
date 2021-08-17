//
//  ResponseAccessPointListV2.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.08.2021.
//

import Foundation

struct ResponseAccessPointList: Codable {
    var pagination: ResponsePagination
    var items:      [ResponseAccessPointListItem]
}
