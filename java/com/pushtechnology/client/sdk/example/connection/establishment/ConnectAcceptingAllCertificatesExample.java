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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;

public class ConnectAcceptingAllCertificatesExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(ConnectAcceptingAllCertificatesExample.class);

    public static void main(String[] args) throws Exception {

        final SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(
            null,
            new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
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
