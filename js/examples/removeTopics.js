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

var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {

    // 1. Like session.topics.add(), remove returns a promise, so we can chain together calls.
    session.topics.add('foo').then(session.topics.remove('foo'))
                             .then(function() {
                                console.log('Removed topic foo');
                             }, function(reason) {
                                console.log('Failed to remove topic foo: ', reason);
                             });
                           
    // 2. Removing a topic will remove all topics underneath it.
    
    // Add a hierarchy of topics.
    var added = session.topics.add('a').then(session.topics.add('a/b'))
                                       .then(session.topics.add('a/b/c'))
                                       .then(session.topics.add('a/b/c/d'));

    // Wait until we've added all the topics
    added.then(session.topics.remove('a'))
         .then(function() {
            console.log('Removed all topics including & under "a"');
         });
});
