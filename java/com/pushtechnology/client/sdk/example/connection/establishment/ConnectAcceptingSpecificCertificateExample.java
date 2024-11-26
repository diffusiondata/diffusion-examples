/*******************************************************************************
 * Copyright (C) 2023 - 2024 DiffusionData Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pushtechnology.client.sdk.example.connection.establishment;

import static java.nio.file.Files.newInputStream;

import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;

public class ConnectAcceptingSpecificCertificateExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(ConnectAcceptingSpecificCertificateExample.class);

    public static void main(String[] args) throws Exception {

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            null,
            getTrustManagers(),
            null);

        final Session session = Diffusion.sessions()
            .secureTransport(true)
            .sslContext(sslContext)
            .principal("admin")
            .password("password")
            .open("wss://localhost:8080");

        LOG.info("Connected, session identifier: '{}'.", session.getSessionId());

        // Insert work here

        session.close();
    }

    private static TrustManager[] getTrustManagers() throws Exception {
        try (InputStream is =
            newInputStream(Paths.get("src/main/resources/cert.crt"))) {

            final X509Certificate certificate = (X509Certificate)
                CertificateFactory.getInstance("X.509").generateCertificate(is);

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("caCert", certificate);
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        }
    }
}
