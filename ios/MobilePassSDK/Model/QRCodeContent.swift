//
//  QRCodeContent.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 18.02.2021.
//

import Foundation

struct QRCodeContent: Codable {
    var code:           String
    var accessPoint:    ResponseAccessPointItem
    var action:         ResponseAccessPointItemQRCodeItem
}
