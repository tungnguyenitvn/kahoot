# Getting started

## Prerequisites

- Docker Engine và Docker Compose v2.
- Bash (WSL được hỗ trợ trên Windows).
- Nếu chạy công cụ riêng: JDK 24, Gradle 8.14.3, Node 24.15.0 và npm. Angular 22 yêu cầu TypeScript 6.0.x.

Mọi service và dependency của sample được dựng bằng Docker:

```bash
cp .env.example .env       # tùy chọn
./scripts/bootstrap
docker compose up
```

Mở `http://localhost:4200`. Vào Host studio bằng tài khoản seed, tạo quiz, xuất bản, mở phòng; cửa sổ khác tham gia bằng PIN. Phòng dùng REST cho command và WebSocket cho state realtime; health endpoint là `http://localhost:8080/actuator/health`.

## Kiểm tra cô lập

`./scripts/test` khởi động PostgreSQL và Redis tạm thời, chạy test backend unit/integration rồi tự dọn container. `./scripts/verify` chạy cùng backend gate, build frontend và test policy/lifecycle và liên kết tài liệu. Nó không truy cập volume dev.

Nếu Docker không chạy, có thể chạy smoke test Lua (texlua hoặc bất kỳ Lua 5.3/5.4 nào):

```bash
texlua scripts/test-room.lua .
node --test frontend/tests/*.test.mjs
```

Các lệnh offline này không kiểm tra Spring, Redis server, PostgreSQL, browser hay WebSocket thật. `frontend/package-lock.json` được container frontend (Node 24.15.0) sinh ra khi chạy `./scripts/verify` ngày 2026-09-23 và nằm trong cây nguồn; commit nó cùng mã nguồn để CI dùng `npm ci` (entrypoint tự chọn `npm ci` khi lockfile tồn tại, ngược lại `npm install`).
