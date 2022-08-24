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

diffusion.connect({
    host : 'hostname',
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    console.log('Connected!');

    // Create a topic that contains Double values
    var doubleSpec = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.DOUBLE);
    
    session.topics.add('foo/counter', doubleSpec);

    // Start updating the topic every second
    var count = 0;

    setInterval(function() {
        session.topicUpdate.set('foo/counter', diffusion.datatypes.double(), count++);
    }, 1000);
});
