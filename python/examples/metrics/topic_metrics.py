#  Copyright (c) 2021 Push Technology Ltd., All Rights Reserved.
#
#  Use is subject to license terms.
#
#  NOTICE: All information contained herein is, and remains the
#  property of Push Technology. The intellectual and technical
#  concepts contained herein are proprietary to Push Technology and
#  may be covered by U.S. and Foreign Patents, patents in process, and
#  are protected by trade secret or copyright law.

# Client implementation that demonstrates the session metric collector.

import asyncio
import diffusion
from diffusion.features.control.metrics.topic_metrics import TopicMetricCollectorBuilder

server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():
    # Runs the session metric collector example.
    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:
        metrics = session.metrics
        topic_selector = "selector"

        try:
            print(
                f"""\
Adding the topic metric collector 'Test' with topic selector '{topic_selector}'.
                """
            )

            collector = (
                TopicMetricCollectorBuilder()
                .group_by_topic_type(True)
                .create("Test", topic_selector)
            )

            await metrics.put_topic_metric_collector(collector)

            print(f"Topic metric collector '{collector.name}' added.")
        except Exception as ex:
            print(f"Failed to add topic metric collector : {ex}.")
            return

        try:
            print("The following topic metric collectors exist:")

            list_topic_metric_collectors = await metrics.list_topic_metric_collectors()

            for topic_metric_collector in list_topic_metric_collectors:
                print(
                    f"Name: '{topic_metric_collector.name}', "
                    f"Topic selector: '{topic_metric_collector.topic_selector}', "
                    f"Exports to Prometheus: '{topic_metric_collector.exports_to_prometheus}', "
                    f"Groups by topic type: '{topic_metric_collector.groups_by_topic_type}'"
                )
        except Exception as ex:
            print(f"Failed to list topic metric collectors : {ex}.")
            return

        try:
            await metrics.remove_topic_metric_collector(collector.name)

            print(f"Collector '{collector.name}' removed.")
        except Exception as ex:
            print(f"Failed to remove topic metric collector : {ex}.")


if __name__ == "__main__":
    asyncio.run(main())
