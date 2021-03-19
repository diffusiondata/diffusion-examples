import asyncio
import diffusion

server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

path = "foo/bar"
topic_type = diffusion.datatypes.STRING


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():
    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # adding a topic
        await session.topics.add_topic(path, topic_type)

        # removing the topic
        removed = await session.topics.remove_topic(path)
        print(f"Removed {removed} topic(s)")


if __name__ == "__main__":
    asyncio.run(main())
