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
from diffusion.datatypes import JSON, STRING
from diffusion_examples.utils.program import Example

class AddAndSetTopicAnd(Example):
    topic = "my/topic/path"

    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            json_data_1 = {"diffusion": "data"}
            # Add and set the topic with the first value
            result = await session.topics.add_and_set_topic(
                self.topic, JSON.with_properties(), JSON(json_data_1)
            )

            # Create the constraint with both 'with' and 'without'
            constraint_factory = ConstraintFactory()
            constraint_with = constraint_factory.json_value().with_(
                "/diffusion", STRING("data")
            )
            constraint_without = constraint_factory.json_value().without("/bar")
            combined_constraint = constraint_with and constraint_without

            if result == TopicAddResponse.CREATED:
                print("Topic has been created with the first value.")
            else:
                print("Topic already exists.")

            json_data_2 = {"diffusion": "baz"}
            # Set the topic with the second value (using the same constraint)
            result = await session.topics.add_and_set_topic(
                self.topic,
                JSON.with_properties(),
                JSON(json_data_2),
                combined_constraint,
            )

            if result == TopicAddResponse.EXISTS:
                print("Topic updated with the second value.")
            else:
                print("Topic was not updated.")




if __name__ == "__main__":
    asyncio.run(
        AddAndSetTopicAnd().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
