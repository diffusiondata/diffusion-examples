#   Copyright (c) 2022 Push Technology Ltd., All Rights Reserved.
#
#   Use is subject to license terms.
#
#  NOTICE: All information contained herein is, and remains the
#  property of Push Technology. The intellectual and technical
#  concepts contained herein are proprietary to Push Technology and
#  may be covered by U.S. and Foreign Patents, patents in process, and
#  are protected by trade secret or copyright law.
import asyncio
import typing

import diffusion.datatypes

from diffusion.datatypes.timeseries import Event
from diffusion.features.timeseries import TimeSeries
from diffusion.features.topics import ValueStreamHandler
from diffusion.features.topics.details.topic_specification import TopicSpecification
from diffusion.internal.services.topics import UnsubscribeReason
from diffusion.internal.session.exception_handler import ErrorReason


# Diffusion server connection information;
# adjust as needed for the server used in practice.
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

TOPIC_PREFIX = "time-series"


async def main():
    """
    Client implementation which subscribes to a string time series topic and
    consumes the data it receives.
    """
    # Creating the session.
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:
        # Create a string topic
        topic_type = diffusion.datatypes.STRING
        topic = f"?{TOPIC_PREFIX}/{topic_type.type_name}//"

        # Add a value stream
        time_series_string_stream = TimeSeriesStringStream()
        session.topics.add_value_stream(topic, time_series_string_stream)

        # Subscribe to the topic
        await session.topics.subscribe(topic)

        # Run until the ending of the example
        await asyncio.sleep(0.3)


# noinspection PyUnusedLocal
class TimeSeriesStringStream(ValueStreamHandler):
    """
    Basic implementation of the ValueStreamHandler for time series string topics.
    """
    def __init__(self):
        super().__init__(
            TimeSeries.of(diffusion.datatypes.STRING),
            update=self.update,
            subscribe=self.subscribe,
            unsubscribe=self.unsubscribe,
            error=self.error,
            close=self.close,
        )

    async def close(self):
        """
        Notification of the stream being closed normally.
        """
        print("The subscrption stream is now closed.")

    async def error(self, error_reason: ErrorReason):
        """
        Notification of a contextual error related to this callback.

        Situations in which <code>OnError</code> is called include
        the session being closed, a communication
        timeout, or a problem with the provided parameters.
        No further calls will be made to this callback.

        Args:
            error_reason: Error reason.
        """
        print(f"An error has occured : {error_reason}.")

    async def subscribe(
        self,
        *,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[Event[diffusion.datatypes.STRING]] = None,
    ):
        """
        Notification of a successful subscription.

        Args:
            topic_path: Topic path.
            topic_spec: Topic specification.
            topic_value: Topic value.
        """
        print(f"Client subscribed to {topic_path}.")

    async def unsubscribe(
        self,
        *,
        topic_path: str,
        topic_spec: TopicSpecification,
        reason: typing.Optional[UnsubscribeReason] = None,
    ):
        """
        Args:
            topic_path: Topic path.
            topic_spec: Topic specification.
            reason: error reason.
        """
        print(f"Client unsubscribed from {topic_path} : {reason}.")

    async def update(
        self,
        *,
        topic_path: str,
        topic_spec: TopicSpecification,
        old_value: Event[diffusion.datatypes.STRING],
        topic_value: Event[diffusion.datatypes.STRING],
    ):
        """
        Topic update received.

        Args:
            topic_path: Topic path.
            topic_spec: Topic specification.
            old_value: Value prior to update.
            topic_value: Value after update.
        """
        print(f"New value of {topic_path} is {topic_value}.")
