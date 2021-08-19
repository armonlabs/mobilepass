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
        var url = "api/v2/listAccessPointsRequest?t=\(pagination.t ?? 100)&s=\(pagination.s ?? 0)"
        
        if (syncDate != nil) {
            url += "&d=\(syncDate!)"
        }
        
        BaseService.shared.requestGet(url: url, completion: completion)
    }
}
