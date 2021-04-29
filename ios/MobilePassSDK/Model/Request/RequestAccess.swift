//
//  RequestAccess.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct RequestAccess: Codable {
    var accessPointId:  String;
    var clubMemberId:   String;
    var direction:      Direction;
    
    var dictionary: [String: Any] {
        return [
            "accessPointId": accessPointId,
            "clubMemberId": clubMemberId,
            "direction": direction.rawValue
        ]
    }
}
