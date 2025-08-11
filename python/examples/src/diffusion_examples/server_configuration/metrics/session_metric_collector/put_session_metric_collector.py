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



class PutSessionMetricCollector(Example):
    """Example class for creating Session Metric Collector."""

    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        """Run the session metric collector.

        Args:
            server_url (str): The server URL.
            principal (str): The principal.
            password (str): The password.
        """
        session_filter = "$Principal is 'control'"

        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            collector_name = "Session Metric Collector 1"
            # Put session metric collector asynchronously and wait
            builder = SessionMetricCollectorBuilder()
            builder = builder.export_to_prometheus(False)
            builder = builder.group_by_properties("$Location")
            builder = builder.remove_metrics_with_no_matches(True)
            builder = builder.maximum_groups(10)
            collector = builder.create(collector_name, session_filter)

            await session.metrics.put_session_metric_collector(collector)
            print(f"Session metric collector '{collector_name}' added.")

            # Wait for the task to complete



if __name__ == "__main__":
    asyncio.run(
        PutSessionMetricCollector().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
