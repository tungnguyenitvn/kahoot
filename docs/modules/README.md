# Module design index

Read only modules affected by the task. This level contains algorithms, invariants,
failure paths and implementation links; [architecture](../architecture/backend.md)
contains the system-wide dependency map.

| Module / concern | Design | Source |
|---|---|---|
| Identity + Catalog (small modules) | [identity-catalog](identity-catalog.md) | backend identity/, catalog/ |
| Live gameplay and provisioning | [gameplay](gameplay.md) | backend gameplay/, redis/room.lua |
| PostgreSQL projection | [archive](archive.md) | backend archive/ |
| Realtime delivery and client lifecycle | [realtime](realtime.md) | RoomWebSocketHub, RoomConnection, RoomStore |

Split a document when its responsibility splits, not when another screen is added.
