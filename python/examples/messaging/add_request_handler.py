""" Example of setting a request handler. """
import asyncio
import diffusion

from diffusion.messaging import RequestHandler


# Diffusion server connection information;
# adjust as needed for the server used in practice.
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# handler callback function
def callback(request: str, context=None) -> str:
    return f"Hello there, {request}!"


path = "path"
request_type = diffusion.datatypes.STRING  # datatype of the request


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():

    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # registering the request handler
        print("Registering the request handler...")

        handler = RequestHandler(
            callback,
            request_type=request_type,
            response_type=request_type
        )

        try:
            await session.messaging.add_request_handler(
                path,
                handler=handler
            )

        except diffusion.DiffusionError as ex:
            print(f"ERROR: {ex}")
        else:
            print("... request handler registered")

            # For the duration of the session, any requests of the right type sent
            # to the path will be received by the session, and then processed and
            # responded to by the callback function registered above.

            # keeping the session alive for 15 seconds
            await asyncio.sleep(15)


if __name__ == "__main__":
    asyncio.run(main())
