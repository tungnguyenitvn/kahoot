import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { Auth, errorMessage } from './api';
@Component({ selector: 'app-root', imports: [RouterOutlet, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<header class="nav"><a routerLink="/" class="brand">QUIZ<span>ROOM</span><small>LIVE PLAY / v0.2</small></a>
    <nav><a routerLink="/">Tham gia</a><a routerLink="/host">Host studio</a>
      @if (auth.user()?.host) { <button class="ghost" (click)="logout()">Đăng xuất</button> }
      @else { <a routerLink="/login">Đăng nhập</a> }</nav></header>
    @if (error()) { <p class="alert" role="alert">{{error()}}</p> }
    <main><router-outlet /></main><footer>Điểm theo thứ tự trả lời đúng · Server quyết định thời hạn</footer>` })
export class App {
  readonly auth = inject(Auth); private readonly router = inject(Router); readonly error = signal('');
  constructor() { void this.auth.ready().catch(e => this.error.set(errorMessage(e))); }
  async logout() { try { await this.auth.logout(); await this.router.navigateByUrl('/'); } catch(e) { this.error.set(errorMessage(e)); } }
}
