//
//  AccessPointService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

class AccessPointService {
    /**
     * Request a remote door open.
     *
     * Response is decoded as `ResponseMessage` for both success and failure so
     * that the result code is available in either case: on success it arrives in
     * the response body, on failure in `RequestError.resultCode`.
     */
    func remoteOpen(request: RequestAccess, completion: @escaping (Result<ResponseMessage?, RequestError>) -> Void) {
        BaseService.shared.requestPost(url: "api/v2/access", data: request.dictionary, completion: completion)
    }
}
