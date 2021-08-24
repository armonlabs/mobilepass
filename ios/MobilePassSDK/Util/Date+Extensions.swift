//
//  Date+Extensions.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 19.08.2021.
//

import Foundation

extension Date {
   func getFormattedDate(format: String) -> String {
        let dateformat = DateFormatter()
        dateformat.dateFormat = format
        return dateformat.string(from: self)
    }
}
