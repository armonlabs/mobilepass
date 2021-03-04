//
//  ResponseAccessPointItemDeviceInfo.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItemDeviceInfo: Codable {
    var id:         String
    var brand:      Int?
    var model:      String?
    var name:       String?
    var location:   String?
    var publicKey:  String?
}
