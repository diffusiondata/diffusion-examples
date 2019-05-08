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

import { connect, datatypes, locks, Session, SessionLock } from 'diffusion';


// example showcasing how to acquire and release session locks
export async function sessionLockExample() {

    const stringDataType = datatypes.string();
    const SessionLockScope = locks.SessionLockScope;

    const LOCK_NAME = "lockA";

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session1: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    const session2: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    let session1Lock: SessionLock;
    let session2Lock: SessionLock;

    async function acquireLockSession1() {
        console.log(`Requesting lock ${LOCK_NAME} by session 1`);
        session1Lock = await session1.lock(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS);

        console.log(`Acquired lock ${LOCK_NAME} by session 1`);

        // Note: no await
        // Not waiting for session 2 to acquire the lock
        acquireLockSession2();
        setTimeout(releaseLock1, 1000);
    }

    async function acquireLockSession2() {
        console.log(`Requesting lock ${LOCK_NAME} by session 2`);

        // this will block until the lock has been released by session 1 in releaseLock1
        session2Lock = await session2.lock(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS);

        console.log(`Acquired lock ${LOCK_NAME} by session 2`);
    }

    async function releaseLock1() {
        console.log(`Requesting lock ${LOCK_NAME} release by session 1`);
        await session1Lock.unlock();

        console.log(`Released lock ${LOCK_NAME} from session 1`);
    }

    acquireLockSession1();
}
