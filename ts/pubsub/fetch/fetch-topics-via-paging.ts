/*******************************************************************************
 * Copyright (C) 2024 Diffusion Data Ltd.
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

import { connect, datatypes, FetchResult, topics } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../test/util';
/// end::log

export async function pubSubFetchTopicViaPaging(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        Array.from({length: 25}). map((v, i) => `my/topic/path/${i}: diffusion data #${i}`)
    ]);
    /// end::log
    /// tag::pub_sub_fetch_topic_via_paging[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.STRING);

    for (let count = 0; count < 25; count++) {
        await session.topicUpdate.set(
            `my/topic/path/${count}`,
            datatypes.string(),
            `diffusion data #${count}`,
            { specification: specification }
        );
    }

    let fetchResult: FetchResult<string>;
    let lastTopicPath: string | undefined = undefined;
    do {
        let fetchRequest = session.fetchRequest()
            .withValues(datatypes.string())
            .first(10);
        if (lastTopicPath !== undefined) {
            fetchRequest = fetchRequest.after(lastTopicPath);
        }
        fetchResult = await fetchRequest.fetch('?my/topic/path//');
        const topicResults = fetchResult.results();

        for (const result of topicResults) {
            console.log(`${result.path()}: ${result.value()}`);
            /// tag::log
            check.log(`${result.path()}: ${result.value()}`);
            /// end::log
        }
        if (fetchResult.hasMore()) {
            /// tag::log
            expect(topicResults.length).toBe(10);
            /// end::log
            console.log('loading next page');
            lastTopicPath = topicResults[topicResults.length - 1].path();
        } else {
            /// tag::log
            expect(topicResults.length).toBe(5);
            /// end::log
            console.log('done');
        }
    } while (fetchResult.hasMore());

    await session.closeSession();
    /// end::pub_sub_fetch_topic_via_paging[]
    /// tag::log
    await check.done();
    /// end::log
}
