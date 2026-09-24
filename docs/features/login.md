# Feature: host login

## Mục tiêu

Host đăng nhập bằng session server-side để tạo quiz, mở room và xem lịch sử. Guest
không cần login để tham gia game.

## Entry point và navigation

- Route: `/login`.
- App shell hiển thị link **Đăng nhập** khi chưa có host identity.
- Login thành công redirect tới `/host`.
- Host logout bằng header action, server xóa session rồi redirect về `/`.

## Main flow

1. Frontend gọi `GET /api/auth/csrf` để nhận CSRF cookie.
2. Host nhập username/email và password.
3. Frontend gửi `POST /api/auth/login` với form fields và CSRF header.
4. Backend tạo Spring Security session; frontend gọi lại `GET /api/auth/me`.
5. Route guard cho phép `/host` khi `identity.host === true`.

## UI state

| State | Behavior |
|---|---|
| Idle | Form username/password, submit enabled khi hợp lệ |
| Submitting | Khóa button, hiển thị `Đang đăng nhập…` |
| Invalid credentials | Hiển thị `INVALID_CREDENTIALS`, không tiết lộ account tồn tại hay không |
| Network/server error | Giữ username, xóa password, cho phép retry |
| Authenticated | Xóa password khỏi memory UI và navigate `/host` |

## Security rules

- Session cookie do browser quản lý, không copy vào localStorage hoặc URL.
- CSRF token không được log hoặc render trong UI.
- `Auth.ready()` phải có thể chạy lại sau reload để khôi phục identity.
- Host guard không thay thế authorization ở backend; mọi catalog/history command vẫn
  kiểm tra role server-side.
- Màn hình không hiển thị credential demo; tài khoản seed chỉ được mô tả trong README
  và `.env.example`.

## Acceptance criteria

- LOGIN-01: Reload sau login vẫn nhận diện host bằng cookie session.
- LOGIN-02: Guest truy cập `/host` bị redirect về `/login`.
- LOGIN-03: Logout trả `204`, xóa session và làm mất quyền host ở request tiếp theo.
- LOGIN-04: Login lỗi không làm mất route hoặc dữ liệu nhập ngoài password.
- LOGIN-05: Template login không nhúng credential demo.

## Contracts

Xem [REST API identity contract](../contracts/rest-api.md#identity-and-security) và
[engineering conventions](../development.md#conventions).
