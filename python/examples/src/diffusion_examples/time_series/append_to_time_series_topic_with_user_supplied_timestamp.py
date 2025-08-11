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
import datetime

import diffusion.datatypes
from diffusion import Credentials
from diffusion.features.timeseries import TimeSeries
from diffusion.features.topics import TopicAddResponse
from diffusion_examples.utils.program import Example, random_double


class AppendToTimeSeriesTopicWithUserSuppliedTimestamp(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with diffusion.sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            specification = TimeSeries.of(
                diffusion.datatypes.DOUBLE
            ).with_properties(
                TIME_SERIES_RETAINED_RANGE="limit 15 last 10s",
                TIME_SERIES_SUBSCRIPTION_RANGE="limit 3",
            )
            topic = "my/time/series/topic/path/user/supplied"
            result = await session.topics.add_topic(topic, specification)
            if result == TopicAddResponse.CREATED:
                print("Topic has been created.")
            else:
                print("Topic already exists.")


            millis = 1000000

            for i in range(25):
                new_value = random_double()
                millis += 1
                await session.time_series.append(
                    topic,
                    new_value,
                    diffusion.datatypes.DOUBLE,
                    datetime.datetime.fromtimestamp(
                        millis, tz=datetime.timezone.utc
                    ),
                )



if __name__ == "__main__":
    asyncio.run(
        AppendToTimeSeriesTopicWithUserSuppliedTimestamp().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
