/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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

var stringDataType = diffusion.datatypes.string();
var SessionLockScope = diffusion.locks.SessionLockScope;

var LOCK_NAME = "lockA";

var session1, session2;
var session1Lock, session2Lock;

function acquireLockSession1() {
    console.log("Requestinglock 1");
    session1.lock(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS)
        .then(function(lock) {
            console.log("Acquired lock 1");
            session1Lock = lock;
            acquireLockSession2();
            setTimeout(function () {
                releaseLock1();
            }, 1000);
        });
}

function acquireLockSession2() {
    console.log("Requesting lock 2");
    session2.lock(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS)
        .then(function(lock) {
            console.log("Acquired lock 2");
            session2Lock = lock;
        });
}

function releaseLock1() {
    console.log("Requesting lock 1 release");
    session1Lock.unlock().then(function() {
        console.log("Released lock 1");
    });
}

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
    session1 = session;
    diffusion.connect({
        host   : 'diffusion.example.com',
        port   : 443,
        secure : true,
        principal : 'control',
        credentials : 'password'
    }).then(function(session) {
        session2 = session;
        acquireLockSession1();
    });
});
