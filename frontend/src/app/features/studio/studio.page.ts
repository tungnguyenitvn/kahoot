import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { form, FormField, required, min, max } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/http';
import { errorMessage } from '../../core/errors';
import { Quiz, Question } from '../../shared/models/catalog';
import { Room } from '../../shared/models/room';
import { HistoryRoom } from '../../shared/models/history';
@Component({ imports: [FormField, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <div class="heading"><div><p class="eyebrow">HOST STUDIO</p><h1>Sẵn sàng lên sóng?</h1></div><button class="ghost" (click)="quizzes.reload(); history.reload()">Làm mới</button></div>
  @if(error()) { <p class="alert" role="alert">{{error()}}</p> }
  @if(quizzes.error() || history.error()) { <p class="alert">Không tải được studio. Hãy làm mới hoặc đăng nhập lại.</p> }
  <div class="studio-grid"><section><h2>Bộ câu hỏi</h2>
    @if(quizzes.isLoading()) { <p>Đang tải…</p> }
    @for(quiz of quizzes.value() ?? []; track quiz.id) {
      <article class="card quiz"><span class="badge">{{quiz.status}}</span><h3>{{quiz.title}}</h3><p>{{quiz.questions.length}} câu hỏi</p>
      @if(quiz.status === 'DRAFT') { <button [disabled]="busy()" (click)="publish(quiz.id)">Xuất bản</button> }
      @else { <button class="primary" [disabled]="busy()" (click)="createRoom(quiz.id)">Mở phòng →</button> }</article>
    }
    <h2>Phòng gần đây</h2>
    @for(item of history.value() ?? []; track item.id) { <article class="card history"><div><strong>{{item.title}}</strong><p>{{item.phase}}</p></div><a [routerLink]="['/room',item.id]">Vào phòng</a>
      @if(item.phase === 'FINISHED') { <button class="ghost" (click)="loadResult(item.id)">Kết quả đã lưu</button> }</article> }
    @if(result().length) { <article class="card"><h3>Kết quả lưu trữ</h3>@for(p of result(); track p.id) { <p>{{p.name}} <strong>{{p.score}} điểm</strong></p> }</article> }
  </section><section class="card"><p class="eyebrow">TẠO QUIZ</p><h2>Bộ câu hỏi mới</h2>
    <form (submit)="save($event)"><label>Tên bộ câu hỏi<input [formField]="fields.title"></label>
    <label>Câu hỏi<input [formField]="fields.text"></label>
    <div class="options-editor"><label>A<input [formField]="fields.a"></label><label>B<input [formField]="fields.b"></label><label>C<input [formField]="fields.c"></label><label>D<input [formField]="fields.d"></label></div>
    <label>Đáp án đúng<select [formField]="fields.correct"><option value="0">A</option><option value="1">B</option><option value="2">C</option><option value="3">D</option></select></label>
    <label>Thời gian (giây)<input type="number" [formField]="fields.seconds"></label>
    <button type="button" [disabled]="busy() || fields().invalid() || questions().length >= 20" (click)="addQuestion()">Thêm câu hỏi</button>
    <ol>@for(q of questions(); track $index) { <li>{{q.text}} <button type="button" class="ghost" (click)="removeQuestion($index)">Bỏ</button></li> }</ol>
    <button class="primary" [disabled]="busy() || !questions().length || !model().title.trim()">Lưu bản nháp ({{questions().length}} câu)</button>
    <p class="muted">Thêm câu hỏi vào danh sách trước khi lưu. Bộ câu hỏi đã xuất bản được giữ nguyên để bảo toàn kết quả.</p></form>
  </section></div>` })
export class Studio {
  private readonly api = inject(Api); private readonly router = inject(Router);
  readonly quizzes = httpResource<Quiz[]>(() => '/api/quizzes'); readonly history = httpResource<HistoryRoom[]>(() => '/api/history');
  readonly model = signal({title: '',text: '',a: '',b: '',c: '',d: '',correct:'0',seconds:15});
  readonly fields = form(this.model,p=>{ required(p.title);required(p.text);required(p.a);required(p.b);required(p.c);required(p.d);min(p.seconds,5);max(p.seconds,120); });
  readonly questions = signal<Question[]>([]); readonly busy = signal(false); readonly error = signal(''); readonly result = signal<{id:string;name:string;score:number}[]>([]);
  private createCommands = new Map<string,string>();
  addQuestion() { if(this.fields().invalid()) return; const m=this.model(); this.questions.update(q=>[...q,{text:m.text,options:[m.a,m.b,m.c,m.d],correctOption:Number(m.correct),seconds:m.seconds}]); this.model.update(v=>({...v,text:'',a:'',b:'',c:'',d:''})); }
  removeQuestion(index:number) { this.questions.update(q=>q.filter((_,i)=>i!==index)); }
  private async run(work:()=>Promise<void>) { if(this.busy())return; this.busy.set(true);this.error.set('');try{await work();}catch(e){this.error.set(errorMessage(e));}finally{this.busy.set(false);} }
  async save(event:Event) { event.preventDefault(); if(!this.questions().length||!this.model().title.trim())return; await this.run(async()=>{await this.api.post('/api/quizzes',{title:this.model().title,questions:this.questions()});this.questions.set([]);this.quizzes.reload();}); }
  publish(id:string) { return this.run(async()=>{await this.api.post(`/api/quizzes/${id}/publish`,{});this.quizzes.reload();}); }
  createRoom(id:string) { return this.run(async()=>{const commandId=this.createCommands.get(id)??crypto.randomUUID();this.createCommands.set(id,commandId);const room=await this.api.post<Room>('/api/rooms',{quizId:id,commandId});this.createCommands.delete(id);await this.router.navigate(['/room',room.id]);}); }
  loadResult(id:string) { return this.run(async()=>{const value=await this.api.get<{id:string;name:string;score:number}[]>(`/api/history/${id}`);this.result.set(value);}); }
}
