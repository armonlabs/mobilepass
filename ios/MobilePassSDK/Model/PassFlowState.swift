//
//  PassFlowState.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

public struct PassFlowState {
    public var state:   Int
    public var data:    String?
    
    init(state: PassFlowStateCode, data: String?) {
        self.state  = state.rawValue
        self.data   = data
    }
}
