//
//  ResponsePagination.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponsePagination: Codable {
    var take:   Int;
    var skip:   Int;
    var total:  Int;
}
