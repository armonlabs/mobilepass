//
//  QRTriggerType.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

enum QRTriggerType: Int, Codable, CaseIterable {
    case Bluetooth              = 1
    case Remote                 = 2
    case BluetoothThenRemote    = 3
    case RemoteThenBluetooth    = 4
}
