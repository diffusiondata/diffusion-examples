"""
Copyright © 2023 - 2024 Diffusion Data Ltd.

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
from diffusion import Session, sessions, Credentials
from diffusion_examples.utils.program import Example
from diffusion.features.topics import TopicAddResponse, TopicSpecification
from diffusion.datatypes import JSON


class RemovingASingleTopicUsingTopicPath(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<pricipal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic_control = session.topics

            topic = "my/topic/path/to/be/removed"
            topic_specification = JSON.with_properties()

            await self.add_and_set_topic(
                session,
                topic,
                topic_specification,
                {"diffusion": ["data", "more data"]},
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

            await topic_control.remove_topic(topic)
            print("Topic has been removed.")


    async def add_and_set_topic(
        self,
        session: Session,
        topic: str,
        topic_specification: TopicSpecification,
        json_data,
    ):
        topic_update = session.topics
        topic_control = session.topics
        result = await topic_control.add_topic(topic, topic_specification)
        if result == TopicAddResponse.CREATED:
            print("Topic has been created.")
        else:
            print("Topic already exists.")

        await topic_update.set_topic(topic, JSON(json_data), topic_specification)


if __name__ == "__main__":
    asyncio.run(
        RemovingASingleTopicUsingTopicPath().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
