package com.pushtechnology.client.sdk.example.serverconfiguration.systemauthenticationcontrol;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.session.AuthenticationException;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.Credentials;

public class AbstainAnonymousConnectionsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(AbstainAnonymousConnectionsExample.class);

    public static void main(String[] args) {

        final Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SystemAuthenticationControl adminControl =
            adminSession.feature(SystemAuthenticationControl.class);

        final SystemAuthenticationControl.ScriptBuilder builder =
            adminControl.scriptBuilder();

        adminControl.updateStore(
                builder
                    .abstainAnonymousConnections()
                    .script())
            .join();

        adminSession.close();

        final Session controlSession = Diffusion.sessions()
            .principal("control")
            .password("password")
            .open("ws://localhost:8080");

        controlSession.feature(AuthenticationControl.class)
            .setAuthenticationHandler("after-system-handler", new MyAuthenticator(LOG)).join();

        try {
            Diffusion.sessions().open("ws://localhost:8080");
        }
        catch (AuthenticationException e) {
            LOG.info("Anonymous connection denied: {}", e.getMessage());
        }

        controlSession.close();
    }

    static class MyAuthenticator implements AuthenticationControl.ControlAuthenticator {

        Logger logger;

        public MyAuthenticator(Logger logger) {
            this.logger = logger;
        }


        @Override
        public void authenticate(String principal, Credentials credentials,
            Map<String, String> sessionProperties,
            Map<String, String> proposedProperties,
            Callback callback) {

            if ("".equals(principal)) {
                logger.info("denying anonymous connection");
                callback.deny();

                return;
            }
            callback.allow();
        }

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
