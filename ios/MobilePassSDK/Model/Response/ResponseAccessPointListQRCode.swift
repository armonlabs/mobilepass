//
//  ResponseAccessPointListV2QRCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.08.2021.
//

import Foundation

struct ResponseAccessPointListQRCode: Codable {
    /** Id */
    var i: String;
    /** Data */
    var q: String;
    /** Direction */
    var d: Direction;
    /** Relay Number */
    var r: Int;
    /** Trigger Type */
    var t: QRTriggerType;
    /** Validate Geo Location */
    var v: Bool?; // Default True
    /** HardwareId */
    var h: String;
}
