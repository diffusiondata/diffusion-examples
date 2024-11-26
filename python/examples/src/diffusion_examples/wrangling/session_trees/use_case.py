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
import typing

from diffusion import sessions, Credentials
from diffusion.features.control.session_trees.branch_mapping_table import (
    BranchMappingTable,
)
import diffusion.datatypes
from diffusion_examples.utils.program import Example
from diffusion.features.topics.streams import ValueStreamHandler
from diffusion.features.topics.details.topic_specification import (
    TopicSpecification,
)



class UseCase(Example):
    async def run(
        self,
        server_url: str = "<url>",
        principal: str = "<principal>",
        password: str = "<password>",
    ):
        async with sessions().principal(principal).credentials(
            Credentials(password)
        ).open(server_url) as session:
            topic_specification = diffusion.datatypes.STRING
            await session.topics.add_and_set_topic(
                "my/topic/path/for/admin",
                topic_specification,
                "Good morning Administrator",
            )
            await session.topics.add_and_set_topic(
                "my/topic/path/for/control",
                topic_specification,
                "Good afternoon Control Client",
            )
            await session.topics.add_and_set_topic(
                "my/topic/path/for/anonymous",
                topic_specification,
                "Good night Anonymous",
            )

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

            topic_selector = ">my/personal/path"
            string_stream = StringStream()
            session.topics.add_value_stream(topic_selector, string_stream)
            await session.topics.subscribe(topic_selector)

            await asyncio.sleep(5)

            async with sessions().open(server_url) as session2:
                another_string_stream = AnotherStringStream()
                session2.topics.add_value_stream(
                    topic_selector, another_string_stream
                )
                await session2.topics.subscribe(topic_selector)

                await asyncio.sleep(5)

                list_session_tree_branches = (
                    await session.session_trees.get_session_tree_branches_with_mappings()
                )
                for session_tree_branch in list_session_tree_branches:
                    print(f"{session_tree_branch}:")
                    branch_mapping_table = (
                        await session.session_trees.get_branch_mapping_table(
                            session_tree_branch
                        )
                    )
                    for branch_mapping in branch_mapping_table.branch_mappings:
                        print(
                            f"{branch_mapping.session_filter}: "
                            f"{branch_mapping.topic_tree_branch}"
                        )


                await session2.topics.unsubscribe(topic_selector)
                await asyncio.sleep(5)



class StringStream(ValueStreamHandler):
    def __init__(self):
        super().__init__(
            diffusion.datatypes.STRING,
            update=self.on_value,
            subscribe=self.on_subscription,
            unsubscribe=self.on_subscription,
        )

    async def on_value(
        self, topic_path, topic_spec, old_value, topic_value
    ):
        print(
            f"{topic_path} changed from {old_value or 'NULL'} to {topic_value}."
        )

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_subscription(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
        reason: typing.Optional[typing.Any] = None,
    ):
        print(f"Subscribed to {topic_path}: {reason}.")

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_unsubscription(
        self,
        topic_path: str,
        topic_spec: TopicSpecification,
        topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
        reason: typing.Optional[typing.Any],
    ):
        print(f"Unsubscribed from {topic_path}: {reason}.")


class AnotherStringStream(ValueStreamHandler):
    def __init__(self):
        super().__init__(
            diffusion.datatypes.STRING,
            update=self.on_value,
            subscribe=self.on_subscription,
            unsubscribe=self.on_subscription,
        )

    async def on_value(
        self, topic_path, topic_spec, old_value, topic_value
    ):
        print(
            f"{topic_path} changed from {old_value or 'NULL'} to {topic_value}."
        )

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_subscription(
            self,
            topic_path: str,
            topic_spec: TopicSpecification,
            topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
            reason: typing.Optional[typing.Any] = None,
    ):
        print(f"Subscribed to {topic_path}.")

    # noinspection PyUnusedLocal,PyMethodMayBeStatic
    async def on_unsubscription(
            self,
            topic_path: str,
            topic_spec: TopicSpecification,
            topic_value: typing.Optional[diffusion.datatypes.AbstractDataType],
            reason: typing.Optional[typing.Any],
    ):
        print(f"Unsubscribed from {topic_path}: {reason}.")


if __name__ == "__main__":
    asyncio.run(
        UseCase().run(
            server_url="ws://localhost:8080",
            principal="admin",
            password="password",
        )
    )
