package com.armongate.mobilepasssdk.manager;

import android.util.Base64;

/**
 * Manages ephemeral keys for secure request signing
 * 
 * Ephemeral keys are temporary cryptographic keys received from the server during handshake.
 * They have a limited lifetime (TTL) and are used in combination with the static API key
 * to sign all authenticated requests.
 * 
 * Thread-safe implementation using synchronized methods.
 */
public class EphemeralKeyManager {

    // Singleton

    private static EphemeralKeyManager instance = null;

    private EphemeralKeyManager() {
    }

    public static EphemeralKeyManager getInstance() {
        if (instance == null) {
            instance = new EphemeralKeyManager();
        }
        return instance;
    }

    // Private Fields

    private byte[] ephemeralKey = null;
    private String serverNonce = null;
    private long handshakeTimestamp = 0;
    private long ephemeralTTL = 120; // Default 120 seconds, updated from server response

    private final Object lock = new Object();

    // Public Methods

    /**
     * Gets the current ephemeral key
     * @return Ephemeral key as byte array, or null if not set
     */
    public byte[] getEphemeralKey() {
        synchronized (lock) {
            return ephemeralKey;
        }
    }

    /**
     * Gets the server nonce received during handshake
     * @return Server nonce string, or null if not set
     */
    public String getServerNonce() {
        synchronized (lock) {
            return serverNonce;
        }
    }

    /**
     * Sets ephemeral key, server nonce, and TTL from handshake response
     * @param ephemeralKeyBase64 Base64-encoded ephemeral key from server
     * @param serverNonce Server nonce from handshake response
     * @param ttl Time-to-live in seconds for the ephemeral key
     */
    public void setEphemeralKey(String ephemeralKeyBase64, String serverNonce, long ttl) {
        synchronized (lock) {
            try {
                this.ephemeralKey = Base64.decode(ephemeralKeyBase64, Base64.NO_WRAP);
                this.serverNonce = serverNonce;
                this.ephemeralTTL = ttl;
                this.handshakeTimestamp = System.currentTimeMillis();

                LogManager.getInstance().debug("Ephemeral key set with TTL: " + ttl + "s");
            } catch (Exception ex) {
                LogManager.getInstance().error("Failed to decode ephemeral key: " + ex.getLocalizedMessage(), null);
                clear();
            }
        }
    }

    /**
     * Checks if ephemeral key needs renewal based on proportional threshold
     * Renewal threshold is 25% of the TTL (e.g., 30s remaining for 120s TTL)
     * @return true if key should be renewed, false otherwise
     */
    public boolean needsRenewal() {
        synchronized (lock) {
            if (ephemeralKey == null || serverNonce == null) {
                return true;
            }

            long currentTime = System.currentTimeMillis();
            long elapsed = (currentTime - handshakeTimestamp) / 1000; // Convert to seconds
            long remaining = ephemeralTTL - elapsed;

            // Renewal threshold: 25% of TTL
            long renewalThreshold = (long) (ephemeralTTL * 0.25);

            boolean shouldRenew = remaining <= renewalThreshold;

            if (shouldRenew) {
                LogManager.getInstance().debug("Ephemeral key needs renewal. Remaining: " + remaining + "s, Threshold: " + renewalThreshold + "s");
            }

            return shouldRenew;
        }
    }

    /**
     * Clears all ephemeral key data
     * Called when configuration changes or on logout
     */
    public void clear() {
        synchronized (lock) {
            ephemeralKey = null;
            serverNonce = null;
            handshakeTimestamp = 0;
            ephemeralTTL = 120; // Reset to default
            LogManager.getInstance().debug("Ephemeral key cache cleared");
        }
    }

    /**
     * Lifecycle hook: Called when app enters background
     * No action needed - key remains valid
     */
    public void handleAppDidEnterBackground() {
        // Key remains valid in background
        LogManager.getInstance().debug("App entered background - ephemeral key retained");
    }

    /**
     * Lifecycle hook: Called when app enters foreground
     * Check if renewal is needed after being backgrounded
     */
    public void handleAppWillEnterForeground() {
        synchronized (lock) {
            if (needsRenewal()) {
                LogManager.getInstance().info("App entered foreground - ephemeral key needs renewal");
            }
        }
    }

    /**
     * Lifecycle hook: Called when app will terminate
     * Clear ephemeral data on termination
     */
    public void handleAppWillTerminate() {
        clear();
        LogManager.getInstance().debug("App terminating - ephemeral key cleared");
    }
}
