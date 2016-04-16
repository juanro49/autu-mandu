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
package me.kuehle.carreport.util.webdav;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.SSLHandshakeException;

import me.kuehle.carreport.util.FileCopyUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebDavClient {
    private static final String TAG = "WebDavClient";

    private OkHttpClient mClient;
    private Uri mBaseUri;

    public WebDavClient(String baseUrl, String userName, String password, X509Certificate trustedCertificate) throws InvalidCertificateException {
        // Base URL needs to point to a directory.
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        mBaseUri = Uri.parse(baseUrl);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (userName != null && password != null) {
            builder.authenticator(new BasicDigestAuthenticator(mBaseUri.getHost(), userName, password));
        }

        if (trustedCertificate != null) {
            builder.sslSocketFactory(CertificateHelper.createSocketFactory(trustedCertificate));
        }

        mClient = builder.build();
    }

    public boolean download(String remoteFilepath, File targetFile) throws IOException {
        Request request = new Request.Builder()
                .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
                .build();
        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            return false;
        }

        FileOutputStream fos = new FileOutputStream(targetFile);
        FileCopyUtil.copyFile(response.body().byteStream(), fos);
        fos.close();
        return true;
    }

    public boolean upload(File localFile, String remoteFilepath, String contentType) throws IOException {
        RequestBody body = RequestBody.create(MediaType.parse(contentType), localFile);
        Request request = new Request.Builder()
                .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
                .put(body)
                .build();
        Response response = mClient.newCall(request).execute();
        return response.isSuccessful();
    }

    public Date getLastModified(String remoteFilepath) throws Exception {
        String bodyText = "<?xml version=\"1.0\"?>\n" +
                "<a:propfind xmlns:a=\"DAV:\">\n" +
                "    <a:prop>\n" +
                "        <a:getlastmodified/>\n" +
                "    </a:prop>\n" +
                "</a:propfind>";
        RequestBody body = RequestBody.create(MediaType.parse("text/xml"), bodyText);
        Request request = new Request.Builder()
                .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
                .method("PROPFIND", body)
                .addHeader("Depth", "0")
                .build();
        Response response = mClient.newCall(request).execute();
        if (response.code() == 207) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(response.body().charStream());
            parser.nextTag();

            DavMultiStatus multiStatus = DavMultiStatus.read(parser);
            if (multiStatus.getResponses().length != 1 ||
                    multiStatus.getResponses()[0].getPropertySets().length != 1 ||
                    multiStatus.getResponses()[0].getPropertySets()[0].getStatus() != 200) {
                throw new Exception("Multi status response does not contain exactly one response.");
            }

            return multiStatus.getResponses()[0].getPropertySets()[0].getLastModified();
        }

        return null;
    }

    public boolean testLogin() throws UntrustedCertificateException {
        try {
            Request request = new Request.Builder()
                    .url(mBaseUri.toString())
                    .head()
                    .build();
            Response response = mClient.newCall(request).execute();
            return response.isSuccessful();
        } catch (SSLHandshakeException e) {
            Throwable innerEx = e;
            while (innerEx != null && !(innerEx instanceof CertPathValidatorException)) {
                innerEx = innerEx.getCause();
            }

            if (innerEx != null) {
                X509Certificate cert = null;
                try {
                    cert = (X509Certificate) ((CertPathValidatorException) innerEx)
                            .getCertPath()
                            .getCertificates()
                            .get(0);
                } catch (Exception e2) {
                    Log.e(TAG, "Error extracting certificate..", e2);
                }

                if (cert != null) {
                    throw new UntrustedCertificateException(cert);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing login.", e);
        }

        return false;
    }
}
