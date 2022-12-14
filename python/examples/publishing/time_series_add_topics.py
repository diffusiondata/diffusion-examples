#   Copyright (c) 2022 Push Technology Ltd., All Rights Reserved.
#
#   Use is subject to license terms.
#
#  NOTICE: All information contained herein is, and remains the
#  property of Push Technology. The intellectual and technical
#  concepts contained herein are proprietary to Push Technology and
#  may be covered by U.S. and Foreign Patents, patents in process, and
#  are protected by trade secret or copyright law.


# <summary>
# Control client implementation that adds a time series topic.
# </summary>

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
    Runs the time series add topics control client example.
    """
    # Creating the session.
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:
        # Create a string topic
        topic_type = diffusion.datatypes.STRING
        topic = f"{TOPIC_PREFIX}/{topic_type.type_name}/{datetime.datetime.utcnow()}"
        specification = TimeSeries.of(topic_type).with_properties()

        try:
            await session.topics.add_topic(topic, specification)

            new_value = f"{datetime.datetime.today()} {datetime.datetime.now().time()}"

            try:
                await session.topics.set_topic(
                    topic, new_value, diffusion.datatypes.STRING
                )
                await asyncio.sleep(0.3)
            except Exception as ex:
                print(f"Topic {topic} could not be updated : {ex}.")
        except Exception as ex:
            print(f"Failed to add topic '{topic}' : {ex}.")
            await session.close()
            return

        # Remove the string topic
        try:
            await session.topics.remove_topic(topic)
        except Exception as ex:
            print(f"Failed to remove topic '{topic}' : {ex}.")
