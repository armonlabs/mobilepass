//
//  DataTypes.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

import Foundation

struct DataTypes {
    enum TYPE {
        static let AuthChallengeForPublicKey = 1000
        static let AuthChallengeResult = 1002
    }
    enum RESULT {
        static let Succeed = 1
        static let Failed = 2
    }
}
