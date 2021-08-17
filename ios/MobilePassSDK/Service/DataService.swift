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
    
    func getAccessList(pagination: RequestPagination, syncDate: Int64?, completion: @escaping (Result<ResponseAccessPointList?, RequestError>) -> Void) {
        var url = "api/v1/accessPoints/list?take=\(pagination.take ?? 100)&skip=\(pagination.skip ?? 0)"
        
        if (syncDate != nil) {
            url += "&syncDate=\(syncDate!)"
        }
        
        BaseService.shared.requestGet(url: url, completion: completion)
    }
}
