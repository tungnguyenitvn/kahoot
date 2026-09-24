# Feature documentation

Feature được tổ chức theo user journey và capability, thay vì gom tất cả UI vào một
tài liệu tổng hợp. Mỗi feature có route/màn hình chính, state, action, error và
acceptance criteria riêng.

| Feature | Route/màn hình | Nội dung |
|---|---|---|
| [Login](login.md) | `/login` | Host authentication, session và logout |
| [Join room](join-room.md) | `/` | Guest vào phòng bằng PIN và nickname |
| [Host studio](host-studio.md) | `/host` | Tạo quiz, publish và mở room |
| [Live room](live-room.md) | `/room/:id` | Lobby, question, submit answer, reveal, ranking và realtime |
| [Room history](room-history.md) | `/host` (panel) | Danh sách phòng và kết quả archive |

Các invariant xuyên feature (scoring, concurrency, privacy, archive, fail-closed) có ID
`LIVE-xx` và nằm trong [domain rules](../domain.md#acceptance-invariants); feature
docs chỉ tham chiếu, không phát biểu lại.

Feature docs trả lời “người dùng đang cố đạt mục tiêu gì và hệ thống phải phản hồi
ra sao”; architecture/contracts trả lời “hệ thống thực hiện điều đó bằng boundary
nào”. Hai lớp tài liệu phải được cập nhật cùng nhau khi thêm hoặc đổi màn hình.

Mỗi acceptance criterion có ID ổn định theo prefix của feature (LOGIN, JOIN, STUDIO,
ROOM, ANSWER, HIST) hoặc LIVE cho invariant domain. Một hành vi chỉ có một ID; ID không
được đánh số lại. Test, issue và PR tham chiếu ID thay vì diễn giải lại; ma trận
ID → test nằm trong [testing](../development.md#testing-and-evidence). Feature docs viết bằng tiếng
Việt theo [chính sách ngôn ngữ](../README.md#language-policy); tên UI, mã lỗi và
identifier giữ nguyên, số liệu chỉ link tới tài liệu sở hữu.

## Retired IDs

ID dưới đây đã được hợp nhất vào invariant domain; không định nghĩa lại và không tái
sử dụng số của chúng.

| ID cũ | Thay bằng |
|---|---|
| ROOM-01 | LIVE-03 |
| ROOM-02 | LIVE-03 |
| ROOM-05 | LIVE-04 |
| ROOM-06 | LIVE-07 |
| ANSWER-02 | LIVE-03 |
| ANSWER-03 | LIVE-02 |
| ANSWER-05 | LIVE-04 |
| HIST-03 | LIVE-06 |
| STUDIO-04 | LIVE-01 |
