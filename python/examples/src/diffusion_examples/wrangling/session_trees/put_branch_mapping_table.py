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
from diffusion.features.control.session_trees.branch_mapping_table import (
    BranchMappingTable,
)

from diffusion_examples.utils.program import Example


class PutBranchMappingTable(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with diffusion.sessions().principal(principal).credentials(
            diffusion.Credentials(password)
        ).open(server_url) as session:
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

            await session.session_trees.put_branch_mapping_table(table)
            print(
                "Branch mapping table created for session tree branch "
                f"'{table.session_tree_branch}'."
            )



if __name__ == "__main__":
    asyncio.run(
        PutBranchMappingTable().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
