package io.github.jbytheway.rideottawa.utils;

import android.content.Context;
import android.net.Uri;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;

import io.github.jbytheway.rideottawa.OcTranspoApi;
import io.github.jbytheway.rideottawa.R;

public class HttpUtils {
    public static class PostResult {
        PostResult(Integer responseCode, String response) {
            ResponseCode = responseCode;
            Response = response;
            Exception = null;
        }

        PostResult(Exception exception) {
            ResponseCode = null;
            Response = null;
            Exception = exception;
        }

        public final Integer ResponseCode;
        public final String Response;
        public final Exception Exception;
    }

    public static PostResult httpPost(Context context, String stringUrl, Map<String, String> params, Integer additionalCertId) {
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            throw new AssertionError("Malformed URL "+stringUrl);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();

            if (additionalCertId != null) {
                TrustUtils.trustAdditionalCert(context, conn, additionalCertId);
            }

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            Uri.Builder builder = new Uri.Builder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            String postContent = builder.build().getEncodedQuery();

            conn.setFixedLengthStreamingMode(postContent.length());

            OutputStream os = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
            writer.write(postContent);
            writer.close();

            conn.connect();

            int responseCode = conn.getResponseCode();

            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            String result = CharStreams.toString(isr);

            return new PostResult(responseCode, result);
        } catch (IOException e) {
            return new PostResult(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
