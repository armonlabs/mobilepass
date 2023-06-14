//
//  PassFlowResultCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

enum PassFlowResultCode: Int, Codable {
    case CANCEL     = 1;
    case FAIL       = 2;
    case SUCCESS    = 3;
}
