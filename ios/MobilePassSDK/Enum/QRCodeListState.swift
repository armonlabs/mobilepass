//
//  QRCodeListState.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 21.02.2021.
//

import Foundation

enum QRCodeListState: Int, Codable {
    case INITIALIZING       = 1
    case SYNCING            = 2
    case USING_STORED_DATA  = 3
    case USING_SYNCED_DATA  = 4
    case EMPTY              = 5
}
