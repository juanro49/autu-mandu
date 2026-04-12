/*
 * Copyright 2016 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.util.webdav;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509TrustManager;

import org.juanro.autumandu.util.FileCopyUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WebDavClient {
    private static final String TAG = "WebDavClient";

    /**
     * SimpleDateFormat is not thread-safe. ThreadLocal is used to provide thread-safety
     * while maintaining compatibility with API 25 (java.time requires API 26).
     */
    private static final ThreadLocal<DateFormat> MODIFICATION_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf;
    });

    private final OkHttpClient client;
    private final Uri baseUri;

    public WebDavClient(String baseUrl, String userName, String password, final X509Certificate trustedCertificate) throws InvalidCertificateException {
        // Base URL needs to point to a directory.
        String finalBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.baseUri = Uri.parse(finalBaseUrl);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (userName != null && password != null) {
            builder.authenticator(new BasicDigestAuthenticator(baseUri.getHost(), userName, password));
        }

        if (trustedCertificate != null) {
            @SuppressLint("CustomX509TrustManager")
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                @SuppressLint("TrustAllX509TrustManager")
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    if (chain == null || chain.length == 0 || !chain[0].equals(trustedCertificate)) {
                        throw new java.security.cert.CertificateException("Certificate not trusted");
                    }
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{trustedCertificate};
                }
            };

            builder.sslSocketFactory(CertificateHelper.createSocketFactory(trustedCertificate), trustManager);
            builder.hostnameVerifier((hostname, session) -> {
                try {
                    X509Certificate certificate = (X509Certificate) session.getPeerCertificates()[0];
                    return certificate.equals(trustedCertificate);
                } catch (SSLException e) {
                    return false;
                }
            });
        }

        this.client = builder.build();
    }

    public void download(String remoteFilepath, File targetFile) throws HttpException, IOException {
        Request request = new Request.Builder()
                .url(baseUri.buildUpon().appendPath(remoteFilepath).toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(response);
            }
            ResponseBody body = response.body();
            if (body != null) {
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                    FileCopyUtil.copyFile(body.byteStream(), bos);
                }
            }
        } catch (IOException e) {
            throw new HttpException(request, e);
        }
    }

    public void upload(File localFile, String remoteFilepath, String contentType) throws HttpException {
        MediaType mediaType = MediaType.parse(contentType);
        RequestBody body = RequestBody.create(localFile, mediaType);
        Request request = new Request.Builder()
                .url(baseUri.buildUpon().appendPath(remoteFilepath).toString())
                .put(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(response);
            }
        } catch (IOException e) {
            throw new HttpException(request, e);
        }
    }

    public Date getLastModified(String remoteFilepath) throws HttpException {
        Request request = new Request.Builder()
                .url(baseUri.buildUpon().appendPath(remoteFilepath).toString())
                .head()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String lastModified = response.header("Last-Modified");
                DateFormat format = MODIFICATION_DATE_FORMAT.get();
                if (lastModified != null && format != null) {
                    return format.parse(lastModified);
                } else {
                    throw new IOException("Server did not return a Last-Modified header.");
                }
            } else {
                throw new HttpException(response);
            }
        } catch (IOException | ParseException e) {
            throw new HttpException(request, e);
        }
    }

    public void testLogin() throws HttpException, UntrustedCertificateException {
        Request request = new Request.Builder()
                .url(baseUri.toString())
                .head()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(response);
            }
        } catch (SSLHandshakeException e) {
            handleSslException(request, e);
        } catch (IOException e) {
            throw new HttpException(request, e);
        }
    }

    private void handleSslException(Request request, SSLHandshakeException e) throws UntrustedCertificateException, HttpException {
        Throwable innerEx = e;
        while (innerEx != null && !(innerEx instanceof CertPathValidatorException)) {
            innerEx = innerEx.getCause();
        }

        if (innerEx instanceof CertPathValidatorException certEx) {
            X509Certificate cert = null;
            try {
                var certificates = certEx.getCertPath().getCertificates();
                if (!certificates.isEmpty()) {
                    cert = (X509Certificate) certificates.get(0);
                }
            } catch (Exception e2) {
                Log.e(TAG, "Error extracting certificate..", e2);
            }

            if (cert != null) {
                throw new UntrustedCertificateException(cert);
            }
        }
        throw new HttpException(request, (e != null) ? e : new IOException("SSL Handshake failed"));
    }
}
