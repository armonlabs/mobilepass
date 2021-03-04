//
//  ResponseAccessPointItemGeoLocation.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItemGeoLocation: Codable {
    var latitude:   Double
    var longitude:  Double
    var radius:     Int
}
