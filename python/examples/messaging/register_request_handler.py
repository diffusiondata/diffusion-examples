""" Example of setting a request handler. """
import asyncio
import diffusion

# Diffusion server connection information;
# adjust as needed for the server used in practice.
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# handler callback function
def path_request_handler(request: str, context=None) -> str:
    return f"Hello there, {request}!"


path = "path"


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():

    # creating the session
    with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # registering the request handler
        await session.messaging.register_request_handler(
            path,
            callback=path_request_handler,
            request_type=diffusion.datatypes.STRING,
            response_type=diffusion.datatypes.STRING,
        )

        # For the duration of the session, any requests of the right type sent
        # to the path will be received by the session, and then processed and
        # responded to by the callback function registered above.

        # keeping the session alive for two minutes
        await asyncio.sleep(120)


if __name__ == "__main__":
    asyncio.run(main())
