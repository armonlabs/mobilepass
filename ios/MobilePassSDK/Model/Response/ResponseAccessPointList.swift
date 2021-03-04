//
//  ResponseAccessPointList.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 18.02.2021.
//

import Foundation

struct ResponseAccessPointList: Codable {
    var pagination: ResponsePagination
    var items:      [ResponseAccessPointItem]
}
