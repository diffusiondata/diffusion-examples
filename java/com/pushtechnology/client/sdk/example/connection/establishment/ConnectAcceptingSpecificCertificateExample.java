/*******************************************************************************
 * Copyright (C) 2023 - 2026 DiffusionData Ltd.
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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This example demonstrates how to establish a secure connection while accepting a specific certificate.
 * <P>
 * A custom trust manager is implemented to verify the server certificate against the expected
 * certificate.
 * An SSL context is created and configured with the trust manager before opening a secure session.
 * @author DiffusionData Limited
 */
public class ConnectAcceptingSpecificCertificateExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(ConnectAcceptingSpecificCertificateExample.class);

    private static final byte[] EXPECTED_BYTES = "example".getBytes();
    
    public static void main(String[] args) throws Exception {

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            null,
            new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                        throws CertificateException {

                        if (certs == null || certs.length == 0) {
                            throw new CertificateException("Empty certificate chain");
                        }

                        final X509Certificate serverCert = certs[0];

                        serverCert.checkValidity();

                        if (!Arrays.equals(serverCert.getEncoded(), EXPECTED_BYTES)) {
                            // throw new CertificateException("Server certificate does not match expected certificate");
                        }
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            },
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
}