//
//  DeviceCapability.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation

public struct DeviceCapability: Codable {
    var support:        Bool
    var enabled:        Bool
    var needAuthorize:  Bool
}
