package io.github.jbytheway.rideottawa.utils;

import android.content.Context;

import com.google.common.collect.Iterables;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.github.jbytheway.rideottawa.R;

public class TrustUtils {
    private static class CompositeX509TrustManager implements X509TrustManager {
        private final X509TrustManager[] mTrustManagers;

        public CompositeX509TrustManager(X509TrustManager[] trustManagers) {
            mTrustManagers = trustManagers;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            String errors = "";
            for (X509TrustManager trustManager : mTrustManagers) {
                try {
                    trustManager.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    errors += "\n" + e.getMessage();
                }
            }
            throw new CertificateException("No TrustManager trusted; errors:" + errors);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            String errors = "";
            for (X509TrustManager trustManager : mTrustManagers) {
                try {
                    trustManager.checkClientTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    errors += "\n" + e.getMessage();
                }
            }
            throw new CertificateException("No TrustManager trusted; errors:" + errors);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            ArrayList<X509Certificate> certificates = new ArrayList<>();
            for (X509TrustManager trustManager : mTrustManagers) {
                certificates.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
            }
            return Iterables.toArray(certificates, X509Certificate.class);
        }
    }

    private static X509TrustManager getTrustManager(String algorithm, KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
        factory.init(keystore);
        return Iterables.getFirst(Iterables.filter(
                Arrays.asList(factory.getTrustManagers()), X509TrustManager.class), null);
    }

    private static Pair<SSLContext, TrustManager[]> getSslContext(Context context, int certResourceId) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca;

            try (InputStream caInput = context.getResources().openRawResource(R.raw.globalsign_dv_ca_g2)) {
                ca = cf.generateCertificate(caInput);
            }
            // Create a KeyStore containing the given CA
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            // Create a default TrustManager
            X509TrustManager defaultTrustManager = getTrustManager(tmfAlgorithm, null);
            // Create a TrustManager that trusts the CA given
            X509TrustManager ourTrustManager = getTrustManager(tmfAlgorithm, keyStore);

            X509TrustManager[] trustManagers = new X509TrustManager[] { defaultTrustManager, ourTrustManager };
            TrustManager[] compositeTrustManager = new TrustManager[] { new CompositeX509TrustManager(trustManagers) };

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, compositeTrustManager, null);

            return new ImmutablePair<>(sslContext, compositeTrustManager);
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new AssertionError("Failed to trust additional cert", e);
        }
    }

    public static void trustAdditionalCert(Context context, HttpURLConnection conn, int certResourceId) {
        Pair<SSLContext, TrustManager[]> p = getSslContext(context, certResourceId);
        ((HttpsURLConnection)conn).setSSLSocketFactory(p.getLeft().getSocketFactory());
    }
}
