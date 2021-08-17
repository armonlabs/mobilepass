//
//  ResponseAccessPointListV2GeoLocation.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.08.2021.
//

import Foundation

struct ResponseAccessPointListGeoLocation: Codable {
    /** Latitude */
    var la: Double
    /** Longitude */
    var lo: Double
    /** Radius */
    var r:  Int
}
