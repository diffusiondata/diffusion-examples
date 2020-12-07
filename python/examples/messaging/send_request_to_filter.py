""" Example of sending a request to a session filter. """
import asyncio
import diffusion

# Diffusion server connection information; same for both sessions
# adjust as needed for the server used in practice
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# filter response handler function
def on_filter_response(response, **kwargs):
    print("Received response from session '{session_id}':".format(**kwargs))
    print(f"    {response}")
    print(" - Request was sent to {filter} on path {path}".format(**kwargs))
    print(" - Received {received} of {expected} response(s).".format(**kwargs))


filter_response_handler = diffusion.handlers.EventStreamHandler(
    response=on_filter_response
)

# request properties
request = "Pushme Pullyou"
path = "path"
request_type = diffusion.datatypes.STRING  # datatype of the request


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():

    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # specifying the session filter
        # this is a very simple filter, addressing all the sessions
        # with the same principal as the current session
        session_filter = f"$Principal is '{principal}'"

        # adding filter response handler
        session.messaging.add_filter_response_handler(
            session_filter=session_filter, callback=on_filter_response
        )

        # sending the request and receiving the number of expected responses
        print(f"Sending request: '{request}' to session filter `{session_filter}`...")
        try:
            response = await session.messaging.send_request_to_filter(
                session_filter=session_filter,
                path=path,
                request=request_type(request),
            )
        except diffusion.DiffusionError as ex:
            print(f"ERROR: {ex}")
        else:
            print(f"... expecting {response} response(s) ...")

        # The response received from the server is the number of sessions that
        # match the filter, i.e. that the request will be forwarded to. All of
        # these sessions that have a handler for the given path should send
        # a response, which will be handled by the filter response handlers
        # added above.

        # keeping the session alive to provide time for all responses to arrive
        await asyncio.sleep(60)


if __name__ == "__main__":
    asyncio.run(main())
