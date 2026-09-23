import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { Auth, errorMessage } from './api';
@Component({ imports: [FormField], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <form class="card narrow" (submit)="login($event)"><p class="eyebrow">DÀNH CHO NGƯỜI DẪN</p><h1>Host studio</h1>
    <label>Email<input [formField]="fields.username" type="email" autocomplete="username"></label>
    <label>Mật khẩu<input [formField]="fields.password" type="password" autocomplete="current-password"></label>
    <button class="primary" [disabled]="busy() || fields().invalid()">{{busy() ? 'Đang đăng nhập…' : 'Đăng nhập'}}</button>
    @if(error()) { <p class="alert" role="alert">{{error()}}</p> }
    <p class="muted">Tài khoản demo mặc định: host@example.test / local-quiz-only</p></form>` })
export class Login {
  private readonly auth = inject(Auth); private readonly router = inject(Router);
  readonly model = signal({username: '', password: ''}); readonly fields = form(this.model, p => { required(p.username); required(p.password); });
  readonly busy = signal(false); readonly error = signal('');
  async login(event: Event) {
    event.preventDefault(); if (this.busy() || this.fields().invalid()) return;
    this.busy.set(true); this.error.set('');
    try { const {username,password}=this.model(); await this.auth.login(username,password); this.model.update(v=>({...v,password:''})); await this.router.navigateByUrl('/host'); }
    catch(e) { this.error.set(errorMessage(e)); } finally { this.model.update(v=>({...v,password:''})); this.busy.set(false); }
  }
}
