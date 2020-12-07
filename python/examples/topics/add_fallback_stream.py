""" Example of sending a request to a session filter. """
import asyncio
import diffusion

# Diffusion server connection information; same for both sessions
# adjust as needed for the server used in practice
server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# stream callback functions
def on_update(*, old_value, topic_path, topic_value, **kwargs):
    print("Topic:", topic_path)
    if old_value is None:
        print("  Initial value:", topic_value)
    else:
        print("  Value updated")
        print("    Old value:", old_value)
        print("    New value:", topic_value)


def on_subscribe(*, topic_path, **kwargs):
    print(f"Subscribed to {topic_path}")


def on_unsubscribe(*, reason, topic_path, **kwargs):
    print(f"Unsubscribed from {topic_path} because {str(reason)}")


# example properties
topic_selector = "foo/bar"
topic_type = diffusion.datatypes.STRING

session_duration = 30

# fallback stream object
fallback_stream = diffusion.topics.ValueStreamHandler(
    data_type=topic_type,
    update=on_update,
    subscribe=on_subscribe,
    unsubscribe=on_unsubscribe,
)


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():

    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        print("Adding fallback stream")
        session.topics.add_fallback_stream(fallback_stream)

        print(f"Subscribing to {topic_selector}")
        await session.topics.subscribe(topic_selector)

        await asyncio.sleep(session_duration)

        print(f"Unsubscribing from {topic_selector}")
        await session.topics.unsubscribe(topic_selector)

        await asyncio.sleep(1)  # keep alive to display the unsubscription message


if __name__ == "__main__":
    asyncio.run(main())
