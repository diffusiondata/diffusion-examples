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

import typing
import asyncio

from diffusion import sessions, Credentials
from diffusion.features.control.metrics.topic_metrics import (
    TopicMetricCollectorBuilder,
)

from diffusion_examples.utils.program import Example



class ListTopicMetricCollectors(Example):
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

            # Create first collector
            builder = TopicMetricCollectorBuilder()
            builder.export_to_prometheus(False)
            builder.group_by_topic_type(True)
            builder.group_by_topic_view(True)
            builder.group_by_path_prefix_parts(15)
            builder.maximum_groups(10)
            collector = builder.create("Topic Metric Collector 1", topic_selector)
            await session.metrics.put_topic_metric_collector(collector)

            # Create second collector
            builder = TopicMetricCollectorBuilder()
            builder.export_to_prometheus(True)
            builder.group_by_topic_type(False)
            builder.group_by_topic_view(True)
            builder.group_by_path_prefix_parts(15)
            builder.maximum_groups(250)
            collector = builder.create("Topic Metric Collector 2", topic_selector)
            await session.metrics.put_topic_metric_collector(collector)
            list_topic_metric_collectors = (
                await session.metrics.list_topic_metric_collectors()
            )
            for topic_metric_collector in list_topic_metric_collectors:
                result_str = (
                    f"{topic_metric_collector.name}: "
                    f"{topic_metric_collector.topic_selector} "
                    f"({topic_metric_collector.maximum_groups}, "
                    f"{get_answer(topic_metric_collector.exports_to_prometheus)}, "
                    f"{get_answer(topic_metric_collector.groups_by_topic_type)}, "
                    f"{get_answer(topic_metric_collector.groups_by_topic_view)}, "
                    f"{topic_metric_collector.group_by_path_prefix_parts})"
                )
                print(result_str)


# noinspection PyMethodMayBeStatic
def get_answer(result: typing.Optional[bool]):
    return "Yes" if result else "No"


if __name__ == "__main__":
    asyncio.run(
        ListTopicMetricCollectors().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
