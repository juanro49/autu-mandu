<<<<<<< a7a91dd4ab86f0bd1a4879c4fd2c55350602a99c:app/src/main/java/me/kuehle/carreport/util/webdav/CertificateHelper.java
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

import android.content.Context;
import androidx.core.text.TextUtilsCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.internal.tls.OkHostnameVerifier;

public class CertificateHelper {
    public static String getShortDescription(X509Certificate certificate, Context context) {
        java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(context);

        X500PrincipalHelper sujectHelper = new X500PrincipalHelper(certificate.getSubjectX500Principal());
        String subject = sujectHelper.getCN();

        SortedSet<String> subjectAltNames = new TreeSet<>();
        subjectAltNames.addAll(OkHostnameVerifier.allSubjectAltNames(certificate));

        X500PrincipalHelper issuerHelper = new X500PrincipalHelper(certificate.getIssuerX500Principal());
        String issuer = issuerHelper.getCN();

        String serialNumber = certificate.getSerialNumber().toString(16).toUpperCase().replaceAll("(?<=..)(..)", ":$1");
        String validFrom = dateFormat.format(certificate.getNotBefore());
        String validUntil = dateFormat.format(certificate.getNotAfter());

        return "Subject: " + subject +
                "\nAlt. names: " + TextUtils.join(", ", subjectAltNames) +
                "\nSerialnumber: " + serialNumber +
                "\nIssuer: " + issuer +
                "\nValid: " + validFrom + " - " + validUntil;
    }

    public static String toString(X509Certificate certificate) throws CertificateEncodingException {
        String header = "-----BEGIN CERTIFICATE-----\n";
        String cert = Base64.encodeToString(certificate.getEncoded(), Base64.DEFAULT);
        String footer = "-----END CERTIFICATE-----";
        return header + cert + footer;
    }

    public static X509Certificate fromString(String certificate) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
    }

    public static SSLSocketFactory createSocketFactory(X509Certificate certificate) throws InvalidCertificateException {
        try {
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
            throw new InvalidCertificateException(certificate, e);
        }
    }
}
=======
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

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.internal.tls.OkHostnameVerifier;

public class CertificateHelper {
    public String getShortDescription(X509Certificate certificate, Context context) {
        java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(context);
        X500PrincipalHelper sujectHelper = new X500PrincipalHelper(certificate.getSubjectX500Principal());
        String subject = sujectHelper.getCN();

        SortedSet<String> subjectAltNames = new TreeSet<>();
        subjectAltNames.addAll(OkHostnameVerifier.INSTANCE.allSubjectAltNames(certificate));

        X500PrincipalHelper issuerHelper = new X500PrincipalHelper(certificate.getIssuerX500Principal());
        String issuer = issuerHelper.getCN();

        String serialNumber = certificate.getSerialNumber().toString(16).toUpperCase().replaceAll("(?<=..)(..)", ":$1");
        String validFrom = dateFormat.format(certificate.getNotBefore());
        String validUntil = dateFormat.format(certificate.getNotAfter());

        return "Subject: " + subject +
                "\nAlt. names: " + TextUtils.join(", ", subjectAltNames) +
                "\nSerialnumber: " + serialNumber +
                "\nIssuer: " + issuer +
                "\nValid: " + validFrom + " - " + validUntil;
    }

    public static String toString(X509Certificate certificate) throws CertificateEncodingException {
        String header = "-----BEGIN CERTIFICATE-----\n";
        String cert = Base64.encodeToString(certificate.getEncoded(), Base64.DEFAULT);
        String footer = "-----END CERTIFICATE-----";
        return header + cert + footer;
    }

    public static X509Certificate fromString(String certificate) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
    }

    public static SSLSocketFactory createSocketFactory(X509Certificate certificate) throws InvalidCertificateException {
        try {
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
            throw new InvalidCertificateException(certificate, e);
        }
    }
}
>>>>>>> Refactorización y mejoras en migración versiones BBDD:app/src/main/java/org/juanro/autumandu/util/webdav/CertificateHelper.java
