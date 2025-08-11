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
import diffusion.datatypes
from diffusion_examples.utils.program import Example



class FetchTopicsProperties(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topics = session.topics
            for i in range(1, 6):
                await topics.add_and_set_topic(
                    f"my/topic/path/with/properties/{i}",
                    diffusion.datatypes.JSON.with_properties(
                        DONT_RETAIN_VALUE=True,
                        PERSISTENT=False,
                        PUBLISH_VALUES_ONLY=True,
                    ),
                    {"diffusion": "data #{i}"},
                )

                await topics.add_and_set_topic(
                    f"my/topic/path/with/default/properties/{i}",
                    diffusion.datatypes.STRING.with_properties(
                        DONT_RETAIN_VALUE=True,
                        PERSISTENT=False,
                        PUBLISH_VALUES_ONLY=True,
                    ),
                    f"diffusion data #{i}",
                )

            fetch_result = (
                await topics.fetch_request()
                .topic_types({diffusion.datatypes.JSON})
                .with_properties()
                .fetch("?my/topic/path//")
            )

            for topic in fetch_result.results:
                print(f"{topic.path}:")
                for (
                    prop_key,
                    prop_value,
                ) in topic.specification.properties.items():
                    print(f"{prop_key}: {prop_value}")


            # Fetch the topic properties for the topic path selector my/topic/path/.
            fetch_result = (
                await topics.fetch_request()
                .topic_types({diffusion.datatypes.STRING})
                .with_properties()
                .fetch("?my/topic/path//")
            )

            for topic in fetch_result.results:
                print(f"{topic.path} properties:")
                for (
                    prop_key,
                    prop_value,
                ) in topic.specification.properties.items():
                    print(f"{prop_key}: {prop_value}")




if __name__ == "__main__":
    asyncio.run(
        FetchTopicsProperties().run(
            server_url="wss://localhost:8080",
            principal="admin",
            password="password",
        )
    )
