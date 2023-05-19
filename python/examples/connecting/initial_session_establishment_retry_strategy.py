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
import traceback

import diffusion
from diffusion.session.retry_strategy import RetryStrategy

# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
from diffusion.session import SessionEstablishmentException


async def main():
    # tag::initial_retry_strategy[]
    # Diffusion server connection information; same for both sessions
    # adjust as needed for the server used in practice
    server_url = "ws://localhost:8080"
    principal = "admin"
    credentials = diffusion.Credentials("password")
    # Create an initial session establishment retry strategy.
    # It will attempt 10 times to connect to the Diffusion server,
    # with 5 seconds interval between attempts.
    initial_retry_strategy = RetryStrategy(attempts=10, interval=5)

    # starting a session provided by using the results of
    # SessionFactory.open() as an async context manager
    try:
        async with (
                diffusion.sessions()
                .principal(principal)
                .credentials(credentials)
                .initial_retry_strategy(initial_retry_strategy)
                .open(server_url)
        ) as session:
            print(f"Connected. Session Identifier: {session.session_id} (via context manager)")

    except SessionEstablishmentException:
        print(f"Failed to open session: {traceback.format_exc()}")
        raise

    # starting a session provided by using the results of
    # awaiting SessionFactory.open() as a Session
    try:
        session = (
            await diffusion.sessions()
            .principal(principal)
            .credentials(credentials)
            .initial_retry_strategy(initial_retry_strategy)
            .open(server_url)
        )
        print(f"Connected. Session Identifier: {session.session_id} (via await)")
    except SessionEstablishmentException:
        print(f"Failed to open session: {traceback.format_exc()}")
        raise
    finally:
        await session.close()

    # end::initial_retry_strategy[]


if __name__ == "__main__":
    asyncio.run(main())
