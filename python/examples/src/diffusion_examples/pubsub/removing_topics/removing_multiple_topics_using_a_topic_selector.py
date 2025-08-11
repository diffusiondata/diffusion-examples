"""
Copyright © 2024 Diffusion Data Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import asyncio
import typing

import diffusion.datatypes
from diffusion import sessions, Credentials
from diffusion.features.topics import (
    TopicAddResponse,
    TopicSpecification
)
from diffusion.datatypes import JSON
from diffusion_examples.utils.program import Example



class RemovingMultipleTopicsUsingATopicSelector(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic_specification = JSON.with_properties()
            await self.add_and_set_topic(
                session,
                "my/topic/path/to/be/removed",
                topic_specification,
                {"diffusion": ["data", "more data"]},
            )
            await self.add_and_set_topic(
                session,
                "my/topic/path/to/be/also/removed",
                topic_specification,
                {"diffusion": ["data", "also more data"]},
            )
            await self.add_and_set_topic(
                session,
                "my/topic/path/will/not/be/removed",
                topic_specification,
                {"diffusion": ["no data"]},
            )
            await self.add_and_set_topic(
                session,
                "my/topic/path/will/not/be/removed/either",
                topic_specification,
                {"diffusion": ["no data either"]},
            )
            result = await session.topics.remove_topic("?my/topic/path/to/be//")
            print(f"{result.removed_count} topics have been removed.")

    async def add_and_set_topic(
        self,
        session: diffusion.session.Session,
        topic: str,
        topic_specification: TopicSpecification,
        json_data: typing.Any,
    ):
        result = await session.topics.add_topic(topic, topic_specification)
        if result == TopicAddResponse.CREATED:
            print("Topic has been created.")
        else:
            print("Topic already exists.")
        assert result == TopicAddResponse.CREATED
        await session.topics.set_topic(topic, JSON(json_data), topic_specification)




if __name__ == "__main__":
    asyncio.run(
        RemovingMultipleTopicsUsingATopicSelector().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
