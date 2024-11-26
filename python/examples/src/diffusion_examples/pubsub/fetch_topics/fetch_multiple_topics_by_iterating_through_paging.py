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

import diffusion
from diffusion import Credentials
from diffusion_examples.utils.program import Example
import diffusion.datatypes


class FetchMultipleTopicsByIteratingThroughPaging(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with diffusion.sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic_specification = diffusion.datatypes.STRING.with_properties()
            for i in range(1, 26):
                string_value = f"diffusion data #{i}"
                await session.topics.add_and_set_topic(
                    f"my/topic/path/{i}",
                    topic_specification,
                    string_value,
                )

            topic_selector = "?my/topic/path//"
            # Fetch the values for the topic selector using a maximum result size of 10.
            fetch_result = (
                await session.topics.fetch_request()
                .with_values(diffusion.datatypes.STRING)
                .first(10)
                .fetch(topic_selector)
            )

            while True:
                for topic in fetch_result.results:
                    addition = f"{topic.path}: {topic.value}"
                    print(addition)
                if fetch_result.has_more:
                    # Fetch the next 10 values.
                    path = fetch_result.results[-1].path
                    fetch_result = await (
                        session.topics.fetch_request()
                        .after(path)
                        .with_values(diffusion.datatypes.STRING)
                        .first(10)
                        .fetch(topic_selector)
                    )
                    print("Loading next page.")
                else:
                    print("Done.")
                    break




if __name__ == "__main__":
    asyncio.run(
        FetchMultipleTopicsByIteratingThroughPaging().run(
            server_url="ws://localhost:8080", principal="admin", password="password"
        )
    )
