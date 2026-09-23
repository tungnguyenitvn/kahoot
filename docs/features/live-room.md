# Feature: live room

## Mục tiêu

Host và player cùng tham gia một room realtime. Redis Lua quyết định membership,
deadline, receipt, thứ tự đúng và score; Angular chỉ hiển thị authoritative snapshot.

## Entry point và transport

- Route: `/room/:id`, dùng chung cho host và player.
- `RoomStore` ủy quyền `RoomConnection` gọi `GET /api/rooms/{id}` trước khi mở WebSocket.
- WebSocket `/ws/rooms/{id}` chỉ nhận `SYNC` và `PING`; mọi mutation dùng REST.
- Snapshot có `version`; state cũ hơn không được ghi đè state mới hơn.

## Shared layout

- Header hiển thị title, PIN và trạng thái realtime.
- Main stage thay đổi theo phase; các phase và transition hợp lệ nằm trong
  [bảng state transitions](../domain/game.md#state-transitions).
- Player list hiển thị active/removed và answered state.
- Leaderboard hiển thị số hàng theo [bảng limits](../architecture/quality-and-risks.md#limits-and-timings),
  cùng điểm dùng competition rank.
- Client không tự tính score, correct order hoặc deadline acceptance.

## Lobby

- Hiển thị số người chơi và tổng số câu hỏi.
- Host thấy **Bắt đầu**, chỉ enabled khi có player.
- Player thấy **Chờ người dẫn bắt đầu…**.
- Host kick player bằng REST; participant bị revoke nhận `REVOKED`.

## Question

- Hiển thị question number, text, options và countdown dựa trên `serverTime/deadline`.
- Player chọn option ở UI state tạm thời, sau đó gửi answer qua REST.
- Answer gửi `roundId`, `option` và `commandId`. Server dedupe theo round + participant;
  `commandId` chỉ là metadata audit ([idempotency contract](../contracts/rest-api.md#idempotency)).
  Khi response timeout, retry gửi lại đúng option và roundId; client giữ nguyên commandId
  để đối chiếu.
- Sau receipt, lựa chọn bị khóa và hiển thị accepted answer.
- Host không trả lời; host thấy **Chốt câu & công bố**.
- `correctOption` là `null`, không hiển thị điểm delta riêng tư trước reveal.

## Submit answer

Capability bên trong màn hình này, không phải route mới; implementation đã có, phạm vi
verification ghi trong [testing](../development/testing.md).

- ANSWER-01: Player đang active chọn một option trong QUESTION hiện tại.
- ANSWER-04: Khi kết quả chưa rõ, retry dùng nguyên option/round/commandId ban đầu. Round
  mới xóa pending UI đã lỗi thời; receipt đã chấp nhận vẫn còn ở server.
- ANSWER-06: Reconnect chỉ làm mới state, không bao giờ tự gửi answer.
- ANSWER-07: Timeout mạng, lỗi 5xx và 408/429 giữ pending; lỗi validation/quyền terminal
  xóa pending. Reload trang làm mất bộ nhớ chưa gửi. Backend không trả 429 cho bất kỳ
  request nào; giữ pending ở 429 là để tương thích với rate limiter hạ tầng nếu có.

Dedupe theo room/round/participant và privacy trước reveal là LIVE-03 và LIVE-04 trong
[domain rules](../domain/game.md#acceptance-invariants). [REST idempotency](../contracts/rest-api.md#idempotency)
định nghĩa identity và error; [gameplay](../modules/gameplay.md) sở hữu scoring,
[realtime](../modules/realtime.md) sở hữu recovery. Browser E2E cho store/template chưa
được implement.

## Reveal và finished

- Reveal hiển thị correct option và leaderboard đã công bố.
- Host thấy **Câu tiếp theo** hoặc **Kết thúc**; control có `roundId` fencing và
  `commandId` idempotency.
- Finished khóa answer/kick/control và hiển thị kết quả cuối cùng. Room cũng có thể
  FINISHED do hết hạn ở bất kỳ phase nào; khi đó không có bước reveal riêng.
- **Vào phòng khác** quay về Join room.

## Realtime, reconnect và error

1. STATE chỉ được apply nếu đúng room và version không cũ.
2. Lỗi REST ban đầu vẫn giữ polling để phục hồi; socket close hiển thị **Đang nối lại**,
   reconnect với backoff có giới hạn và vẫn poll REST định kỳ. Giá trị nằm trong
   [bảng limits](../architecture/quality-and-risks.md#limits-and-timings).
3. HTTP response và STATE có thể đến khác thứ tự; version guard hợp nhất an toàn.
4. Pending answer giữ nguyên tới khi nhận receipt hoặc terminal error.
5. `STALE_ROUND`, `DEADLINE_PASSED`, `ALREADY_ANSWERED` yêu cầu reconcile snapshot.
6. `REVOKED`, 401, 403 hoặc 404 dừng reconnect.

## Acceptance criteria

- ROOM-03: Stale round không tác động round mới.
- ROOM-04: Reconnect giữ pending command trong memory; reload khôi phục receipt đã chấp
  nhận từ server nhưng không giữ command chưa gửi.

Thứ tự đúng do server quyết định, dedupe answer, privacy trước reveal và fail-closed khi
mất Redis là các invariant LIVE-02, LIVE-03, LIVE-04 và LIVE-07 trong
[domain rules](../domain/game.md#acceptance-invariants).

## Contracts

Xem [WebSocket contract](../contracts/websocket.md), [REST API](../contracts/rest-api.md),
[Redis room](../contracts/redis-room.md) và [domain game rules](../domain/game.md).
