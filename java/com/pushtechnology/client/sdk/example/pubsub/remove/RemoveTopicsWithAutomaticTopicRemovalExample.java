package com.pushtechnology.client.sdk.example.pubsub.remove;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddTopicResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

public class RemoveTopicsWithAutomaticTopicRemovalExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(RemoveTopicsWithAutomaticTopicRemovalExample.class);

    public static void main(String[] args) {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final TopicControl topicControl = session.feature(TopicControl.class);
            final TopicSpecification specification =
                Diffusion.newTopicSpecification(TopicType.JSON);
            AddTopicResult result;

            result = topicControl.addTopic("my/topic/path/to/be/removed/time/after",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when time after 'Tue, 4 May 2077 11:05:30 GMT'")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/subscriptions",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when subscriptions < 1 for 10m")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/local/subscriptions",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when local subscriptions < 1 for 10m")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/no/updates",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when no updates for 10m")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/no/session",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when no session has '$Principal is \"client\"' for 1h")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/no/local/session",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when no local session has 'Department is \"Accounts\"' for 1h after 1d")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/subcriptions/or/updates",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when subscriptions < 1 for 10m or no updates for 20m")).join();

            LOG.info(result.toString());

            result = topicControl.addTopic("my/topic/path/to/be/removed/subcriptions/and/updates",
                specification.withProperty(TopicSpecification.REMOVAL,
                        "when subscriptions < 1 for 10m and no updates for 20m")).join();

            LOG.info(result.toString());
        }
    }
}
