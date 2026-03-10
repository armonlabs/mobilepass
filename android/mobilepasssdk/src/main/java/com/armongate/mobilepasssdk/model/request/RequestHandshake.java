package com.armongate.mobilepasssdk.model.request;

/**
 * Request model for handshake endpoint
 * Used to establish ephemeral keys for secure request signing
 */
public class RequestHandshake {
    /**
     * Member ID identifying the user
     */
    public String memberId;

    /**
     * Current timestamp in seconds (Unix epoch)
     */
    public long timestamp;

    /**
     * Client-generated ephemeral key (32 bytes, base64-encoded)
     */
    public String ephemeralKey;

    /**
     * HMAC-SHA256 signature: HMAC(STATIC_KEY, memberId + timestamp + ephemeralKey)
     */
    public String signature;
}
