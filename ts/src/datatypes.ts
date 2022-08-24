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

import { connect, datatypes, topics, Session } from 'diffusion';

// example showcasing the diffusion data types
export async function datatypesExample() {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    // 1. Data Types are exposed from the top level Diffusion namespace. It is often easier
    // to assign these directly to a local variable.
    const stringDataType = datatypes.string();
    const jsonDataType = datatypes.json();
    const TopicSpecification = topics.TopicSpecification;
    const TopicType = topics.TopicType;

    // 2. Data Types are currently provided for JSON, Binary, String, Double, Int64 and RecordV2 topic types.
    await Promise.all([
        session.topics.add('topic/string', new TopicSpecification(TopicType.STRING)),
        session.topics.add('topic/json', new TopicSpecification(TopicType.JSON))
    ]);

    // 3. Values can be created directly from the data type.
    const jsonValue = jsonDataType.from({
        "foo" : "bar"
    });

    await Promise.all([
        // Topics are updated using the standard update mechanisms
        session.topics.updateValue('topic/json', jsonValue, jsonDataType),
        // For String, Double and Int64 topics, values can be passed directly
        session.topics.updateValue('topic/string', "This is a new string value", stringDataType)
    ]);

    // 4. Add a value streams for receiving JSON values.
    session.addStream('topic/json', jsonDataType).on('value', function(topic, spec, newValue, oldValue) {
        // When a JSON or Binary topic is updated, any value handlers on a subscription will be called with both the
        // new value, and the old value.

        // The oldValue parameter will be undefined if this is the first value received for a topic.

        // For JSON topics, value#get returns a JavaScript object.
        // For Binary topics, value#get returns a Buffer instance.
        console.log("Update for " + topic, newValue.get());
    });

    session.addStream('topic/string', stringDataType).on('value', function(topic, spec, newValue, oldValue) {
        // Unlike JSON or Binary, String, Double and Int64 datatypes provide values as primitive types.
        // This means you don't need to call #get to receive the actual data.
        console.log("Update for string topic: " + newValue);
    });

    session.select('?topic//');

    // 5. Raw values of an appropriate type can also be used for JSON and Binary topics.
    // For example, plain JSON objects can be used to update JSON topics.
    session.topics.updateValue('topic/json', {"foo" : "baz", "numbers" : [1, 2, 3] }, jsonDataType);
}
