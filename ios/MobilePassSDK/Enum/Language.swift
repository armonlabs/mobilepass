//
//  Language.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 23.05.2025.
//

import Foundation

enum Language: String, Codable {
    case TR = "tr"
    case EN = "en"

    /**
     * Resolve a configured language value to a supported language.
     *
     * Applies exactly the rule the server applies to `Accept-Language`: anything
     * starting with "tr" is Turkish, everything else is English. Both the request
     * header and the Bluetooth language byte are derived from this, so the server
     * and the device can never answer the same pass attempt in different
     * languages - values like "en-US" or "EN" used to resolve to English on the
     * server and Turkish on the device.
     */
    static func normalized(_ value: String?) -> Language {
        let value = value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        return value.hasPrefix("tr") ? .TR : .EN
    }
}
