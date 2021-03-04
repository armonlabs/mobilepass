//
//  AccessPointService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class AccessPointService {
    func remoteOpen(accessPointId: String, direction: Direction, completion: @escaping (Result<ResponseDefault?, RequestError>) -> Void) {
        BaseService.shared.requestGet(url: "api/v1/access?accessPointId=\(accessPointId)&direction=\(direction.rawValue)", completion: completion)
    }
}
