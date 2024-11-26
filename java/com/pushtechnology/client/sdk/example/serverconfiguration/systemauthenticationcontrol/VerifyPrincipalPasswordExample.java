package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.Session;

public class VerifyPrincipalPasswordExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(VerifyPrincipalPasswordExample.class);

    public static void main(String[] args) {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SystemAuthenticationControl authenticationControl =
            session.feature(SystemAuthenticationControl.class);

        String updateScript = authenticationControl.scriptBuilder()
            .verifyPassword("control", "password")
            .setPassword("control", "12345")
            .script();

        authenticationControl.updateStore(updateScript).join();

        final Session controlSession = Diffusion.sessions()
            .principal("control")
            .password("12345")
            .open("ws://localhost:8080");

        LOG.info("connected as {}", controlSession.getPrincipal());

        controlSession.close();

        updateScript = authenticationControl.scriptBuilder()
            .verifyPassword("control", "this_is_not_the_right_password")
            .setPassword("control", "new_password")
            .script();

        try {
            authenticationControl.updateStore(updateScript).join();
        }
        catch (Exception e) {
            LOG.info("Password was not updated: {}", e.getMessage());
        }

        try {
            Diffusion.sessions()
                .principal("control")
                .password("new_password")
                .open("ws://localhost:8080");
        }
        catch (Exception e) {
            LOG.info("Authentication failed: {}", e.getMessage());
        }

        session.close();
    }
}
