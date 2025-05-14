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

from diffusion import sessions, Credentials
from diffusion.features.topics import (
    TopicAddResponse,
    ValueStreamHandler,
    TopicSpecification,
)
import diffusion.datatypes
from diffusion_examples.utils.program import Example

class SubscribeToSingleTopicUsingTopicPath(Example):
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
            topic_selector = ">my/topic/path"
            topic_specification = diffusion.datatypes.JSON.with_properties()
            result = await session.topics.add_topic(topic, topic_specification)
            if result == TopicAddResponse.CREATED:
                print("Topic has been created.")
            else:
                print("Topic already exists.")

            json_stream = JSONStream()
            session.topics.add_value_stream(topic_selector, json_stream)
            await session.topics.subscribe(topic_selector)
            await session.topics.set_topic(
                topic, diffusion.datatypes.JSON({"diffusion": "bar"}), diffusion.datatypes.JSON
            )

            await asyncio.sleep(5)
            await session.topics.unsubscribe(topic_selector)
            await session.topics.remove_stream(json_stream)



class JSONStream(ValueStreamHandler):
    def __init__(self) -> None:
        super().__init__(
            diffusion.datatypes.JSON,
            subscribe=self.on_subscription,
            unsubscribe=self.on_unsubscription,
            update=self.on_update,
            close=self.on_close
        )
        self._stream_values: typing.List[str] = []


    async def on_close(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[diffusion.datatypes.JSON],
        **kwargs
    ) -> None:
        pass


    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_subscription(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[diffusion.datatypes.JSON],
        **kwargs
    ) -> None:
        print(f"Subscribed to {topic_path}.")

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_unsubscription(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[diffusion.datatypes.JSON],
        reason: typing.Any,
        **kwargs
    ) -> None:
        print(f"Unsubscribed from {topic_path}: {reason}.")

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_update(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        old_value: typing.Optional[diffusion.datatypes.JSON],
        topic_value: typing.Optional[diffusion.datatypes.JSON],
        **kwargs
    ) -> None:
        print(f"{topic_path} changed from {old_value} to {topic_value}.")


if __name__ == "__main__":
    asyncio.run(
        SubscribeToSingleTopicUsingTopicPath().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
