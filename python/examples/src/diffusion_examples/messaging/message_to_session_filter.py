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

from diffusion import sessions, Credentials, SessionId
from diffusion.handlers import EventStreamHandler
from diffusion.messaging import RequestHandler
import diffusion.datatypes

from diffusion_examples.utils.program import Example


class MessageToSessionFilter(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        path = "my/message/path"

        async with sessions().principal("admin").credentials(
            Credentials(password)
        ).open(server_url) as session:
            request_stream = SimpleRequestStream()
            session.messaging.add_stream_handler(
                path, request_stream, addressed=True
            )

            async with sessions().principal("control").credentials(
                Credentials(password)
            ).open(server_url) as session2:
                request_stream2 = AnotherRequestStream()
                session2.messaging.add_stream_handler(path, request_stream2, addressed=True)

                async with sessions().principal("control").credentials(
                    Credentials(password)
                ).open(server_url) as session3:
                    request_callback = RequestCallback()
                    session3.messaging.add_filter_response_handler(
                        "$Principal is 'admin'", request_callback
                    )
                    await session3.messaging.send_request_to_filter(
                        "$Principal EQ 'admin'",
                        path,
                        diffusion.datatypes.STRING("Hello"),
                    )


class SimpleRequestStream(RequestHandler):
    def __init__(self):
        super().__init__(
            self.on_request,
            diffusion.datatypes.STRING,
            diffusion.datatypes.STRING,
        )

    # noinspection PyUnusedLocal
    async def on_request(self, request: str, **kwargs):
        print(f"{type(self).__qualname__}: Received message: {request}.")
        return "Goodbye"

    async def on_close(self):
        pass

    async def on_error(self, error_reason):
        pass


class AnotherRequestStream(RequestHandler):
    def __init__(self):
        super().__init__(
            self.on_request,
            diffusion.datatypes.STRING,
            diffusion.datatypes.STRING,
        )

    # noinspection PyUnusedLocal
    async def on_request(self, request: str, **kwargs):
        print(f"{type(self).__qualname__}: Received message: {request}.")
        return "I'm not supposed to receive a message."

    async def on_close(self):
        pass

    async def on_error(self, error_reason):
        pass


class RequestCallback(EventStreamHandler):
    def __init__(self):
        super().__init__(
            on_error=self.on_response_error, response=self.on_response
        )

    # noinspection PyUnusedLocal
    async def on_response(
        self,
        *,
        path: str,
        response: diffusion.datatypes.AbstractDataType,
        session_id: SessionId,
        received: int,
        expected: int,
        **kwargs,
    ):
        print(f"{type(self).__qualname__}: Received response: {response}.")

    async def on_response_error(self, code: int, description: str):
        pass


if __name__ == "__main__":
    asyncio.run(
        MessageToSessionFilter().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
