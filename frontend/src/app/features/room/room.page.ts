import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RoomStore } from './room.store';
@Component({ imports:[RouterLink], providers:[RoomStore], changeDetection:ChangeDetectionStrategy.OnPush, template:`
  @if(s.error()){<p class="alert" role="alert">{{s.error()}} <button class="ghost" (click)="s.refresh()">Đồng bộ lại</button></p>}
  @if(s.room(); as room){
    <div class="heading"><div><p class="eyebrow">{{room.title}}</p><h1>PIN <span class="pin">{{room.pin}}</span></h1></div><span class="badge">{{s.connected()?'● Trực tiếp':'○ Đang nối lại'}}</span></div>
    <div class="room-grid"><section>
    @switch(room.phase){
      @case('LOBBY'){<article class="card stage"><p class="eyebrow">SẢNH CHỜ</p><h2>Mời mọi người vào chơi</h2><p>{{s.activePlayers().length}} người đang tham gia · {{room.questionCount}} câu hỏi</p>
        @if(s.host()){<button class="primary" [disabled]="s.busy() || !s.activePlayers().length" (click)="s.control('start')">Bắt đầu →</button>}@else{<p class="muted">Chờ người dẫn bắt đầu…</p>}</article>}
      @case('FINISHED'){<article class="card stage"><p class="eyebrow">HOÀN THÀNH</p><h2>Cảm ơn bạn đã chơi!</h2><p>Kết quả cuối cùng ở bảng xếp hạng.</p><a routerLink="/">Vào phòng khác →</a></article>}
      @default{
        @if(room.question; as q){<article class="card stage"><div class="heading"><span class="badge">Câu {{room.questionNumber}} / {{room.questionCount}}</span><strong class="timer">{{room.phase==='QUESTION'?s.remaining()+'s':'Đáp án'}}</strong></div>
          <h2>{{q.text}}</h2><div class="answer-grid">
          @for(option of q.options; track $index){<button class="answer" [class.selected]="s.selection()===$index || room.me.answer?.option===$index" [class.correct]="q.correctOption===$index"
            [disabled]="s.host() || room.phase!=='QUESTION' || !!room.me.answer || !!s.pendingAnswer() || s.remaining()===0" (click)="s.selection.set($index)"><span>{{letters[$index]}}</span>{{option}}</button>}
          </div>
          @if(s.host()){
            @if(room.phase==='QUESTION'){<button [disabled]="s.busy()" (click)="s.control('reveal')">Chốt câu & công bố</button>}
            @else{<button class="primary" [disabled]="s.busy()" (click)="s.control('next')">{{room.questionNumber===room.questionCount?'Kết thúc':'Câu tiếp theo →'}}</button>}
          }@else{
            @if(room.me.answer){<p class="success">Đã ghi nhận đáp án {{letters[room.me.answer.option]}}.</p>}
            @else if(room.phase==='QUESTION' && !s.pendingAnswer()){<button class="primary" [disabled]="s.busy() || s.selection()===null || s.remaining()===0" (click)="s.answer()">Gửi đáp án</button>}
          }
        </article>}
      }
    }
    @if(s.pendingAnswer()){<article class="card"><p>Chưa xác nhận được lần nộp trước. Thử lại sẽ gửi đúng đáp án và mã yêu cầu cũ.</p><button [disabled]="s.busy()" (click)="s.answer()">Thử lại lần nộp</button></article>}
    <article class="card"><h3>Người chơi</h3><div class="player-list">@for(p of s.players();track p.id){<div><span>{{p.name}} {{p.active?(p.answered?'✓':''):'(đã rời)'}}</span>@if(s.host() && p.active && room.phase!=='FINISHED'){<button class="ghost" [disabled]="s.busy()" (click)="s.control('kick',p.id)">Mời ra</button>}</div>}</div></article>
    </section><aside class="card leaderboard"><p class="eyebrow">TOP 10</p><h2>Bảng xếp hạng</h2><p class="muted">Điểm được công bố khi chốt câu. Cùng điểm, cùng hạng.</p>
      @for(p of s.ranking().slice(0,10);track p.id){<div class="rank"><span class="number">{{p.rank}}</span><strong>{{p.name}}</strong><span>{{p.score}}</span></div>}
    </aside></div>
  }@else{<article class="card"><p>{{s.accessDenied()?'Không còn quyền truy cập phòng.':'Đang tải phòng…'}}</p><a routerLink="/">Về trang tham gia</a></article>}` })
export class RoomPage {
  readonly s=inject(RoomStore);readonly letters=['A','B','C','D'];
  constructor(){inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params=>this.s.open(params.get('id')!));}
}
