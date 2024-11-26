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

from diffusion import Credentials
from diffusion.features.topics import TopicAddResponse, TopicSpecification
from diffusion.internal.services.topics import UnsubscribeReason
from diffusion_examples.utils.program import Example
import diffusion.datatypes
import diffusion.features.timeseries
from diffusion.features.topics.streams import ValueStreamHandler


class SubscribeToTimeSeriesTopics(Example):
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

            for _ in range(25):
                new_value = random.random()
                await session.time_series.append(
                    topic, new_value, diffusion.datatypes.DOUBLE
                )

            value_stream = self.ValueStream()
            session.topics.add_value_stream(topic_selector, value_stream)

            await session.topics.subscribe(topic_selector)

            await session.topics.unsubscribe(topic_selector)
            await session.topics.remove_stream(value_stream)


    class ValueStream(ValueStreamHandler):
        def __init__(self):
            self._stream_values = []
            super().__init__(
                diffusion.datatypes.DOUBLE,
                on_error=self.on_error,
                subscribe=self.on_subscription,
                unsubscribe=self.on_unsubscription,
                update=self.on_value,
                close=self.on_close,
            )


        def on_close(self):
            pass

        def on_error(self, error_reason):
            pass

        # noinspection PyUnusedLocal
        def on_subscription(
            self,
            topic_path: str,
            topic_spec: TopicSpecification,
            topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
        ):
            message = f"Subscribed to {topic_path}."
            print(message)

        # noinspection PyUnusedLocal
        def on_unsubscription(
            self,
            topic_path: str,
            topic_spec: TopicSpecification,
            topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
            reason: UnsubscribeReason,
        ):
            message = f"Unsubscribed from {topic_path}: {reason}."
            print(message)

        def on_value(self, topic_path, topic_spec, old_value, topic_value):
            message = (
                f"{topic_path} changed from "
                f"{'NULL' if old_value is None else old_value} to {topic_value}."
            )
            print(message)


if __name__ == "__main__":
    asyncio.run(
        SubscribeToTimeSeriesTopics().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
