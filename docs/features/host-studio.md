# Feature: host studio

## Mục tiêu

Host quản lý quiz, tạo draft, publish quiz và mở live room từ một màn hình điều
hành duy nhất.

## Entry point

- Route: `/host`, bảo vệ bởi host route guard.
- Dữ liệu catalog dùng `httpResource`; form authoring dùng Angular Signal Forms.
- Studio có ba khu vực: quiz catalog, quiz authoring và recent rooms.

## Quiz catalog

- `GET /api/quizzes` trả quiz của host.
- Card hiển thị title, số câu và status `DRAFT`/`PUBLISHED`.
- Draft có action **Xuất bản**.
- Published quiz có action **Mở phòng**.
- Sau publish, catalog được reload; mở room thành công điều hướng tới room.

## Quiz authoring

- Host nhập title và xây dựng danh sách câu hỏi tạm thời.
- Mỗi câu có các option A–D với một đáp án đúng và thời gian giới hạn; số câu tối đa,
  số option và khoảng thời gian theo [domain rules](../domain.md), bound validate
  theo [REST contract](../contracts/rest-api.md#catalog-host).
- Có thể xóa câu trước khi lưu.
- **Lưu bản nháp** gọi `POST /api/quizzes` và xóa danh sách câu đã lưu sau success; các field editor còn lại giữ nguyên.
- Quiz đã publish không chỉnh sửa tại chỗ; room copy immutable question content khi
  được tạo.

## Open room và idempotency

- **Mở phòng** gọi `POST /api/rooms` với `quizId` và một `commandId` được Studio giữ
  ổn định cho quiz đó trong bộ nhớ cho tới khi mở phòng thành công.
- Nếu response mất, retry cùng commandId phải mở lại cùng room, không tạo room thứ hai.
  Sau khi thành công, lần **Mở phòng** tiếp theo dùng commandId mới nên tạo room mới
  cho cùng quiz.
- Sau snapshot thành công, navigate `/room/{id}`.
- Quiz draft creation hiện chưa có commandId server-side; khi timeout mơ hồ, reload
  catalog trước khi retry để tránh tạo draft trùng.

## UI state và failure behavior

| State | Behavior |
|---|---|
| Loading catalog/history | Catalog có loading text; history chưa có loading/empty state riêng |
| Form invalid | Khóa action dựa trên Signal Forms; error từng field/giới hạn text phía UI còn thiếu |
| Mutation busy | Khóa button để chặn double-click |
| `QUIZ_NOT_PUBLISHED` | Không mở room, yêu cầu publish trước |
| Storage/network error | Giữ form draft đang soạn, hiển thị retryable alert |
| Unauthorized | Guard chặn vào route; lỗi session trong route hiển thị alert, chưa tự redirect |

## Acceptance criteria

- STUDIO-01: Guest không render được Studio qua route guard.
- STUDIO-02: Không lưu quiz nếu thiếu title, câu hỏi hoặc option.
- STUDIO-03: Publish chỉ tác động quiz của host hiện tại.
- STUDIO-04: Room tạo từ quiz published không bị ảnh hưởng bởi thay đổi catalog sau đó.
- STUDIO-05: Retry open-room không tạo room duplicate.

## Contracts

Xem [REST catalog và room contract](../contracts/rest-api.md),
[backend architecture](../architecture/backend.md) và [Live room](live-room.md).
