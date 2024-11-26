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
package com.pushtechnology.client.sdk.example.sessiontrees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.SessionTrees;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

public class SessionTreesUseCaseExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SessionTreesUseCaseExample.class);

    public static void main(String[] args) {

        try (Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topicsAdminSession = adminSession.feature(Topics.class);
            final TopicSpecification topicSpecification = Diffusion.newTopicSpecification(TopicType.STRING);

            topicsAdminSession.addAndSet("my/topic/path/for/admin", topicSpecification, String.class, "Good morning Administrator")
                .join();

            topicsAdminSession.addAndSet("my/topic/path/for/control", topicSpecification, String.class, "Good afternoon Control Client")
                .join();

            topicsAdminSession.addAndSet("my/topic/path/for/anonymous", topicSpecification, String.class, "Good night Anonymous")
                .join();

            final SessionTrees.BranchMappingTable branchMappingTable = Diffusion.newBranchMappingTableBuilder()
                .addBranchMapping("$Principal is 'admin'", "my/topic/path/for/admin")
                .addBranchMapping("$Principal is 'control'", "my/topic/path/for/control")
                .addBranchMapping("$Principal is ''", "my/topic/path/for/anonymous")
                .create("my/personal/path");

            adminSession.feature(SessionTrees.class).putBranchMappingTable(branchMappingTable)
                .join();

            LOG.info("Session tree mappings added.");

            final MyLoggingStringStream myLoggingStringStreamAdminSession = new MyLoggingStringStream(adminSession.getPrincipal());

            topicsAdminSession.addStream(">my/personal/path", String.class, myLoggingStringStreamAdminSession);
            topicsAdminSession.subscribe(">my/personal/path")
                .join();

            final Session anonymousSession = Diffusion.sessions()
                .principal("").open("ws://localhost:8080");

            final Topics topicsAnonymousSession = anonymousSession.feature(Topics.class);

            final MyLoggingStringStream myLoggingStringStreamAnonymousSession = new MyLoggingStringStream(anonymousSession.getPrincipal());

            topicsAnonymousSession.addStream(">my/personal/path", String.class, myLoggingStringStreamAnonymousSession);
            topicsAnonymousSession.subscribe(">my/personal/path")
                .join();

            topicsAdminSession.removeStream(myLoggingStringStreamAdminSession);
            topicsAnonymousSession.removeStream(myLoggingStringStreamAnonymousSession);

            topicsAdminSession.listSessionTreeBranchesWithMappings()
                .join()
                .forEach(sessionTreeBranch -> {
                    LOG.info("{}:", sessionTreeBranch);
                });

            adminSession.close();
            anonymousSession.close();
        }
    }

    public static class MyLoggingStringStream implements Topics.ValueStream<String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyLoggingStringStream.class);

        private final String principal;

        public MyLoggingStringStream(String principal) {
            this.principal = principal;
        }

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            String oldValue,
            String newValue) {

            LOG.info("Principal: '{}' - '{}' changed from '{}' to '{}}'.",
                principal, topicPath, oldValue, newValue);
        }

        @Override
        public void onSubscription(
            String topicPath,
            TopicSpecification topicSpecification) {

            LOG.info("Principal: '{}' - subscribed to: '{}'.", principal, topicPath);
        }

        @Override
        public void onUnsubscription(
            String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {

            LOG.info("Principal: '{}' - unsubscribed from: '{}', reason: {}.",
                principal, topicPath, unsubscribeReason);
        }

        @Override
        public void onClose() {
            LOG.info("Principal: '{}' - on close.", principal);
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("Principal: '{}' - on error: {}.", principal, errorReason);
        }
    }
}
