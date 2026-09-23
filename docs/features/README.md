# Feature documentation

Feature được tổ chức theo user journey và capability, thay vì gom tất cả UI vào một
tài liệu tổng hợp. Mỗi feature có route/màn hình chính, state, action, error và
acceptance criteria riêng.

| Feature | Route/màn hình | Nội dung |
|---|---|---|
| [Login](login.md) | `/login` | Host authentication, session và logout |
| [Join room](join-room.md) | `/` | Guest vào phòng bằng PIN và nickname |
| [Host studio](host-studio.md) | `/host` | Tạo quiz, publish và mở room |
| [Live room](live-room.md) | `/room/:id` | Lobby, question, reveal, ranking và realtime |
| [Submit answer](submit-answer.md) | Trong Live room | Receipt, retry, deadline, privacy |
| [Room history](room-history.md) | `/host` (panel) | Danh sách phòng và kết quả archive |
| [Live quiz rules](live-quiz.md) | Cross-feature | Scoring, concurrency, privacy và archive invariants |

Feature docs trả lời “người dùng đang cố đạt mục tiêu gì và hệ thống phải phản hồi
ra sao”; architecture/contracts trả lời “hệ thống thực hiện điều đó bằng boundary
nào”. Hai lớp tài liệu phải được cập nhật cùng nhau khi thêm hoặc đổi màn hình.

Mỗi acceptance criterion có ID ổn định theo prefix của feature (LOGIN, JOIN, STUDIO,
ROOM, ANSWER, HIST, LIVE). Test, issue và PR tham chiếu ID thay vì diễn giải lại.
Feature docs viết bằng tiếng Việt theo [chính sách ngôn ngữ](../README.md#language-policy);
tên UI, mã lỗi, identifier và số liệu giữ nguyên, số liệu chỉ link tới
[bảng limits](../architecture/quality-and-risks.md#limits-and-timings).
