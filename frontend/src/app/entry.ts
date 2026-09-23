import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { form, FormField, required, pattern, maxLength } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { Api, Auth, errorMessage } from './api';
import { Room } from './models';
@Component({ imports: [FormField], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <section class="hero"><div><p class="eyebrow">MỘT CÂU HỎI. CẢ PHÒNG CÙNG CHƠI.</p>
    <h1>Nghĩ nhanh.<br><em>Chơi hết mình.</em></h1><p>Trả lời đúng để ghi điểm. Ai đúng trước sẽ nhận bậc điểm cao hơn.</p>
    <div class="tiles" aria-hidden="true"><span>▲</span><span>◆</span><span>●</span><span>■</span></div></div>
    <form class="card join" (submit)="join($event)"><p class="eyebrow">BẮT ĐẦU TẠI ĐÂY</p><h2>Vào phòng chơi</h2>
      <label>PIN phòng<input [formField]="fields.pin" inputmode="numeric" autocomplete="off" placeholder="6 chữ số"></label>
      <label>Tên hiển thị<input [formField]="fields.name" autocomplete="nickname" placeholder="Bạn muốn được gọi là gì?"></label>
      <button class="primary" [disabled]="busy() || fields().invalid()">{{busy() ? 'Đang tham gia…' : 'Tham gia →'}}</button>
      @if(error()) { <p class="alert" role="alert">{{error()}}</p> }
      <p class="muted">Không cần tài khoản. Giữ trình duyệt này để quay lại phòng.</p>
    </form></section>` })
export class Entry {
  private readonly api = inject(Api); private readonly auth = inject(Auth); private readonly router = inject(Router);
  readonly model = signal({pin: '', name: ''});
  readonly fields = form(this.model, p => { required(p.pin); pattern(p.pin, /^\d{6}$/); required(p.name); maxLength(p.name, 24); });
  readonly busy = signal(false); readonly error = signal('');
  async join(event: Event) {
    event.preventDefault(); if (this.busy() || this.fields().invalid()) return;
    this.busy.set(true); this.error.set('');
    try { await this.auth.ready(); const room = await this.api.post<Room>('/api/rooms/join', this.model()); await this.router.navigate(['/room',room.id]); }
    catch(e) { this.error.set(errorMessage(e)); } finally { this.busy.set(false); }
  }
}
