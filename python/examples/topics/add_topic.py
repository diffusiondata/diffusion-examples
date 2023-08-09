import asyncio
import diffusion
import diffusion.datatypes as datatypes

server_url = "ws://localhost:8080"
principal = "admin"
credentials = diffusion.Credentials("password")


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():
    # creating the session
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=credentials
    ) as session:

        # Create a topic from a topic type
        path = "topic/string"
        add_response = await session.topics.add_topic(path, diffusion.datatypes.STRING)

        print_response(add_response, path, session)

        # Create a topic from a topic specification, with optional properties
        path = "topic/integer"
        add_response = await session.topics.add_topic(
            path, datatypes.INT64.with_properties(VALIDATES_VALUES=True)
        )
        print_response(add_response, path, session)


def print_response(add_response, path, session):
    if add_response == session.topics.CREATED:
        print(f"Topic {path} successfully created.")
    if add_response == session.topics.EXISTS:
        print(f"Topic {path} already exists.")


if __name__ == "__main__":
    asyncio.run(main())
