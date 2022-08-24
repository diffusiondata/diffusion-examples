import asyncio
import diffusion
import diffusion.datatypes

server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")

path = "foo/bar"
topic_type = diffusion.datatypes.STRING
value = "bla bla"


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():
    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # adding a topic, setting its value
        add_response = await session.topics.add_topic(path, topic_type)

        if add_response == session.topics.CREATED:
            print(f"Topic {path} successfully created.")
        if add_response == session.topics.EXISTS:
            print(f"Topic {path} already exists.")

        await session.topics.set_topic(path, value, specification=topic_type)
        print(f"Topic {path} successfully set to {value}")

if __name__ == "__main__":
    asyncio.run(main())
