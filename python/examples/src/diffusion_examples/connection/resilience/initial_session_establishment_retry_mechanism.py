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
from diffusion.session.retry_strategy import RetryStrategy

from diffusion_examples.utils.program import Example



class InitialSessionEstablishmentRetryMechanism(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        # Create an initial session establishment retry strategy.
        # It will attempt 5 times to connect to the Diffusion server,
        # with 250 milliseconds interval between attempts.
        retry_strategy = RetryStrategy(interval=250, attempts=5)
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).initial_retry_strategy(retry_strategy).open(server_url) as session:
            print(f"Connected. Session Identifier: {session.session_id}.")
            # Insert work here...


if __name__ == "__main__":
    asyncio.run(
        InitialSessionEstablishmentRetryMechanism().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
