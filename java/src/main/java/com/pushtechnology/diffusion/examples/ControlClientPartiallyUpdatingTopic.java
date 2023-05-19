/*******************************************************************************
 * Copyright (C) 2019 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.TopicUpdate.JsonPatchResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This example demonstrates the use of partial update. A JSON patch is used to
 * update the existing value of the topic by adding a new key-value pair.
 * <p>
 * The 'TopicUpdate' feature provides this functionality. The patch is provided
 * as a String that is parsed on the server. A JSON patch is a JSON document
 * containing a list of operations: add, replace, copy, move, remove and test.
 * <p>
 * To perform a partial update the topic must have a JSON value and the client
 * must have the 'update_topic' permission for the path.
 *
 * @author DiffusionData Limited
 * @since 6.4
 * @see <a href="https://tools.ietf.org/html/rfc6902">JavaScript Object Notation (JSON) Patch</a>
 */

public final class ControlClientPartiallyUpdatingTopic {

    private static final JSONDataType JSON_DATATYPE = Diffusion.dataTypes().json();

    private final Session session;
    private final TopicUpdate topicUpdate;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientPartiallyUpdatingTopic(String serverUrl) {

        // 1. Connect to the Diffusion endpoint.
        session = Diffusion.sessions().principal("control").password("password")
            .open(serverUrl);

        // 2. Access the TopicUpdate feature.
        topicUpdate = session.feature(TopicUpdate.class);
    }

    /**
     * Patches can have multiple operations which are applied incrementally and
     * atomically.
     *
     * @param topicPath the path of the topic
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void applySimpleJsonPatch(String topicPath)
        throws ExecutionException, InterruptedException, TimeoutException {

        // 1. Create JSON topic specification.
        final TopicSpecification specification =
            Diffusion.newTopicSpecification(TopicType.JSON);

        // 2. Set the initial value to an empty JSON object.
        final JSON initialValue = JSON_DATATYPE.fromJsonString("{}");
        topicUpdate.addAndSet(topicPath, specification, JSON.class, initialValue).get(5, TimeUnit.SECONDS);

        // 3. The patch [{"op":"add", "path":"/array","value":"[0, 1, 2]"}] will
        // add the array at the designated path changing the value to
        // {"array" : [0, 1, 2]}.
        final String addArrayPatch = "[{\"op\": \"add\", \"path\": \"/array\", \"value\": [0, 1, 2]}]";

        // 4. Apply the patch.
        final JsonPatchResult result =
            topicUpdate.applyJsonPatch(topicPath, addArrayPatch).get(5, TimeUnit.SECONDS);

        // 5. Check for failure.
        result.failedOperation().ifPresent(failedOperation -> {
            // If an operation in the JSON patch did not apply to the value, the
            // Integer failedOperation will be the index of the operation in the
            // JSON patch that failed and can be handled accordingly.
        });
    }
}