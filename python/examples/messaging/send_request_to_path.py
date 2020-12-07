""" Example of sending a request to a path. """
import asyncio
import diffusion

# Diffusion server connection information;
# adjust as needed for the server used in practice.
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

# request properties
request = "Pushme Pullyou"
path = "path"
request_type = diffusion.datatypes.STRING  # datatype of the request


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():

    # Creating the session.
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # Sending the request and receiving the response.
        print(f"Sending request: '{request}'...")
        try:
            response = await session.messaging.send_request_to_path(
                path=path, request=request_type(request)
            )
        except diffusion.DiffusionError as ex:
            print(f"ERROR: {ex}")
        else:
            print(f"... received response '{response}'")


if __name__ == "__main__":
    asyncio.run(main())
