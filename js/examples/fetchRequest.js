/*******************************************************************************
 * Copyright (C) 2018, 2021 Push Technology Ltd.
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

var jsonDataType = diffusion.datatypes.json();
var TopicType = diffusion.topics.TopicType;


// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'client',
    credentials : 'password'
}).then(function(session) {
    session
        .fetchRequest()             // obtain a FetchRequest
        .from("SomeTopic/B")        // limit to topics after and including SomeTopic/B
        .to("SomeTopic/X")          // limit to topics before and including SomeTopic/X
        .first(10)                  // only fetch the first 10 topics
        .topicTypes([TopicType.STRING, TopicType.INT64]) // limit to string and integer topic types
        .withValues(jsonDataType)   // fetch values return them as JSON objects
        .withProperties()           // get the topic properties
        .fetch("*SomeTopic//")      // perform the fetch request using a topic selector
        .then(function(fetchResult) {
            var results = fetchResult.results();
            console.log("Fetch Request returned "+results.length+" topics");

            results.forEach(function(topicResult) {
                console.log("Path: "+topicResult.path());
                console.log("Type: "+topicResult.type());
                console.log("Value: "+topicResult.value().get());
            });

            if (fetchResult.hasMore()) {
                console.log("There are more topics");
            }
        });

    session
        .fetchRequest()             // obtain a FetchRequest
        .limitBranchDepth(3, 3)     // A deep branch has a root path that has a
                                    // number of parts equal to the deep_branch_depth parameter.
                                    // The deep_branch_limit specifies the maximum number of results for each deep branch.
        .withProperties()           // get the topic properties
        .topicTypes([TopicType.STRING, TopicType.INT64]) // limit to string and integer topic types
        .withValues(jsonDataType)   // fetch values return them as JSON objects
        .fetch("?.//")              // perform the fetch using a topic selector
        .then(function(fetchResult) {
            var results = fetchResult.results()

            console.log("Fetch Request returned "+results.length+" topics");

            results.forEach(function(topicResult) {
                console.log("Path: "+topicResult.path());
                console.log("Type: "+topicResult.type());
                console.log("Value: "+topicResult.value().get());
            });

            if (fetchResult.hasMore()) {
                console.log("There are more topics");
            }
        });
});
