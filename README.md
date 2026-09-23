# Quiz Room — sample game kiểu Kahoot

[![ci](https://github.com/tungnguyenitvn/kahoot/actions/workflows/ci.yml/badge.svg)](https://github.com/tungnguyenitvn/kahoot/actions/workflows/ci.yml)
[![release](https://github.com/tungnguyenitvn/kahoot/actions/workflows/release.yml/badge.svg)](https://github.com/tungnguyenitvn/kahoot/actions/workflows/release.yml)

Sample end-to-end dùng **Java 24 + Spring Boot 4.1.1 + Gradle 8.14.3 + Angular 22**. Host tạo quiz, mở phòng; người chơi vào bằng PIN, trả lời theo thứ tự và nhận điểm theo bậc. REST xử lý command; WebSocket phát state realtime. Redis giữ trạng thái live và leaderboard, PostgreSQL giữ tài khoản, quiz bất biến và lịch sử.

## Chạy bằng Docker

Cần Docker Engine, Docker Compose v2 và Bash (WSL trên Windows). Sao chép `.env.example` thành `.env` nếu muốn đổi tài khoản seed, sau đó:

```bash
./scripts/bootstrap             # build image backend và frontend
docker compose up               # PostgreSQL, Redis, backend, Angular
```

Mở `http://localhost:4200`; health endpoint của backend là `http://localhost:8080/actuator/health`. Tài khoản host mặc định là `host@example.test` / `local-quiz-only`. Host vào **Host studio**, tạo quiz tối thiểu một câu, xuất bản rồi mở phòng. Người chơi mở tab khác, nhập PIN sáu chữ số và tên. Dừng bằng `Ctrl+C`; dữ liệu dev nằm trong volume Docker.

Gradle wrapper (`backend/gradlew`, `backend/gradle/wrapper`) và `frontend/package-lock.json` nằm trong cây nguồn; entrypoint chỉ bootstrap lại wrapper nếu thiếu và dùng `npm ci` khi có lockfile. Chạy công cụ ngoài Docker cần JDK 24, Gradle 8.14.3, Node theo `engines` trong `frontend/package.json` và TypeScript 6.0.x.

## Kiểm tra

```bash
./scripts/test                 # Redis/PostgreSQL tạm thời + backend unit/integration
./scripts/verify               # cổng đầy đủ: compose config, Lua smoke, backend, check-docs, Angular test/build
node scripts/check-docs.mjs    # lint tài liệu, chạy được không cần Docker
node --test frontend/tests/*.test.mjs
texlua scripts/test-room.lua . # smoke test Lua với Redis double; texlua hoặc Lua 5.3/5.4 bất kỳ
```

Các lệnh offline không kiểm tra Spring, Redis, PostgreSQL, browser hay WebSocket thật. `./scripts/verify` dùng compose cô lập, không đụng volume dev. Phạm vi từng gate và ma trận acceptance ID → test: [testing](docs/development/testing.md).

## Release

CI (`ci.yml`) chạy `scripts/verify` và `scripts/smoke-release` trên mọi PR và push lên
`main`. Gắn tag `vX.Y.Z` rồi push tag: workflow `release` verify lại cây được tag, build
image release đa tầng (`backend/Dockerfile.release`, `frontend/Dockerfile.release`),
smoke test stack `compose.release.yaml`, đẩy image lên GHCR
(`ghcr.io/tungnguyenitvn/kahoot-backend`, `ghcr.io/tungnguyenitvn/kahoot-frontend`)
và tạo GitHub Release kèm jar. Chạy stack từ image đã publish:

```bash
DB_PASSWORD=... DEMO_PASSWORD=... PUBLIC_ORIGIN=http://localhost:8081 docker compose -f compose.release.yaml up
```

Stack release chưa có TLS; đặt TLS terminator phía trước và chỉnh `PUBLIC_ORIGIN`,
`COOKIE_SECURE` theo [deployment](docs/architecture/deployment.md).

## Tài liệu và workflow AI

Bắt đầu tại [bản đồ tài liệu](docs/README.md). Architecture chỉ giữ cấu trúc chung;
module design chứa runtime/failure paths; feature và contracts có trách nhiệm riêng.
[Workflow](docs/development/workflow.md) và AGENTS.md quy định context, scope và
Definition of Done cho người và AI. [Verification status](docs/verification/README.md)
ghi kết quả mới nhất của từng gate kèm revision và môi trường.

## Thiết kế

- [Feature index](docs/features/README.md) · [invariant live quiz](docs/domain/game.md#acceptance-invariants) · [quy tắc game](docs/domain/game.md)
- [Tổng quan kiến trúc](docs/architecture/overview.md) · [Backend architecture](docs/architecture/backend.md) · [Frontend architecture](docs/architecture/frontend.md) · [Redis live state](docs/adr/0002-redis-game-state.md)
- [Contracts index](docs/contracts/README.md) · [REST API](docs/contracts/rest-api.md) · [Redis room](docs/contracts/redis-room.md) · [WebSocket](docs/contracts/websocket.md) · [Realtime delivery](docs/modules/realtime.md)
- [Kiểm thử và traceability](docs/development/testing.md) · [vận hành](docs/operations/runbook.md)

Đây là sample một backend instance. Redis Lua đảm bảo mỗi command trong một phòng là nguyên tử tại Redis; WebSocket chỉ phát snapshot sau khi command thành công, không quyết định điểm hay thứ tự. Stream được worker ghi tuần tự sang PostgreSQL. Sample không tuyên bố HA, đa vùng, hay durability tuyệt đối khi Redis mất dữ liệu.
