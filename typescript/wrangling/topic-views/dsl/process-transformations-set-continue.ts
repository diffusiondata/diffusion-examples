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

import { connect, datatypes, topics } from 'diffusion';

export async function topicViewsDslProcessTransformationsContinue(): Promise<void> {
    const check = new PartiallyOrderedCheckpointTester([[
        'Subscribed to views/2'
    ]]);
            check.log(`Subscribed to ${topic}`);
    await expectJsonTopicToHaveValue('views/2', {
        name: 'AMZN',
        quantity: 256,
        price_per_share: 87.65
    });

    await expectTopicCounts({ 'views/1': 0 });

    await check.done();
