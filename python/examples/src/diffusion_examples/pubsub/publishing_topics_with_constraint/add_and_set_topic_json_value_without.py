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
from diffusion import sessions, Credentials
from diffusion.features.topics import TopicAddResponse
from diffusion.features.topics.update.constraint_factory import (
    ConstraintFactory,
)
from diffusion.datatypes import JSON
from diffusion_examples.utils.program import Example

class AddAndSetTopicJSONValueWithout(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic = "my/topic/path"
            json_data = {"diffusion": "data"}
            constraint = ConstraintFactory().json_value.without("/bar")
            topic_specification = JSON.with_properties()
            result = await session.topics.add_and_set_topic(
                topic, topic_specification, JSON(json_data)
            )
            if result == TopicAddResponse.CREATED:
                print("Topic has been created.")
            else:
                raise RuntimeError("Topic failed to be created.")


            json_data2 = {"diffusion": "baz"}
            result = await session.topics.add_and_set_topic(
                topic, topic_specification, JSON(json_data2), constraint
            )
            if result == TopicAddResponse.EXISTS:
                print("Topic already exists.")
            else:
                raise Exception("Topic should exist already.")



if __name__ == "__main__":
    asyncio.run(
        AddAndSetTopicJSONValueWithout().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
