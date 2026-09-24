import { HttpErrorResponse } from '@angular/common/http';
/** Error code to message; codes are contract values (docs/contracts/rest-api.md), never parsed from text. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) return error.error?.code ?? (error.status === 0 ? 'Mất kết nối. Có thể thử lại cùng thao tác.' : `Yêu cầu thất bại (${error.status}).`);
  return error instanceof Error ? error.message : 'Đã có lỗi xảy ra.';
}
