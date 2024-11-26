"""
Copyright © 2023 - 2024 Diffusion Data Ltd.

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
from __future__ import annotations
import asyncio
import typing

from diffusion import sessions, Credentials
if typing.TYPE_CHECKING:
    from diffusion.session import State

from diffusion.session import SessionListener, Session

from diffusion_examples.utils.program import Example


class Listener(SessionListener):
    async def on_session_event(
        self,
        *,
        session: Session,
        old_state: typing.Optional[State],
        new_state: State,
    ):
        print(f"State changed from {old_state} to {new_state}.")


class SessionStateListener(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            session.add_listener(Listener())
            print(f"Connected. Session Identifier: {session.session_id}.")
            # Insert work here...


if __name__ == "__main__":
    asyncio.run(
        SessionStateListener().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
