package com.matburt.mobileorg;

/*
 * Code from Nagroid:
 * http://code.google.com/p/nagroid/source/browse/src/de/schoar/android/helper/http/SSLSelfSigned.java
 */

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import android.util.Log;

public final class SSLHandler {
        private static final String LOGT = "SSLHandler";

        private static SSLHandler INSTANCE = null;

        public static SSLHandler gi() {
                if (INSTANCE == null) {
                        INSTANCE = new SSLHandler();
                }
                return INSTANCE;
        }

        private HostnameVerifier mDefaultHostnameVerifier = null;
        private SSLSocketFactory mDefaultSSLSocketFactory = null;
        private HostnameVerifier mSelfSignedHostnameVerifier = null;
        private SSLSocketFactory mSelfSignedSSLSocketFactory = null;

        private SSLHandler() {
                init();
        }

        public void disable() {
                Log.d(LOGT, "Disabled");
                HttpsURLConnection.setDefaultHostnameVerifier(mDefaultHostnameVerifier);
                HttpsURLConnection.setDefaultSSLSocketFactory(mDefaultSSLSocketFactory);
        }

        public void enable() {
                Log.d(LOGT, "Enabled");
                HttpsURLConnection
                                .setDefaultHostnameVerifier(mSelfSignedHostnameVerifier);
                HttpsURLConnection
                                .setDefaultSSLSocketFactory(mSelfSignedSSLSocketFactory);
        }

        private void init() {
                mDefaultHostnameVerifier = HttpsURLConnection
                                .getDefaultHostnameVerifier();
                mDefaultSSLSocketFactory = HttpsURLConnection
                                .getDefaultSSLSocketFactory();

                mSelfSignedHostnameVerifier = new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                                return true;
                        }
                };

                try {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null,
                                        new TrustManager[] { new AcceptAllX509TrustManager() },
                                        new SecureRandom());
                        mSelfSignedSSLSocketFactory = sslContext.getSocketFactory();
                } catch (Exception e) {
                }
        }

        // ----

        private static class AcceptAllX509TrustManager implements
                        javax.net.ssl.X509TrustManager {
                private static final X509Certificate[] AcceptAllIssuers = new X509Certificate[] {};

                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                                throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                                throws CertificateException {
                }

                public boolean isClientTrusted(X509Certificate[] chain) {
                        return (true);
                }

                public boolean isServerTrusted(X509Certificate[] chain) {
                        return (true);
                }

                public X509Certificate[] getAcceptedIssuers() {
                        return (AcceptAllIssuers);
                }
        }
}