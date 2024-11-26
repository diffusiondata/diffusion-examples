package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.AuthenticationException;
import com.pushtechnology.diffusion.client.session.Session;

public class DenyAnonymousConnectionsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(DenyAnonymousConnectionsExample.class);

    public static void main(String[] args) {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SystemAuthenticationControl authenticationControl =
            session.feature(SystemAuthenticationControl.class);

        final SystemAuthenticationControl.ScriptBuilder builder =
            authenticationControl.scriptBuilder();

        authenticationControl.updateStore(
                builder
                    .denyAnonymousConnections()
                    .script())
            .join();

        try {
            Diffusion.sessions().open("ws://localhost:8080");
        }
        catch (AuthenticationException e) {
            LOG.info("Anonymous connections denied: {}", e.getMessage());
        }

        session.close();
    }
}
