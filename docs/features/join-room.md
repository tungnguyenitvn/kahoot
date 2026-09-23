# Feature: join room

## Mục tiêu

Guest vào một live room bằng PIN sáu chữ số và nickname, không cần tài khoản. Một
browser guest identity được giữ trong session và gắn với một participant trong room.

## Entry point

- Route: `/`.
- Form gồm `PIN phòng` và `Tên hiển thị`.
- Thành công redirect tới `/room/{roomId}`.

## Main flow

1. Validate PIN đúng định dạng và nickname không rỗng, độ dài theo
   [REST contract](../contracts/rest-api.md#room-commands).
2. Gọi `GET /api/auth/csrf` và `GET /api/auth/me` để tạo/khôi phục guest session.
3. Gọi `POST /api/rooms/join` với `pin` và `name`.
4. Nhận room snapshot, sau đó khởi tạo Live room feature.

## UI state và error mapping

| State/error | Behavior |
|---|---|
| Invalid form | Khóa submit; chưa có cơ chế tự focus lỗi |
| Submitting | Khóa submit, hiển thị `Đang tham gia…` |
| `ROOM_NOT_FOUND` | PIN không tồn tại hoặc đã hết hạn |
| `NAME_TAKEN` | Yêu cầu nickname khác |
| `JOIN_CLOSED` | Room đã bắt đầu hoặc hết hạn |
| `ROOM_FULL` | Room đã đạt giới hạn participant, tính cả participant đã bị mời ra |
| Network error | Giữ input, cho phép retry cùng session |

## Rules

- PIN chỉ là locator, không phải credential.
- Retry không được tạo identity guest mới nếu session hiện tại còn tồn tại.
- Không hiển thị question, answer hoặc leaderboard trước khi join thành công.
- Quyền đọc snapshot và WebSocket membership được backend kiểm tra lại; frontend
  không tự coi việc biết PIN là đủ quyền.

## Acceptance criteria

- JOIN-01: PIN sai format không gọi API.
- JOIN-02: Nickname trùng bị từ chối atomically trong Redis.
- JOIN-03: Refresh sau join vẫn giữ participant identity và receipt của participant đó.
- JOIN-04: Guest không thể thực hiện host action sau khi vào room.

## Contracts

Xem [REST room contract](../contracts/rest-api.md#room-commands),
[Redis room contract](../contracts/redis-room.md) và [Live room](live-room.md).
