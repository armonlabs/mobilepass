package com.armongate.mobilepasssdk.manager;

import android.util.Base64;
import com.armongate.mobilepasssdk.model.request.RequestHandshake;
import com.armongate.mobilepasssdk.model.response.ResponseHandshake;
import com.armongate.mobilepasssdk.service.BaseService;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages handshake process and request signing for secure API communication
 * 
 * Responsibilities:
 * - Perform handshake with server to obtain ephemeral keys
 * - Sign requests using HMAC-SHA256 with combined key (STATIC + EPHEMERAL)
 * - Queue concurrent handshake requests to prevent duplicates
 * - Provide signature headers for all authenticated requests
 * 
 * Thread-safe implementation using synchronized methods and completion queues.
 */
public class HandshakeManager {

    // Singleton

    private static HandshakeManager instance = null;

    private HandshakeManager() {
    }

    public static HandshakeManager getInstance() {
        if (instance == null) {
            instance = new HandshakeManager();
        }
        return instance;
    }

    // Private Fields

    private boolean isHandshaking = false;
    private final List<HandshakeCompletionListener> completionQueue = new ArrayList<>();
    private final Object lock = new Object();

    // Listener Interface

    public interface HandshakeCompletionListener {
        void onHandshakeCompleted(boolean success, String error);
    }

    // Public Methods

    /**
     * Ensures a valid handshake exists before making authenticated requests
     * Performs handshake if needed, or queues completion if already in progress
     * 
     * @param listener Callback invoked when handshake is complete (or immediately if already valid)
     */
    public void ensureHandshake(final HandshakeCompletionListener listener) {
        synchronized (lock) {
            // Check if renewal is needed
            if (!EphemeralKeyManager.getInstance().needsRenewal()) {
                LogManager.getInstance().debug("Ephemeral key is still valid, no handshake needed");
                listener.onHandshakeCompleted(true, null);
                return;
            }

            // Add to queue
            completionQueue.add(listener);

            // If already handshaking, just wait in queue
            if (isHandshaking) {
                LogManager.getInstance().debug("Handshake already in progress, queuing completion");
                return;
            }

            // Start handshake
            isHandshaking = true;
            LogManager.getInstance().info("Starting handshake process");
        }

        performHandshake();
    }

    /**
     * Signs a request with HMAC-SHA256 signature
     * 
     * @param method HTTP method (GET, POST, etc.)
     * @param path Request path (without domain, with query params if any)
     * @param timestamp Request timestamp in seconds (Unix epoch)
     * @param bodyHash SHA-256 hex hash of request body
     * @return Signature as base64 string, or null on error
     */
    public String signRequest(String method, String path, long timestamp, String bodyHash) {
        try {
            // Get ephemeral key and nonce
            byte[] ephemeralKey = EphemeralKeyManager.getInstance().getEphemeralKey();
            String serverNonce = EphemeralKeyManager.getInstance().getServerNonce();

            if (ephemeralKey == null || serverNonce == null) {
                LogManager.getInstance().error("Cannot sign request: ephemeral key or nonce missing", null);
                return null;
            }

            // Get static key (API key as UTF-8 bytes)
            byte[] staticKey = getStaticKey();
            if (staticKey == null) {
                LogManager.getInstance().error("Cannot sign request: static key (apiKey) missing", null);
                return null;
            }

            // Derive COMBINED_KEY = HMAC(STATIC_KEY, EPHEMERAL_KEY)
            byte[] combinedKey = CryptoManager.getInstance().hmacSHA256(staticKey, ephemeralKey);
            if (combinedKey == null) {
                LogManager.getInstance().error("Failed to derive combined key", null);
                return null;
            }

            // Build canonical string: METHOD|PATH|timestamp|serverNonce|bodyHash
            String canonicalString = method.toUpperCase() + "|" + path + "|" + timestamp + "|" + serverNonce + "|" + bodyHash;

            // Compute signature: HMAC(COMBINED_KEY, canonicalString)
            byte[] signatureBytes = CryptoManager.getInstance().hmacSHA256(combinedKey, canonicalString);
            if (signatureBytes == null) {
                LogManager.getInstance().error("Failed to compute request signature", null);
                return null;
            }

            String signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP);

            return signatureBase64;
        } catch (Exception ex) {
            LogManager.getInstance().error("Request signing failed: " + ex.getLocalizedMessage(), null);
            return null;
        }
    }

    /**
     * Clears handshake cache (called when configuration changes)
     */
    public void clearCache() {
        synchronized (lock) {
            EphemeralKeyManager.getInstance().clear();
            
            // Fail any pending handshake completions
            for (HandshakeCompletionListener listener : completionQueue) {
                listener.onHandshakeCompleted(false, "Cache cleared");
            }
            completionQueue.clear();
            isHandshaking = false;
            
            LogManager.getInstance().debug("Handshake cache cleared");
        }
    }

    // Private Methods

    /**
     * Performs the actual handshake request to the server
     */
    private void performHandshake() {
        try {
            // Generate ephemeral key (32 bytes random)
            byte[] clientEphemeralKey = CryptoManager.getInstance().generateRandomBytes(32);
            String clientEphemeralKeyBase64 = Base64.encodeToString(clientEphemeralKey, Base64.NO_WRAP);

            // Get static key for signing handshake
            byte[] staticKey = getStaticKey();
            if (staticKey == null) {
                notifyHandshakeCompletion(false, "API key not configured");
                return;
            }

            // Get current timestamp
            long timestamp = System.currentTimeMillis() / 1000;

            // Compute handshake signature: HMAC(STATIC_KEY, memberId|timestamp|ephemeralKey)
            String signatureData = ConfigurationManager.getInstance().getMemberId() + "|" + timestamp + "|" + clientEphemeralKeyBase64;
            byte[] signatureBytes = CryptoManager.getInstance().hmacSHA256(staticKey, signatureData);
            if (signatureBytes == null) {
                notifyHandshakeCompletion(false, "Failed to compute handshake signature");
                return;
            }
            String signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP);

            // Build handshake request
            RequestHandshake request = new RequestHandshake();
            request.memberId = ConfigurationManager.getInstance().getMemberId();
            request.timestamp = timestamp;
            request.ephemeralKey = clientEphemeralKeyBase64;
            request.signature = signature;

            LogManager.getInstance().debug("Sending handshake request");

            // Store ephemeral key to use in response handler
            final byte[] ephemeralKeyToStore = clientEphemeralKey;

            // Send handshake request (BaseService will skip signature for this endpoint)
            BaseService.getInstance().requestPost("api/v2/sdk/handshake", request, ResponseHandshake.class,
                new BaseService.ServiceResultListener<ResponseHandshake>() {
                    @Override
                    public void onCompleted(ResponseHandshake response) {
                        handleHandshakeResponse(response, ephemeralKeyToStore);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        String error = "Handshake failed with status " + statusCode + ": " + message;
                        LogManager.getInstance().error(error, null);
                        notifyHandshakeCompletion(false, error);
                    }
                });

        } catch (Exception ex) {
            String error = "Handshake request failed: " + ex.getLocalizedMessage();
            LogManager.getInstance().error(error, null);
            notifyHandshakeCompletion(false, error);
        }
    }

    /**
     * Handles handshake response from server
     */
    private void handleHandshakeResponse(ResponseHandshake response, byte[] clientEphemeralKey) {
        if (response == null) {
            notifyHandshakeCompletion(false, "Empty handshake response");
            return;
        }

        if (!response.ok) {
            String error = response.error != null ? response.error : "Unknown error";
            LogManager.getInstance().error("Handshake failed: " + error, null);
            notifyHandshakeCompletion(false, error);
            return;
        }

        if (response.serverNonce == null) {
            notifyHandshakeCompletion(false, "Invalid handshake response: missing serverNonce");
            return;
        }

        // Store client-generated ephemeral key and server nonce
        long ttl = response.ephemeralTTL != null ? response.ephemeralTTL : 120;
        String clientEphemeralKeyBase64 = Base64.encodeToString(clientEphemeralKey, Base64.NO_WRAP);
        EphemeralKeyManager.getInstance().setEphemeralKey(clientEphemeralKeyBase64, response.serverNonce, ttl);

        LogManager.getInstance().info("Handshake completed successfully, TTL: " + ttl + "s");
        notifyHandshakeCompletion(true, null);
    }

    /**
     * Notifies all queued completion listeners
     */
    private void notifyHandshakeCompletion(boolean success, String error) {
        List<HandshakeCompletionListener> listeners;
        
        synchronized (lock) {
            listeners = new ArrayList<>(completionQueue);
            completionQueue.clear();
            isHandshaking = false;
        }

        for (HandshakeCompletionListener listener : listeners) {
            listener.onHandshakeCompleted(success, error);
        }
    }

    /**
     * Gets static key (API key as UTF-8 bytes)
     * @return API key bytes, or null if not configured
     */
    private byte[] getStaticKey() {
        try {
            String apiKey = ConfigurationManager.getInstance().getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return null;
            }
            return apiKey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LogManager.getInstance().error("Failed to encode API key: " + ex.getLocalizedMessage(), null);
            return null;
        }
    }
}
