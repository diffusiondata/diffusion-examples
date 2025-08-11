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
from diffusion import Credentials
from diffusion.features.topics import TopicAddResponse
from diffusion_examples.utils.program import Example
import diffusion.datatypes
import diffusion.features.timeseries


class EditTimeSeriesTopic(Example):
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
            result = await session.topics.add_topic(topic, specification)

            if result == TopicAddResponse.CREATED:
                print("Topic has been created.")
            else:
                print("Topic already exists.")

            for _ in range(0, 25):
                new_value = random.random()
                await session.time_series.append(
                    topic, new_value, diffusion.datatypes.DOUBLE
                )

            await session.time_series.edit(
                topic, 20, 3.14, diffusion.datatypes.DOUBLE
            )




if __name__ == "__main__":
    asyncio.run(
        EditTimeSeriesTopic().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
