#  Copyright (c) 2022 - 2023 DiffusionData Ltd., All Rights Reserved.
#
#  Use is subject to licence terms.
#
#  NOTICE: All information contained herein is, and remains the
#  property of DiffusionData. The intellectual and technical
#  concepts contained herein are proprietary to DiffusionData and
#  may be covered by U.S. and Foreign Patents, patents in process, and
#  are protected by trade secret or copyright law.


import asyncio
import datetime

import diffusion
import diffusion.datatypes


# Diffusion server connection information;
# adjust as needed for the server used in practice.
from diffusion.features.timeseries import TimeSeries

server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

TOPIC_PREFIX = "time-series"


async def main():
    """
    Control client implementation that appends values
    with a user supplied timestamp to a time series topic.
    """
    # Creating the session.
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:
        # Create a string topic
        topic_type = diffusion.datatypes.STRING
        topic_path = f"{TOPIC_PREFIX}/{topic_type.type_name}/{datetime.datetime.utcnow()}"
        specification = TimeSeries.of(topic_type).with_properties()

        try:
            await session.topics.add_topic(topic_path, specification)
        except Exception as ex:
            print(f"Failed to add topic '{topic_path}' : {ex}.")
            await session.close()
            return

        try:
            await session.time_series.append(
                topic_path,
                "Value1",
                diffusion.datatypes.STRING,
                timestamp=datetime.datetime.now()
                + datetime.timedelta(milliseconds=322),
            )
            await session.time_series.append(
                topic_path,
                "Value1",
                diffusion.datatypes.STRING,
                timestamp=datetime.datetime.now()
                + datetime.timedelta(milliseconds=323),
            )
            await session.time_series.append(
                topic_path,
                "Value1",
                diffusion.datatypes.STRING,
                timestamp=datetime.datetime.now()
                + datetime.timedelta(milliseconds=323),
            )
            await session.time_series.append(
                topic_path,
                "Value1",
                diffusion.datatypes.STRING,
                timestamp=datetime.datetime.now()
                + datetime.timedelta(milliseconds=324),
            )
            await session.time_series.append(
                topic_path,
                "Value1",
                diffusion.datatypes.STRING,
                timestamp=datetime.datetime.now()
                + datetime.timedelta(milliseconds=325),
            )

            await asyncio.sleep(0.3)
        except Exception as ex:
            print(f"Topic {topic_path} value could not be appended : {ex}.")

        # Remove the string topic
        try:
            await session.topics.remove_topic(topic_path)
        except Exception as ex:
            print(f"Failed to remove topic '{topic_path}' : {ex}.")
