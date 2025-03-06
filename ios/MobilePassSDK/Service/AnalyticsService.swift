//
//  AnalyticsService.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 4.03.2025.
//


class AnalyticsService {
    private enum Constants {
        static let maxRetryAgeHours: TimeInterval = 72
        static let analyticsQueueKey = StorageKeys.ANALYTICS_QUEUE
    }

    private func loadQueue() -> [AnalyticsQueueItem] {
        guard let data = try? StorageManager.shared.getValue(key: Constants.analyticsQueueKey, secure: false),
                let jsonData = data.data(using: .utf8),
                let queue = try? JSONDecoder().decode([AnalyticsQueueItem].self, from: jsonData) else {
            return []
        }
        
        let cutoffDate = Date().addingTimeInterval(-Constants.maxRetryAgeHours * 3600)
        return queue.filter { $0.timestamp > cutoffDate }
    }
    
    private func saveQueue(_ queue: [AnalyticsQueueItem]) {
        do {
            let data = try JSONEncoder().encode(queue)
            guard let str = String(data: data, encoding: .utf8) else {
                LogManager.shared.error(message: "Failed to encode analytics queue")
                return
            }
            try StorageManager.shared.setValue(key: Constants.analyticsQueueKey, value: str, secure: false)
        } catch {
            LogManager.shared.error(message: "Failed to save analytics queue: \(error)")
        }
    }

    func sendAnalytics(request: RequestAnalyticsData, completion: @escaping (Result<ResponseDefault?, RequestError>) -> Void) {
        var queue = loadQueue()
        var queuedItems = queue
        queue.removeAll()
        saveQueue(queue)

        queuedItems.append(AnalyticsQueueItem(request: request))

        var failedItems: [AnalyticsQueueItem] = []
        var completeCount = 0;

        // Process queued items first
        for item in queuedItems {
            BaseService.shared.requestPost(url: "api/v2/analytics", data: item.request.dictionary) { [weak self] (result: Result<ResponseDefault?, RequestError>) in
                if case .failure(_) = result {
                    // Re-queue failed item if it's not too old
                    if item.timestamp > Date().addingTimeInterval(-Double(Constants.maxRetryAgeHours * 3600)) {
                        failedItems.append(item)
                    }
                }

                completeCount += 1
                
                if (completeCount == queuedItems.count) {
                    if !failedItems.isEmpty {
                        var currentQueue: [AnalyticsQueueItem] = []

                        // Load queue
                        if let data = try? StorageManager.shared.getValue(key: Constants.analyticsQueueKey, secure: false),
                           let queue = try? JSONDecoder().decode([AnalyticsQueueItem].self, from: data.data(using: .utf8)!) {
                            let cutoffDate = Date().addingTimeInterval(-Double(Constants.maxRetryAgeHours * 3600))
                            currentQueue = queue.filter { $0.timestamp > cutoffDate }
                        }

                        // Append failed items to queue
                        currentQueue.append(contentsOf: failedItems)

                        // Save queue
                        if let data = try? JSONEncoder().encode(currentQueue),
                           let str = String(data: data, encoding: .utf8) {
                            _ = try? StorageManager.shared.setValue(key: Constants.analyticsQueueKey, value: str, secure: false)
                        }
                    }
                }
            }
        }
    }
    
}
