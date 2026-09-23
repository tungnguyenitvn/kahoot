# Feature: room history

## Mục tiêu

Host xem lại các room đã tạo và kết quả đã được archive bền vững trong PostgreSQL.
Đây là panel trong Host Studio, chưa phải route độc lập.

## Main flow

1. Studio gọi `GET /api/history` để lấy danh sách room gần đây của host; số lượng tối đa
   theo [REST contract](../contracts/rest-api.md#history-host).
2. Mỗi card hiển thị title, phase và link quay lại room nếu snapshot còn truy cập được.
3. Room `FINISHED` hiển thị action **Kết quả đã lưu**.
4. Action gọi `GET /api/history/{id}` và render participant/score từ archive.

## State và consistency

| State | Behavior |
|---|---|
| Loading | Chưa có placeholder/empty-state riêng cho history panel |
| `ARCHIVE_NOT_READY` | Hiện error code trong alert; cho phép reload, không thay bằng kết quả rỗng |
| Archived | Hiển thị score projection từ PostgreSQL |
| Storage/auth error | Giữ layout Studio và hiển thị retryable alert |

Archive có thể trễ so với live room. Redis live leaderboard và PostgreSQL history
không được trộn trong một response; archive worker replay Stream event idempotently
theo room version trước khi ACK.

## Acceptance criteria

- HIST-01: Host chỉ xem được history của chính mình.
- HIST-02: Room chưa FINISHED không được coi là archived result hoàn chỉnh.
- HIST-04: Kết quả archive vẫn đọc được sau khi Redis live keys được cleanup.

Replay hoặc worker retry không nhân đôi answer/score là invariant LIVE-06 trong
[domain rules](../domain/game.md#acceptance-invariants).

## Contracts

Xem [REST API](../contracts/rest-api.md), [Redis room contract](../contracts/redis-room.md)
và [archive design](../modules/archive.md).
