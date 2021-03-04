//
//  RequestError.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

struct RequestError: Error {
    
    enum Reason {
      case invalidServer
      case missingToken
      case other
    }
    
  var message:  String
  var code:     Int
  var reason:   Reason
    
    private var reasonTag: String {
        switch self.reason {
        case .invalidServer:
          return "Invalid Server Address"
        case .missingToken:
          return "Missing Refreh Token"
        case .other:
          return "Error"
        }
    }
    
    var localizedDescription: String {
      return "\(self.reasonTag) / \(self.code.description) | \(self.message)"
    }
    
    init(message: String, reason: Reason, code: Int?) {
        self.code     = code ?? -1000
        self.message  = message
        self.reason   = reason
    }
        
}
