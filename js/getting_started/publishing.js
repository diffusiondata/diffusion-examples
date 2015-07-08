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
    host : 'diffusion.example.com',
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    console.log('Connected!');

    var count = 0;
        
    // Create a topic with a default value of 0. 
    session.topics.add('foo/counter', count);
  
    // Start updating the topic every second
    setInterval(function() {
        session.topics.update('foo/counter', ++count);
    }, 1000);
});
