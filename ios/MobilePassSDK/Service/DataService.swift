//
//  DataService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

class DataService {
    func sendUserInfo(request: RequestSetUserData, completion: @escaping (Result<ResponseDefault?, RequestError>) -> Void) {
        BaseService.shared.requestPost(url: "api/v1/setpublickey", data: request.dictionary, completion: completion)
    }
    
    func getAccessList(request: RequestPagination, completion: @escaping (Result<ResponseAccessPointList?, RequestError>) -> Void) {
        BaseService.shared.requestGet(url: "api/v1/listAccessPointsRequest?take=\(request.take ?? 100)&skip=\(request.skip ?? 0)", completion: completion)
    }
}
