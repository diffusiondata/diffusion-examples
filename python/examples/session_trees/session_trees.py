import asyncio
import diffusion.datatypes
from diffusion.features.control.session_trees.branch_mapping_table import (
    BranchMappingTable,
)

server_url = "ws://localhost:8080"
principal = "control"
credentials = diffusion.Credentials("password")

path = "foo/bar"
topic_type = diffusion.datatypes.STRING
value = "bla bla"


# Because Python SDK for Diffusion is async, all the code needs to be
# wrapped inside a coroutine function, and executed using asyncio.run.
async def main():
    # creating the session
    async with diffusion.Session(
        url=server_url, principal="control", credentials=credentials
    ) as session:

        # adding a topic, setting its value
        try:
            table = (
                BranchMappingTable.Builder()
                .add_branch_mapping("$Principal is 'control'", "target/1")
                .add_branch_mapping("all", "target/2")
                .create("source/path")
             )
            await session.session_trees.put_branch_mapping_table(table)

            print(f"Branch mapping table created for session tree branch '{table.session_tree_branch}'.")
        except Exception as ex:
            print(f"Failed to create branch mapping table : {ex}.")
            return

        try:
            print("Retrieving session tree branches.")
            list_session_tree_branches = await session.session_trees.get_session_tree_branches_with_mappings()
        except Exception as ex:
            print(f"Failed to retrieve session tree branches : {ex}.")
            return

        try:
            print(f"Retrieving branch mapping table:")

            for session_tree_branch in list_session_tree_branches:
                branch_mapping_table = await session.session_trees.get_branch_mapping_table(session_tree_branch)

                for branch_mapping in branch_mapping_table.branch_mappings:
                    print(f"Session tree branch: '{session_tree_branch}', Session filter: '{branch_mapping.session_filter}', Topic tree branch: '{branch_mapping.topic_tree_branch}'");
        except Exception as ex:
            print(f"Failed to retrieve a branch mapping : {ex}.")


if __name__ == "__main__":
    asyncio.run(main())
