//
//  BLEFailCode.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 23.05.2025.
//

import Foundation

public enum BLEFailCode: Int, Codable {
    case UserNotFound           = 20
    case ChallengeFailed        = 21
    case PerfectGymNetworkError = 30
    case PerfectGymNoAccess     = 31
    case BenefitsNetworkError   = 40
    case BenefitsInvalidCard    = 41
    case BenefitsLimitReached   = 42
    case BenefitsUnknownError   = 43
    case DeviceError            = 90
}
