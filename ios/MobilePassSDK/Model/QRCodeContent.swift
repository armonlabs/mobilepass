//
//  QRCodeContent.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 18.02.2021.
//

import Foundation

struct QRCodeContent: Codable {
    var terminals:      [ResponseAccessPointListTerminal]
    var qrCode:         ResponseAccessPointListQRCode
    var geoLocation:    ResponseAccessPointListGeoLocation?
}
