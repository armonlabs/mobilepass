//
//  AnalyticsQueueItem.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 4.03.2025.
//

struct AnalyticsQueueItem: Codable {
    var request: RequestAnalyticsData
    var timestamp: Date
    
    init(request: RequestAnalyticsData) {
        self.request = request
        self.timestamp = Date()
    }
}
