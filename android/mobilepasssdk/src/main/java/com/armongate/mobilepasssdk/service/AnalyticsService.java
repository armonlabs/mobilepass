package com.armongate.mobilepasssdk.service;

import android.content.Context;

import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.StorageKeys;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.StorageManager;
import com.armongate.mobilepasssdk.model.AnalyticsQueueItem;
import com.armongate.mobilepasssdk.model.request.RequestAnalyticsData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyticsService {
    private static final int MAX_RETRY_AGE_HOURS = 72;

    private List<AnalyticsQueueItem> loadQueue(Context context) {
        try {
            String data = StorageManager.getInstance().getValue(context, StorageKeys.ANALYTICS_QUEUE);
            Type listType = new TypeToken<ArrayList<AnalyticsQueueItem>>(){}.getType();
            List<AnalyticsQueueItem> queue = new Gson().fromJson(data, listType);

            if (queue == null) return new ArrayList<>();
            
            // Filter out old entries
            long cutoffTime = System.currentTimeMillis() - (MAX_RETRY_AGE_HOURS * 3600 * 1000);
            List<AnalyticsQueueItem> filteredQueue = new ArrayList<>();
            for (AnalyticsQueueItem item : queue) {
                if (item.timestamp > cutoffTime) {
                    filteredQueue.add(item);
                }
            }
            return filteredQueue;

        } catch (Exception e) {
            LogManager.getInstance().error("Failed to load analytics queue: " + e.getLocalizedMessage(), LogCodes.OTHER);
            return new ArrayList<>();
        }
    }
    
    private void saveQueue(Context context, List<AnalyticsQueueItem> queue) {
        try {
            String data = new Gson().toJson(queue);
            StorageManager.getInstance().setValue(context, StorageKeys.ANALYTICS_QUEUE, data);
        } catch (Exception e) {
            LogManager.getInstance().error("Failed to save analytics queue: " + e.getLocalizedMessage(), LogCodes.OTHER);
        }
    }

    public void sendAnalytics(Context context, RequestAnalyticsData request, BaseService.ServiceResultListener listener) {
        // Load and clear existing queue
        List<AnalyticsQueueItem> queue = loadQueue(context);
        List<AnalyticsQueueItem> queuedItems = new ArrayList<>(queue);
        queue.clear();
        saveQueue(context, queue);

        // Add new request to queued items
        queuedItems.add(new AnalyticsQueueItem(request));

        List<AnalyticsQueueItem> failedItems = new ArrayList<>();
        AtomicInteger completeCount = new AtomicInteger(0);

        // Process all queued items
        for (AnalyticsQueueItem item : queuedItems) {
            BaseService.getInstance().requestPost("api/v2/analytics", item.request, null, new BaseService.ServiceResultListener() {
                @Override
                public void onCompleted(Object result) {
                    processCompletion(context, queuedItems, failedItems, completeCount, item, null);
                }
                
                @Override
                public void onError(int statusCode, String message) {
                    processCompletion(context, queuedItems, failedItems, completeCount, item, message);
                }
            });
        }
    }

    private synchronized void processCompletion(Context context, 
                                              List<AnalyticsQueueItem> queuedItems,
                                              List<AnalyticsQueueItem> failedItems, 
                                              AtomicInteger completeCount,
                                              AnalyticsQueueItem item,
                                              String errorMessage) {
        // Add to failed items if there was an error and item isn't too old
        if (errorMessage != null) {
            long cutoffTime = System.currentTimeMillis() - (MAX_RETRY_AGE_HOURS * 3600 * 1000);
            if (item.timestamp > cutoffTime) {
                failedItems.add(item);
            }
        }

        // Check if all items are processed
        if (completeCount.incrementAndGet() == queuedItems.size()) {
            if (!failedItems.isEmpty()) {
                // Load current queue
                List<AnalyticsQueueItem> currentQueue = loadQueue(context);
                
                // Add failed items
                currentQueue.addAll(failedItems);
                
                // Save updated queue
                saveQueue(context, currentQueue);
            }
        }
    }
}