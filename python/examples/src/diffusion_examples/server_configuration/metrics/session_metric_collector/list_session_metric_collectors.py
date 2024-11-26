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
from diffusion.features.control.metrics.session_metrics import (
    SessionMetricCollectorBuilder,
)
from diffusion_examples.utils.program import Example


class ListSessionMetricCollectors(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            session_filter = "$Principal is 'control'"

            collector = (
                SessionMetricCollectorBuilder()
                .export_to_prometheus(False)
                .group_by_properties("$Location")
                .remove_metrics_with_no_matches(True)
                .maximum_groups(10)
                .create("Session Metric Collector 1", session_filter)
            )
            await session.metrics.put_session_metric_collector(collector)

            collector = (
                SessionMetricCollectorBuilder()
                .export_to_prometheus(True)
                .group_by_properties("$Location")
                .remove_metrics_with_no_matches(True)
                .maximum_groups(250)
                .create("Session Metric Collector 2", session_filter)
            )
            await session.metrics.put_session_metric_collector(collector)

            list_session_metric_collectors = (
                await session.metrics.list_session_metric_collectors()
            )

            for session_metric_collector in list_session_metric_collectors:
                output = (
                    f"{session_metric_collector.name}: "
                    f"{session_metric_collector.session_filter} "
                    f"({session_metric_collector.maximum_groups}, "
                    f"{self.get_answer(session_metric_collector.exports_to_prometheus)}, "
                    f"{self.get_answer(session_metric_collector.removes_metrics_with_no_matches)}, "  # noqa: E501
                    f"{','.join(session_metric_collector.group_by_properties)})"
                )
                print(output)

    def get_answer(self, result):
        return "Yes" if result else "No"



if __name__ == "__main__":
    asyncio.run(
        ListSessionMetricCollectors().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
