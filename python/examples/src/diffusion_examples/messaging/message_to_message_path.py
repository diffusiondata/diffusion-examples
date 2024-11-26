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
from diffusion.messaging import RequestHandler
from diffusion_examples.utils.program import Example
import diffusion.datatypes


class MessageToMessagePath(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        path = "my/message/path"

        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            request_handler = SimpleRequestHandler()
            await session.messaging.add_request_handler(path, request_handler)

            async with sessions().principal(principal).credentials(
                Credentials(password)
            ).open(server_url) as session2:
                response = await session2.messaging.send_request_to_path(
                    path, diffusion.datatypes.STRING("Hello")
                )
                print(f"Received response: {response}.")


class SimpleRequestHandler(RequestHandler):
    def __init__(self):
        super().__init__(
            self.on_request,
            diffusion.datatypes.STRING,
            diffusion.datatypes.STRING,
        )

    # noinspection PyUnusedLocal
    async def on_request(
        self,
        request: str,
        *,
        conversation_id: int,
        sender_session_id: SessionId,
        path: str,
        **kwargs,
    ):
        print(f"Received message: {request}.")
        return "Goodbye"

    async def on_close(self):
        pass

    async def on_error(self, error_reason):
        pass


if __name__ == "__main__":
    asyncio.run(
        MessageToMessagePath().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
