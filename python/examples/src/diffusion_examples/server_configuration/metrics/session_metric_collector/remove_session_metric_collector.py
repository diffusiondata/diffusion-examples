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
from diffusion.features.control.metrics.session_metrics import (
    SessionMetricCollectorBuilder,
)
from diffusion import sessions, Credentials
from diffusion_examples.utils.program import Example


class RemoveSessionMetricCollector(Example):
    async def run(
        self,
        server_url="<server_url>",
        principal="<principal>",
        password="<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            session_filter = "$Principal is 'control'"
            builder = (
                SessionMetricCollectorBuilder()
                .export_to_prometheus(False)
                .group_by_properties("$Location")
                .remove_metrics_with_no_matches(True)
                .maximum_groups(10)
            )
            collector = builder.create(
                "Session Metric Collector 1", session_filter
            )
            await session.metrics.put_session_metric_collector(collector)
            await session.metrics.remove_session_metric_collector(
                collector.name
            )
            print(f"{collector.name} has been removed.")



if __name__ == "__main__":
    asyncio.run(
        RemoveSessionMetricCollector().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
