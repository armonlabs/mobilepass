//
//  PassResult.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 26.07.2022.
//

import Foundation

public struct PassResult {
    public var success:     Bool
    public var failCode:    Int?
    public var direction:   Int?
    public var clubId:      String?
    public var clubName:    String?
    
    init(success: Bool, direction: Direction?, clubId: String?, clubName: String?, failCode: Int?) {
        self.success    = success
        self.direction  = direction != nil ? direction!.rawValue : nil
        self.clubId     = clubId
        self.clubName   = clubName
        self.failCode   = failCode
    }
}
