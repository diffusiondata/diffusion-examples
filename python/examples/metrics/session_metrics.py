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
from diffusion.features.control.metrics.session_metrics import SessionMetricCollectorBuilder

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
        session_filter = "x is 'y'"

        try:
            print(
                f"""\
Adding the session metric collector 'Test' with session filter '{session_filter}'."""
            )

            collector = (
                SessionMetricCollectorBuilder()
                .group_by_properties("$Location")
                .remove_metrics_with_no_matches(True)
                .maximum_groups(10)
                .create("Test", session_filter)
            )

            await metrics.put_session_metric_collector(collector)

            print(f"Session metric collector '{collector.name}' added.")
        except Exception as ex:
            print(f"Failed to add session metric collector : {ex}.")
            return

        try:
            print("The following session metric collectors exist:")

            listSessionMetricCollectors = await metrics.list_session_metric_collectors()

            for session_metric_collector in listSessionMetricCollectors:
                print(
                    f"""
Name: '{session_metric_collector.name}',
Session filter: '{session_metric_collector.session_filter}',
Maximum Groups: {session_metric_collector.maximum_groups},
Exports to Prometheus: '{session_metric_collector.exports_to_prometheus}',
Removes metrics with no matches: '{session_metric_collector.removes_metrics_with_no_matches}'
                    """
                )

            for property in session_metric_collector.group_by_properties:
                print(f"Group by: '{property}' property")
        except Exception as ex:
            print(f"Failed to list session metric collectors : {ex}.")

        try:
            await metrics.remove_session_metric_collector(collector.name)

            print(f"Collector '{collector.name}' removed.")
        except Exception as ex:
            print(f"Failed to remove session metric collector : {ex}.")

if __name__ == "__main__":
    asyncio.run(main())
