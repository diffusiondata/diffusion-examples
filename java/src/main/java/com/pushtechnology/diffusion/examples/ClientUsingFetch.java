/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.FetchContextStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.TopicSelector;

/**
 * This is a simple example of a client that fetches the state of topics but
 * does not subscribe to them.
 * <P>
 * This makes use of the 'Topics' feature only.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public final class ClientUsingFetch {

    private final Session session;
    private final Topics topics;

    /**
     * Constructor.
     */
    public ClientUsingFetch() {

        session =
            Diffusion.sessions().principal("client").password("password")
                .open("ws://diffusion.example.com:80");

        topics = session.feature(Topics.class);
    }

    /**
     * Issues a fetch request for a topic or selection of topics.
     *
     * @param topicSelector a {@link TopicSelector} expression
     * @param fetchContext context string to be returned with the fetch
     *        response(s)
     * @param stream callback for fetch responses
     */
    public void fetch(
        String topicSelector,
        String fetchContext,
        FetchContextStream<String> stream) {

        topics.fetch(topicSelector, fetchContext, stream);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
