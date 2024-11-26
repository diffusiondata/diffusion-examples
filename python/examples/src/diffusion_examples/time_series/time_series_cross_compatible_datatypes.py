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
import random
import typing

import diffusion.datatypes
from diffusion import Credentials
from diffusion.features.topics import TopicAddResponse, ValueStreamHandler
import diffusion.features.timeseries
from diffusion_examples.utils.program import Example


class TimeSeriesCrossCompatibleDatatypes(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with diffusion.sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            specification = diffusion.features.timeseries.TimeSeries.of(
                diffusion.datatypes.DOUBLE
            ).with_properties(
                TIME_SERIES_RETAINED_RANGE="limit 15 last 10s",
                TIME_SERIES_SUBSCRIPTION_RANGE="limit 3",
            )

            topic = "my/time/series/topic/path"
            topic_selector = "?my/time/series//"

            result = await session.topics.add_topic(topic, specification)

            if result == TopicAddResponse.CREATED:
                print("Topic has been created.")
            else:
                print("Topic already exists.")

            for i in range(25):
                new_value = random.random()
                await session.time_series.append(
                    topic, new_value, diffusion.datatypes.DOUBLE
                )

            json_stream = JSONStream()
            session.topics.add_value_stream(topic_selector, json_stream)

            await session.topics.subscribe(topic_selector)

            await session.topics.unsubscribe(topic_selector)
            await session.topics.remove_stream(json_stream)


class JSONStream(ValueStreamHandler):
    def __init__(self):
        super().__init__(
            data_type=diffusion.datatypes.JSON,
            subscribe=self.on_subscription,
            update=self.on_value,
            unsubscribe=self.on_unsubscription,
            on_error=self.on_error,
        )

    def on_close(self):
        pass

    def on_error(self, error_reason):
        pass

    def on_subscription(self, topic_path, topic_spec, topic_value):
        print(f"Subscribed to {topic_path}.")

    def on_unsubscription(self, topic_path, topic_spec, reason, topic_value):
        print(f"Unsubscribed from {topic_path}: {reason}.")

    def on_value(
        self,
        topic_path,
        topic_spec,
        old_value: typing.Optional[diffusion.datatypes.JSON],
        topic_value: diffusion.datatypes.JSON,
    ):
        addition = (
            f"{topic_path} changed from "
            f"{('NULL' if old_value is None else old_value.value)}"
            f" to {topic_value.value}."
        )
        print(addition)


if __name__ == "__main__":
    asyncio.run(
        TimeSeriesCrossCompatibleDatatypes().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
