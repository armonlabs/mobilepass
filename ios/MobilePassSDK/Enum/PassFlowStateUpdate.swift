//
//  PassFlowStateUpdate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 15.02.2021.
//

public enum PassFlowStateUpdate {
    case stateChanged(state: Int, message: String?)
    case completed(result: PassFlowResult)
}

