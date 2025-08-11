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
from diffusion.features.control.metrics.topic_metrics import (
    TopicMetricCollectorBuilder,
)


from diffusion_examples.utils.program import Example


class RemoveTopicMetricCollector(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic_selector = "?my/topic//"
            builder = TopicMetricCollectorBuilder().export_to_prometheus(False)
            builder = builder.group_by_topic_type(True)
            builder = builder.group_by_topic_view(True)
            builder = builder.group_by_path_prefix_parts(15)
            builder = builder.maximum_groups(10)
            collector = builder.create(
                "Topic Metric Collector 1", topic_selector
            )
            await session.metrics.put_topic_metric_collector(collector)
            await session.metrics.remove_topic_metric_collector(collector.name)
            print(f"{collector.name} has been removed.")


if __name__ == "__main__":
    asyncio.run(
        RemoveTopicMetricCollector().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
