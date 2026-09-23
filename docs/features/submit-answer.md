# Feature: submit answer

Trạng thái: yêu cầu đã chấp nhận; đã có implementation; phạm vi verification ghi trong
[evidence](../verification/refactor-review.md). Đây là một capability bên trong màn hình
[live room](live-room.md), không phải route mới.

## Acceptance criteria

- ANSWER-01: Player đang active chọn một option trong QUESTION hiện tại.
- ANSWER-02: Mỗi participant/round được chấp nhận một option; gửi lại cùng option trả về
  acceptance ban đầu, đổi option bị từ chối và không tạo thêm score/event.
- ANSWER-03: Đồng hồ browser không quyết định deadline hay thứ tự đúng.
- ANSWER-04: Khi kết quả chưa rõ, retry dùng nguyên option/round/commandId ban đầu. Round
  mới xóa pending UI đã lỗi thời; receipt đã chấp nhận vẫn còn ở server.
- ANSWER-05: Việc chấp nhận không tiết lộ đúng/sai hay điểm trước reveal.
- ANSWER-06: Reconnect chỉ làm mới state, không bao giờ tự gửi answer.
- ANSWER-07: Timeout mạng, lỗi 5xx và 408/429 giữ pending; lỗi validation/quyền terminal
  xóa pending. Reload trang làm mất bộ nhớ chưa gửi. Backend không trả 429 cho bất kỳ
  request nào; giữ pending ở 429 là để tương thích với rate limiter hạ tầng nếu có.

## Boundaries và evidence

[REST idempotency](../contracts/rest-api.md#idempotency) định nghĩa identity và error:
server dedupe answer theo room/round/participant, `commandId` của answer là metadata
audit. [Gameplay](../modules/gameplay.md) sở hữu scoring; [realtime](../modules/realtime.md)
sở hữu recovery. Lua smoke phủ các invariant tuần tự; GameIntegrationTest có kịch bản
concurrency với Redis thật. Browser E2E cho store/template chưa được implement.
