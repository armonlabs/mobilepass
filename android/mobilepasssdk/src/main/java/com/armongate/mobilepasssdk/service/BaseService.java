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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.response.ResponseMessage;
import com.google.gson.Gson;

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

    private static final String METHOD_POST = "Post";
    private static final String METHOD_GET = "Get";

    // Public Functions

    public void setContext(Context context) {
        this.activeContext = context;
    }


    public <K> void requestGet(String url, @Nullable Class<K> clazz, final BaseService.ServiceResultListener<K> listener) {
        this.request(METHOD_GET, url, null, listener, clazz);
    }

    public <T, K> void requestPost(String url, T data, @Nullable Class<K> clazz, final BaseService.ServiceResultListener<K> listener) {
        try {
            String jsonInString = new Gson().toJson(data);
            this.request(METHOD_POST, url, new JSONObject(jsonInString), listener, clazz);
        } catch (Exception ex) {
            listener.onError(-1, "");
        }
    }

    // Private Functions

    private <T> void request(String method, String url, JSONObject data, final BaseService.ServiceResultListener<T> listener, @Nullable final Class<T> clazz) {
        // TODO Remove
        nuke();

        String serverUrl = ConfigurationManager.getInstance().getServerURL() + url;

        LogManager.getInstance().debug("New request to " + serverUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (method.equals(METHOD_GET) ? Request.Method.GET : Request.Method.POST, serverUrl, data, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // LogManager.getInstance().debug("Response received > " + response.toString());

                        if (clazz != null) {
                            Gson gson = new Gson();
                            listener.onCompleted(gson.fromJson(response.toString(), clazz));
                        } else {
                            listener.onCompleted(null);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LogManager.getInstance().info("Error received " + (error.networkResponse != null ? error.networkResponse.statusCode : "NoNetwork"));

                        String message = "";

                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                LogManager.getInstance().info(new String(error.networkResponse.data));

                                Gson gson = new Gson();
                                ResponseMessage responseMsg = gson.fromJson(new String(error.networkResponse.data), ResponseMessage.class);

                                message = responseMsg.message;
                            } catch (Exception ex) {
                                LogManager.getInstance().error(ex.getMessage(), null);
                            }
                        }

                        if (error.networkResponse == null) {
                            listener.onError(0, message);
                        } else if (error instanceof TimeoutError) {
                            listener.onError(408, message);
                        } else if (error instanceof AuthFailureError) {
                            listener.onError(401, message);
                        } else {
                            listener.onError(error.networkResponse.statusCode, message);
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = ConfigurationManager.getInstance().getToken();

                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                params.put("Authorization", token);
                params.put("If-Modified-Since", "Mon, 26 Jul 1997 05:00:00 GMT");
                params.put("Cache-Control", "no-cache");
                params.put("mobilepass-version", "1.6.5");
                params.put("mobilepass-memberid", ConfigurationManager.getInstance().getMemberId());
                params.put("mobilepass-config", ConfigurationManager.getInstance().getConfigurationLog());
                params.put("mobilepass-provider", ConfigurationManager.getInstance().getServiceProvider());

                return params;
            }
        };

        this.addToRequestQueue(jsonObjectRequest);
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
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 0));
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

