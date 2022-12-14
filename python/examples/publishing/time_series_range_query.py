#   Copyright (c) 2022 Push Technology Ltd., All Rights Reserved.
#
#   Use is subject to license terms.
#
#  NOTICE: All information contained herein is, and remains the
#  property of Push Technology. The intellectual and technical
#  concepts contained herein are proprietary to Push Technology and
#  may be covered by U.S. and Foreign Patents, patents in process, and
#  are protected by trade secret or copyright law.


# Control client implementation that performs a range query of a time series.

import diffusion
from datetime import datetime, timedelta
from diffusion.features.timeseries import TimeSeries
from diffusion.datatypes import STRING

# Diffusion server connection information;
# adjust as needed for the server used in practice.
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

TOPIC_PREFIX = "time-series"


async def main():
    """
    Runs the time series topic range query example.
    """
    # Creating the session.
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:
        # Create a string topic
        data_type = STRING
        topic_path = f"{TOPIC_PREFIX}/{data_type.type_name}/{datetime.utcnow()}"
        specification = TimeSeries.of(data_type)

        try:
            await session.topics.add_topic(topic_path, specification)
        except Exception as ex:
            print(f"Failed to add topic '{topic_path}' : {ex}.")
            return

        epoch = datetime.fromtimestamp(0)
        try:
            await session.time_series.append(
                topic_path, "Value1", STRING, epoch + timedelta(milliseconds=200)
            )
            await session.time_series.append(
                topic_path, "Value2", STRING, epoch + timedelta(milliseconds=301)
            )
            await session.time_series.append(
                topic_path, "Value3", STRING, epoch + timedelta(milliseconds=301)
            )
            await session.time_series.append(
                topic_path, "Value4", STRING, epoch + timedelta(milliseconds=301)
            )
            await session.time_series.append(
                topic_path, "Value5", STRING, epoch + timedelta(milliseconds=324)
            )
            await session.time_series.append(
                topic_path, "Value6", STRING, epoch + timedelta(milliseconds=501)
            )
            await session.time_series.append(
                topic_path, "Value7", STRING, epoch + timedelta(milliseconds=501)
            )
            await session.time_series.append(
                topic_path, "Value8", STRING, epoch + timedelta(milliseconds=501)
            )

        except Exception as ex:
            print(f"Topic {topic_path} value could not be appended : {ex}.")

        try:
            range_query = session.time_series.range_query(STRING)

            range = await (
                range_query.from_(timedelta(milliseconds=301))
                .to(timedelta(milliseconds=501))
                .select_from(topic_path)
            )

            results = range.events
            print(f"{len(results)} results obtained from range query (301 to 501):")

            for result in results:
                print(
                    f"Sequence number: {result.metadata.sequence}, Value: {result.value} "
                    f"with Timestamp: {result.metadata.timestamp}"
                )
        except Exception as ex:
            print(f"Range query failed for topic {topic_path} : {ex}.")

        # Remove the string topic
        try:
            await session.topics.remove_topic(topic_path)
        except Exception as ex:
            print(f"Failed to remove topic '{topic_path}' : {ex}.")
