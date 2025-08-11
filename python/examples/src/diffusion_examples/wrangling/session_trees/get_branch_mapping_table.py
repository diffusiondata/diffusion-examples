"""
Copyright © 2024 Diffusion Data Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import asyncio

import diffusion
from diffusion.session import Session
from diffusion.features.control.session_trees.branch_mapping_table import (
    BranchMappingTable,
)
from diffusion_examples.utils.program import Example

class GetBranchMappingTable(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with Session(
            server_url,
            principal=principal,
            credentials=diffusion.Credentials(password),
        ) as session:
            # noinspection DuplicatedCode
            session_trees = session.session_trees
            table = (
                BranchMappingTable.Builder()
                .add_branch_mapping(
                    "$Principal is 'admin'", "my/topic/path/for/admin"
                )
                .add_branch_mapping(
                    "$Principal is 'control'", "my/topic/path/for/control"
                )
                .add_branch_mapping(
                    "$Principal is ''", "my/topic/path/for/anonymous"
                )
                .create("my/personal/path")
            )

            await session_trees.put_branch_mapping_table(table)

            table2 = (
                BranchMappingTable.Builder()
                .add_branch_mapping(
                    "$Transport is 'WEBSOCKET'",
                    "my/alternate/path/for/websocket",
                )
                .add_branch_mapping(
                    "$Transport is 'HTTP_LONG_POLL'",
                    "my/alternate/path/for/http",
                )
                .add_branch_mapping(
                    "$Transport is 'TCP'", "my/alternate/path/for/tcp"
                )
                .create("my/alternate/path")
            )

            await session_trees.put_branch_mapping_table(table2)

            list_session_tree_branches = (
                await session_trees.get_session_tree_branches_with_mappings()
            )
            for session_tree_branch in list_session_tree_branches:
                print(f"{session_tree_branch}:")
                branch_mapping_table = (
                    await session_trees.get_branch_mapping_table(
                        session_tree_branch
                    )
                )
                for branch_mapping in branch_mapping_table.branch_mappings:
                    print(
                        f"{branch_mapping.session_filter}: {branch_mapping.topic_tree_branch}"
                    )




if __name__ == "__main__":
    asyncio.run(
        GetBranchMappingTable().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
