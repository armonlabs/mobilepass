package com.armongate.mobilepasssdk.service;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.CryptoManager;
import com.armongate.mobilepasssdk.manager.HandshakeManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.response.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class BaseService {

    // Listener defined earlier
    public interface ServiceResultListener<T> {
        void onCompleted(T result);

        void onError(int errorCode, String message);
    }

    // Singleton

    private static BaseService instance = null;

    private BaseService() {
    }

    public static BaseService getInstance() {
        if (instance == null) {
            instance = new BaseService();
        }

        return instance;
    }

    // Private Fields

    private RequestQueue requestQueue;
    private Context activeContext;
    private String cachedUserAgent = null;

    private static final String METHOD_POST = "Post";
    private static final String METHOD_GET = "Get";

    // Public Functions

    public void setContext(Context context) {
        this.activeContext = context;
    }


    public <K> void requestGet(String url, @Nullable Class<K> clazz, final BaseService.ServiceResultListener<K> listener) {
        if (requiresHandshake(url)) {
            ensureHandshake(url, METHOD_GET, null, listener, clazz);
        } else {
            this.request(METHOD_GET, url, null, listener, clazz);
        }
    }

    public <T, K> void requestPost(String url, T data, @Nullable Class<K> clazz, final BaseService.ServiceResultListener<K> listener) {
        try {
            Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
            String jsonInString = gson.toJson(data);
            
            if (requiresHandshake(url)) {
                ensureHandshake(url, METHOD_POST, jsonInString, listener, clazz);
            } else {
                this.request(METHOD_POST, url, jsonInString, listener, clazz);
            }
        } catch (Exception ex) {
            listener.onError(-1, "");
        }
    }

    // Private Functions

    private String getUserAgent() {
        if (cachedUserAgent != null) {
            return cachedUserAgent;
        }
        
        try {
            String sdkVersion = "2.0.1-rc.1";
            String osVersion = "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")";
            
            // Safe access to potentially null device info with fallbacks
            String manufacturer = android.os.Build.MANUFACTURER != null ? android.os.Build.MANUFACTURER : "Unknown";
            String model = android.os.Build.MODEL != null ? android.os.Build.MODEL : "Unknown";
            String deviceInfo = manufacturer + " " + model;
            
            // Safe locale access with fallback
            java.util.Locale currentLocale = java.util.Locale.getDefault();
            String locale = currentLocale != null ? currentLocale.toString() : "en_US";
            
            // Sanitize to ensure HTTP header compliance (remove control characters and newlines)
            osVersion = osVersion.replaceAll("[\\p{Cntrl}]", "");
            deviceInfo = deviceInfo.replaceAll("[\\p{Cntrl}]", "");
            locale = locale.replaceAll("[\\p{Cntrl}]", "");
            
            cachedUserAgent = String.format("MobilePassSDK/%s (%s; %s; %s)", 
                sdkVersion, osVersion, deviceInfo, locale);
        } catch (Exception ex) {
            // Fallback to basic User-Agent if any exception occurs
            LogManager.getInstance().warn("Failed to build detailed User-Agent, using fallback", null);
            cachedUserAgent = "MobilePassSDK/2.0.1-rc.1";
        }
        
        return cachedUserAgent;
    }

    private boolean requiresHandshake(String url) {
        // Skip handshake for the handshake endpoint itself
        return url != null && !url.contains("/sdk/handshake");
    }

    private <T> void ensureHandshake(final String url, final String method, final String jsonString,
                                     final BaseService.ServiceResultListener<T> listener, @Nullable final Class<T> clazz) {
        HandshakeManager.getInstance().ensureHandshake(new HandshakeManager.HandshakeCompletionListener() {
            @Override
            public void onHandshakeCompleted(boolean success, String error) {
                if (success) {
                    request(method, url, jsonString, listener, clazz);
                } else {
                    LogManager.getInstance().error("Handshake failed, cannot proceed with request: " + error, null);
                    listener.onError(401, error != null ? error : "Handshake failed");
                }
            }
        });
    }

    private boolean addSignatureHeaders(Map<String, String> params, String method, String url, String jsonBody) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            
            String bodyHash;
            if (jsonBody == null || jsonBody.isEmpty()) {
                bodyHash = CryptoManager.getInstance().sha256Hex("");
            } else {
                bodyHash = CryptoManager.getInstance().sha256Hex(jsonBody);
            }
            
            String path = url;
            
            if (path.startsWith("api/v1/")) {
                path = path.substring(7);
            } else if (path.startsWith("api/v2/")) {
                path = path.substring(7);
            }
            
            String signature = HandshakeManager.getInstance().signRequest(method, path, timestamp, bodyHash);
            
            if (signature == null) {
                LogManager.getInstance().error("Failed to sign request - signature is null", null);
                return false;
            }

            params.put("mobilepass-signature", signature);
            params.put("mobilepass-timestamp", String.valueOf(timestamp));
            
            LogManager.getInstance().debug("Request signed successfully");
            return true;
            
        } catch (Exception ex) {
            LogManager.getInstance().error("Failed to add signature headers: " + ex.getLocalizedMessage(), null);
            return false;
        }
    }

    private <T> void request(String method, String url, String jsonString, final BaseService.ServiceResultListener<T> listener, @Nullable final Class<T> clazz) {
        nuke();

        String serverUrl = ConfigurationManager.getInstance().getServerURL() + url;

        LogManager.getInstance().debug("New request to " + serverUrl);

        final String jsonBody = jsonString;

        StringRequest stringRequest = new StringRequest
                (method.equals(METHOD_GET) ? Request.Method.GET : Request.Method.POST, serverUrl, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        if (clazz != null) {
                            Gson gson = new Gson();
                            listener.onCompleted(gson.fromJson(response, clazz));
                        } else {
                            listener.onCompleted(null);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LogManager.getInstance().error("Error received " + (error.networkResponse != null ? error.networkResponse.statusCode : "NoNetwork"), null);

                        String message = "";

                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Gson gson = new Gson();
                                ResponseMessage responseMsg = gson.fromJson(new String(error.networkResponse.data), ResponseMessage.class);

                                if (responseMsg != null && responseMsg.message != null) {
                                    message = responseMsg.message;
                                }
                            } catch (Exception ex) {
                                LogManager.getInstance().error(ex.getMessage() != null ? ex.getMessage() : "Failed to parse error response", null);
                            }
                        }

                        if (error.networkResponse == null) {
                            listener.onError(0, message);
                        } else if (error instanceof TimeoutError) {
                            listener.onError(408, message);
                        } else if (error instanceof AuthFailureError) {
                            // Return actual status code if available, otherwise 401
                            int statusCode = error.networkResponse.statusCode;
                            listener.onError(statusCode, message);
                        } else {
                            listener.onError(error.networkResponse.statusCode, message);
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                params.put("If-Modified-Since", "Mon, 26 Jul 1997 05:00:00 GMT");
                params.put("Cache-Control", "no-cache");
                params.put("User-Agent", getUserAgent());
                params.put("accept-language", ConfigurationManager.getInstance().getLanguage());
                params.put("mobilepass-version", "2.0.1-rc.1");
                params.put("mobilepass-memberid", ConfigurationManager.getInstance().getMemberId());
                params.put("mobilepass-barcode", ConfigurationManager.getInstance().getBarcodeId());
                params.put("mobilepass-config", ConfigurationManager.getInstance().getConfigurationLog());

                if (requiresHandshake(url)) {
                    boolean signed = addSignatureHeaders(params, method, url, jsonBody);
                    if (!signed) {
                        LogManager.getInstance().error("Cannot proceed with unsigned request", null);
                        throw new AuthFailureError("Request signing failed");
                    }
                }

                return params;
            }
            
            @Override
            public byte[] getBody() throws AuthFailureError {
                if (jsonBody != null) {
                    return jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                return null;
            }
        };

        this.addToRequestQueue(stringRequest);
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(activeContext.getApplicationContext());
        }

        return requestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 0) {
            @Override
            public void retry(VolleyError error) throws VolleyError {
                if (error instanceof AuthFailureError) {
                    throw error;
                }
                super.retry(error);
            }
        });
        getRequestQueue().add(req);
    }


    public static void nuke() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
    }
}


