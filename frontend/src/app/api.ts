import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Identity } from './models';
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) return error.error?.code ?? (error.status === 0 ? 'Mất kết nối. Có thể thử lại cùng thao tác.' : `Yêu cầu thất bại (${error.status}).`);
  return error instanceof Error ? error.message : 'Đã có lỗi xảy ra.';
}
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  get<T>(url: string) { return firstValueFrom(this.http.get<T>(url)); }
  post<T>(url: string, body: unknown) { return firstValueFrom(this.http.post<T>(url, body)); }
}
@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly api = inject(Api);
  readonly user = signal<Identity | null>(null);
  private pending?: Promise<void>;
  ready(): Promise<void> {
    if (this.user()) return Promise.resolve();
    return this.pending ??= this.refresh().finally(() => this.pending = undefined);
  }
  async refresh() {
    // Login/logout rotate CSRF tokens. Angular sends X-XSRF-TOKEN for relative URLs.
    await this.api.get('/api/auth/csrf');
    this.user.set(await this.api.get<Identity>('/api/auth/me'));
  }
  async login(username: string, password: string) {
    await this.ready();
    await this.api.post('/api/auth/login', new HttpParams().set('username', username).set('password', password));
    await this.refresh();
  }
  async logout() {
    await this.api.post('/api/auth/logout', {});
    this.user.set(null);
    await this.refresh();
  }
}
