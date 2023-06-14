//
//  PassFlowResult.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

public struct PassFlowResult {
    public var result:      Int
    public var direction:   Int?
    public var clubId:      String?
    public var clubName:    String?
    public var states:      [PassFlowState]
    
    init(result: PassFlowResultCode, states: [PassFlowState], direction: Direction?, clubId: String?, clubName: String?) {
        self.result     = result.rawValue
        self.direction  = direction != nil ? direction!.rawValue : nil
        self.clubId     = clubId
        self.clubName   = clubName
        self.states     = states
    }
}
