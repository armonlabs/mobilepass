//
//  ResponseAccessPointItem.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItem: Codable {
    var id:             String
    var name:           String
    var qrCodeData:     [ResponseAccessPointItemQRCodeItem]
    var geoLocation:    ResponseAccessPointItemGeoLocation?
    var deviceInfo:     ResponseAccessPointItemDeviceInfo
}
