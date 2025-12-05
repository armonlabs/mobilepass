package com.armongate.mobilepasssdk.model.response;

/**
 * Response model for handshake endpoint
 * Contains server-generated nonce for request signing
 */
public class ResponseHandshake {
    /**
     * Indicates if handshake was successful
     */
    public boolean ok;

    /**
     * Server-generated nonce used in all request signatures during this session
     */
    public String serverNonce;

    /**
     * Time-to-live for ephemeral key in seconds (e.g., 120)
     * Key should be renewed after this period
     */
    public Long ephemeralTTL;

    /**
     * Error message if handshake failed
     */
    public String error;
}
