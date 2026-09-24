import { Injectable, inject, signal } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Api } from './http';
import { Identity } from '../shared/models/identity';
/** Session bootstrap and the identity signal; the server session stays authoritative and no credential is stored here. */
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
