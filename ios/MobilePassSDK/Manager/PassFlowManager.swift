//
//  PassFlowManager.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 1.06.2023.
//

import Foundation

class PassFlowManager: NSObject {
    
    // MARK: Singleton
    
    static let shared = PassFlowManager()
    private override init() {
        super.init()
    }
    
    // MARK: Fields
    
    private var states: [PassFlowState] = [];
    
    // MARK: Public Functions
    
    func clearStates() {
        self.states.removeAll()
    }
    
    func getStates() -> [PassFlowState] {
        return self.states
    }
    
    func addToStates(state: PassFlowStateCode, data: String? = nil) {
        self.states.append(PassFlowState(state: state, data: data))
    }
    
}
