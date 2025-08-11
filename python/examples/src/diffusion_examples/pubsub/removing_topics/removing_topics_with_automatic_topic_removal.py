# Copyright © 2024 Diffusion Data Ltd.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import asyncio

import diffusion.datatypes
from diffusion import sessions, Credentials
from diffusion.features.topics import TopicAddResponse
from diffusion_examples.utils.program import Example


class RemovingTopicsWithAutomaticTopicRemoval(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(Credentials(password)).open(
                server_url) as session:
            await self.add_topic(session, 'my/topic/path/to/be/removed/time/after',
                                 "when time after 'Tue, 4 May 2077 11:05:30 GMT'")

            await self.add_topic(session, 'my/topic/path/to/be/removed/subscriptions',
                                 'when subscriptions < 1 for 10m')

            await self.add_topic(session, 'my/topic/path/to/be/removed/local/subscriptions',
                                 'when local subscriptions < 1 for 10m')

            await self.add_topic(session, 'my/topic/path/to/be/removed/no/updates',
                                 'when no updates for 10m')

            await self.add_topic(session, 'my/topic/path/to/be/removed/no/session',
                                 'when no session has \'$Principal is "client"\' for 1h')

            await self.add_topic(session, 'my/topic/path/to/be/removed/no/local/session',
                                 'when no local session has \'Department is "Accounts"\' for 1h after 1d')  # noqa: E501

            await self.add_topic(session, 'my/topic/path/to/be/removed/subcriptions/or/updates',
                                 'when subscriptions < 1 for 10m or no updates for 20m')
            await self.add_topic(session,
                                 'my/topic/path/to/be/removed/subcriptions/and/updates',
                                 'when subscriptions < 1 for 10m and no updates for 20m')


    async def add_topic(self, session, topic: str, removal_value: str):
        topic_specification = diffusion.datatypes.JSON.with_properties(REMOVAL=removal_value)

        result = await session.topics.add_topic(topic, topic_specification)

        if result == TopicAddResponse.CREATED:
            print('Topic has been created.')
        else:
            print('Topic already exists.')




if __name__ == '__main__':
    asyncio.run(RemovingTopicsWithAutomaticTopicRemoval().run(
        server_url="ws://localhost:8080",
        principal="admin",
        password="password",
    ))
