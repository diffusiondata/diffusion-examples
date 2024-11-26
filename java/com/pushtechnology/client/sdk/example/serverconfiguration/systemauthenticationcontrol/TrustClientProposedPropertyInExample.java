package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.Session;

public class TrustClientProposedPropertyInExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TrustClientProposedPropertyInExample.class);

    public static void main(String[] args) {

        final Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SystemAuthenticationControl authenticationControl =
            adminSession.feature(SystemAuthenticationControl.class);

        final Set<String> allowedValues = new HashSet<String>(){{
            add("Fred");
            add("Wilma");
            add("Pebbles");
        }};

        String updateScript = authenticationControl.scriptBuilder()
            .trustClientProposedPropertyIn("Flintstone", allowedValues)
            .script();

        authenticationControl.updateStore(updateScript).join();

        final Session sessionWithAllowedProp =
            Diffusion.sessions()
                .property("Flintstone", "Fred")
                .principal("admin")
                .password("password")
                .open("ws://localhost:8080");

        Map<String, String> properties;

        properties = adminSession.feature(ClientControl.class)
            .getSessionProperties(sessionWithAllowedProp.getSessionId(),
                Collections.singleton("Flintstone")).join();

        LOG.info("Session properties: {}", properties);

        final Session sessionWithNotAllowedProp =
            Diffusion.sessions()
                .property("Flintstone", "Barney")
                .principal("admin")
                .password("password")
                .open("ws://localhost:8080");

        properties = adminSession.feature(ClientControl.class)
            .getSessionProperties(sessionWithNotAllowedProp.getSessionId(),
                Collections.singleton("Flintstone")).join();

        LOG.info("Empty properties: {}", properties);

        sessionWithNotAllowedProp.close();
        sessionWithAllowedProp.close();
        adminSession.close();
    }
}
