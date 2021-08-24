//
//  AccessPointService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class AccessPointService {
    func remoteOpen(request: RequestAccess, completion: @escaping (Result<ResponseDefault?, RequestError>) -> Void) {
        BaseService.shared.requestPost(url: "api/v2/access", data: request.dictionary, completion: completion)
    }
}
