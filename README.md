# Quiz Room — sample game kiểu Kahoot

Sample end-to-end dùng **Java 24 + Spring Boot 4.1.1 + Gradle 8.14.3 + Angular 22**. Host tạo quiz, mở phòng; người chơi vào bằng PIN, trả lời theo thứ tự và nhận điểm theo bậc. REST xử lý command; WebSocket phát state realtime. Redis giữ trạng thái live và leaderboard, PostgreSQL giữ tài khoản, quiz bất biến và lịch sử.

## Chạy bằng Docker

Cần Docker Compose v2. Sao chép `.env.example` thành `.env` nếu muốn đổi tài khoản seed, sau đó:

```bash
./scripts/bootstrap             # build image; lần đầu chạy compose tạo Gradle wrapper thật trong backend/
docker compose up               # PostgreSQL, Redis, backend, Angular
```

Mở `http://localhost:4200`. Tài khoản host mặc định là `host@example.test` / `local-quiz-only`. Host vào **Host studio**, tạo quiz tối thiểu một câu, xuất bản rồi mở phòng. Người chơi mở tab khác, nhập PIN sáu chữ số và tên. Dừng bằng `Ctrl+C`; dữ liệu dev nằm trong volume Docker.

Wrapper Gradle được bootstrap trong container từ distribution chính thức rồi lưu vào `backend/` khi backend khởi động, vì archive này không nhúng binary wrapper. Sau lần bootstrap có thể chạy `backend/gradlew` trực tiếp trong image.

## Kiểm tra

```bash
./scripts/test                 # Redis/PostgreSQL tạm thời + backend unit/integration
./scripts/verify               # thêm docker compose config và Angular test/build
texlua scripts/test-room.lua .
```

Lệnh cuối là smoke test Lua chạy offline với Redis double (texlua hoặc bất kỳ Lua 5.3/5.4 nào); nó kiểm tra các invariant cốt lõi nhưng không thay thế Redis thật. `./scripts/verify` là cổng kiểm tra đầy đủ và không dùng volume dữ liệu dev.

## Tài liệu và workflow AI

Bắt đầu tại [bản đồ tài liệu](docs/README.md). Architecture chỉ giữ cấu trúc chung;
module design chứa runtime/failure paths; feature và contracts có trách nhiệm riêng.
[Workflow](docs/development/workflow.md) và AGENTS.md quy định context, scope và
Definition of Done cho người và AI. [Verification](docs/verification/refactor-review.md)
phân biệt kiểm tra đã chạy với những gate còn thiếu.

## Thiết kế

- [Feature index](docs/features/README.md) · [Luồng live quiz](docs/features/live-quiz.md) · [quy tắc game](docs/domain/game.md)
- [Tổng quan kiến trúc](docs/architecture/overview.md) · [Backend architecture](docs/architecture/backend.md) · [Frontend architecture](docs/architecture/frontend.md) · [Redis live state](docs/adr/0002-redis-game-state.md)
- [Contracts index](docs/contracts/README.md) · [REST API](docs/contracts/rest-api.md) · [Redis room](docs/contracts/redis-room.md) · [WebSocket](docs/contracts/websocket.md)
- [WebSocket architecture notes](docs/architecture/websocket.md)
- [Bắt đầu và vận hành](docs/development/getting-started.md) · [kiểm thử](docs/development/testing.md)

Đây là sample một backend instance. Redis Lua đảm bảo mỗi command trong một phòng là nguyên tử tại Redis; WebSocket chỉ phát snapshot sau khi command thành công, không quyết định điểm hay thứ tự. Stream được worker ghi tuần tự sang PostgreSQL. Sample không tuyên bố HA, đa vùng, hay durability tuyệt đối khi Redis mất dữ liệu.
