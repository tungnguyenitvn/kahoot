# Live quiz: acceptance end-to-end

Đây là hành vi đã được chấp nhận, không phải tuyên bố đã verify toàn bộ. Quy tắc
chấm điểm, giới hạn và deadline chính xác là canonical trong [domain](../domain/game.md).

Hành trình: [login](login.md) → [studio](host-studio.md) → [live room](live-room.md);
guest [join](join-room.md) → [submit answer](submit-answer.md); host
[history](room-history.md).

- LIVE-01: Owner đã đăng nhập mở room từ quiz đã publish và được đóng băng; guest
  session join nguyên tử bằng PIN/nickname. PIN không bao giờ cấp quyền đọc hay quyền host.
- LIVE-02: Room chuyển phase theo [bảng state transitions](../domain/game.md#state-transitions);
  hết hạn có thể kết thúc room từ bất kỳ phase chưa FINISHED nào và bỏ qua REVEAL.
  Deadline và thứ tự đúng do server quyết định.
- LIVE-03: Hiệu ứng của answer được chấp nhận xảy ra đúng một lần cho mỗi participant/round.
  Các answer đúng đồng thời nhận thứ tự khác nhau; answer sai chỉ tiêu tốn lượt trả lời.
- LIVE-04: Response trong QUESTION không bao giờ lộ correct option, điểm chưa công bố hay rank.
- LIVE-05: REST mutate; WS và polling đồng bộ lại toàn bộ state đã lọc privacy.
  Mất notification không được làm thay đổi kết quả đã chấp nhận.
- LIVE-06: Archive áp dụng event theo thứ tự với SQL deduplication trước khi ACK.
- LIVE-07: Mất Redis state thì fail closed; không âm thầm fallback sang history chưa đầy đủ.

Phạm vi verification: [testing](../development/testing.md).
