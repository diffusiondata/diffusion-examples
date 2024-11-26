package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.Session;

public class AllowAnonymousConnectionsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(AllowAnonymousConnectionsExample.class);

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
                .allowAnonymousConnections(Collections.singleton("CLIENT"))
                .script())
            .join();

        final Session anonSession = Diffusion.sessions().open("ws://localhost:8080");

        LOG.info("Anon session state: {}", anonSession.getState());

        anonSession.close();
        session.close();
    }
}
