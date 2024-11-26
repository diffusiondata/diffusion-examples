package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.Session;

public class RemovePrincipalExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(RemovePrincipalExample.class);

    public static void main(String[] args) {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SystemAuthenticationControl authenticationControl =
            session.feature(SystemAuthenticationControl.class);

        final SystemAuthenticationControl.ScriptBuilder builder =
            authenticationControl.scriptBuilder();

        builder.addPrincipal(
            "super_user",
            "password12345",
            Collections.singleton("ADMINISTRATOR"));

        authenticationControl.updateStore(builder.script()).join();

        final Session superSession = Diffusion.sessions()
            .principal("super_user")
            .password("password12345")
            .open("ws://localhost:8080");

        LOG.info("connected as {}", superSession.getPrincipal());

        superSession.close();

        final SystemAuthenticationControl.ScriptBuilder newBuilder =
            authenticationControl.scriptBuilder();

        newBuilder.removePrincipal("super_user");

        authenticationControl.updateStore(newBuilder.script()).join();

        try {
            Diffusion.sessions()
                .principal("super_user")
                .password("password12345")
                .open("ws://localhost:8080");
        }
        catch (Exception e) {
            LOG.info("connection failed: {}", e.getMessage());
        }

        session.close();
    }
}
