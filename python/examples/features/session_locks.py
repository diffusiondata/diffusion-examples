import asyncio
import dataclasses
import traceback
import typing

import diffusion
from diffusion.session import SessionLockScope
from diffusion.session.locks.session_locks import SessionLock

LOCK_NAME = "lockA"


async def main(
    server_url: str = "<url>",
    principal: str = "<principal>",
    password: str = "<password>",
):
    async with diffusion.Session(
        url=server_url, principal=principal, credentials=diffusion.Credentials(password)
    ) as session1, diffusion.Session(
        url=server_url, principal=principal, credentials=diffusion.Credentials(password)
    ) as session2:
        print("Sessions 1 and 2 have been created.")
        await ContendedCluster(session1, session2).acquire_lock_session1()


# noinspection PyBroadException
@dataclasses.dataclass
class ContendedCluster(object):
    session1: diffusion.Session
    session2: diffusion.Session
    _session_lock1: typing.Optional[SessionLock] = dataclasses.field(init=False, default=None)
    _session_lock2: typing.Optional[SessionLock] = dataclasses.field(init=False, default=None)

    async def acquire_lock_session1(self):
        try:
            print("Requesting lock 1...")
            self._session_lock1 = await self.session1.lock(
                LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS
            )
            print("Lock 1 has been acquired.")
            asyncio.create_task(
                self.acquire_lock_session2()
            )  # spawn as a "background" task
            await asyncio.sleep(1)
            await self.release_lock1()
        except Exception as ex:
            print(f"Failed to get lock 1 : {traceback.format_exc()}.")

    async def acquire_lock_session2(self):
        try:
            print("Requesting lock 2...")
            self._session_lock2 = await self.session2.lock(
                LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS
            )
            print("Lock 2 has been acquired.")
            await asyncio.sleep(1)
            await self.release_lock2()
        except Exception:
            print(f"Failed to get lock 2 : {traceback.format_exc()}.")

    async def release_lock1(self):
        try:
            print("Requesting lock 1 release...")
            await self._session_lock1.unlock()
            print("Lock 1 has been released.")
        except Exception as ex:
            print(f"Failed to release lock 1 : {traceback.format_exc()}.")

    async def release_lock2(self):
        try:
            print("Requesting lock 2 release...")
            await self._session_lock2.unlock()
            print("Lock 2 has been released.")
        except Exception as ex:
            print(f"Failed to release lock 2 : {traceback.format_exc()}.")


asyncio.run(
    main(server_url="ws://localhost:8080", principal="control", password="password")
)
