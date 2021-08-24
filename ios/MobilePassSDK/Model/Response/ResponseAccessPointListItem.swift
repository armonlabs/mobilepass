//
//  ResponseAccessPointListV2Item.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.08.2021.
//

import Foundation

struct ResponseAccessPointListItem: Codable {
    /** Id */
    var i: String?
    /** Name */
    var n: String?
    /** QR Codes */
    var q: [ResponseAccessPointListQRCode]?
    /** Terminals */
    var t: [ResponseAccessPointListTerminal]?
    /** GeoLocation */
    var g: ResponseAccessPointListGeoLocation?
    
    /** QR Code Ids | Only for mapping on storage */
    var d: [String]?
}
