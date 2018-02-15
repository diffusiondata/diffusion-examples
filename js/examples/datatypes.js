/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
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

var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {

    // 1. Data Types are exposed from the top level Diffusion namespace. It is often easier
    // to assign these directly to a local variable.
    var stringDataType = diffusion.datatypes.string();
    var jsonDataType = diffusion.datatypes.json();


    // 2. Data Types are currently provided for JSON, Binary, String, Double, Int64 and RecordV2 topic types.
    session.topics.add('topic/string', diffusion.topics.TopicType.STRING);
    session.topics.add('topic/json', diffusion.topics.TopicType.JSON);

    // 3. Values can be created directly from the data type.
    var jsonValue = jsonDataType.from({
        "foo" : "bar"
    });

    // Topics are updated using the standard update mechanisms
    session.topics.update('topic/json', jsonValue);

    // For String, Double and Int64 topics, values can be passed directly
    session.topics.update('topic/string', "This is a new string value");


    session.subscribe('topic/.*');

    // 4. Streams can be specialised to provide values from a specific datatype.
    session.stream('topic/json').asType(jsonDataType).on('value', function(topic, spec, newValue, oldValue) {
        // When a JSON or Binary topic is updated, any value handlers on a subscription will be called with both the
        // new value, and the old value.
   
        // The oldValue parameter will be undefined if this is the first value received for a topic.

        // For JSON topics, value#get returns a JavaScript object.
        // For Binary topics, value#get returns a Buffer instance.
        console.log("Update for " + topic, newValue.get());
    });

    session.stream('topic/string').asType(stringDataType).on('value', function(topic, spec, newValue, oldValue) {
        // Unlike JSON or Binary, String, Double and Int64 datatypes provide values as primitive types.
        // This means you don't need to call #get to receive the actual data.
        console.log("Update for string topic: " + newValue);
    });

    // 5. Raw values of an appropriate type can also be used for JSON and Binary topics. 
    // For example, plain JSON objects can be used to update JSON topics.
    session.topics.update('topic/json', {
         "foo" : "baz",
         "numbers" : [1, 2, 3]
    });
});
