import { Injectable, DestroyRef, computed, inject, linkedSignal, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Api, Auth, errorMessage } from './api';
import { Room, Receipt } from './models';
import { leaderboard, shouldApply, isRoomSnapshot } from './room-state.mjs';
import { RoomConnection } from './room-connection.mjs';

@Injectable()
export class RoomStore {
  private readonly api = inject(Api);
  private readonly auth = inject(Auth);
  readonly room = signal<Room | null>(null);
  private readonly commandError = signal('');
  private readonly transportError = signal('');
  readonly error = computed(() => this.commandError() || this.transportError());
  readonly connected = signal(false);
  readonly accessDenied = signal(false);
  readonly busy = signal(false);
  readonly pendingAnswer = signal<{roundId: string; option: number; commandId: string} | null>(null);
  readonly selection = linkedSignal({
    source: () => this.room()?.question?.roundId,
    computation: (): number | null => null
  });
  readonly now = signal(Date.now());
  readonly remaining = computed(() => Math.max(0, Math.ceil(((this.room()?.deadline ?? 0) - this.now()) / 1000)));
  readonly players = computed(() => Object.values(this.room()?.players ?? {}));
  readonly activePlayers = computed(() => this.players().filter(player => player.active));
  readonly ranking = computed(() => leaderboard(this.room()?.players ?? {}));
  readonly host = computed(() => this.room()?.me.role === 'HOST');

  private offset = 0;
  private id = '';
  private connection?: RoomConnection;
  private clock?: ReturnType<typeof setInterval>;
  private disposed = false;
  private readonly controls = new Map<string, string>();

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      this.disposed = true;
      this.connection?.stop();
      clearInterval(this.clock);
    });
  }

  open(id: string) {
    if (this.disposed || (id === this.id && this.connection)) return;
    this.connection?.stop();
    clearInterval(this.clock);
    this.id = id;
    this.room.set(null);
    this.pendingAnswer.set(null);
    this.controls.clear();
    this.busy.set(false);
    this.accessDenied.set(false);
    this.commandError.set('');
    this.transportError.set('');
    this.connection = new RoomConnection({
      roomId: id,
      loadSnapshot: async () => {
        await this.auth.ready();
        return this.api.get<Room>('/api/rooms/' + id);
      },
      openSocket: () => new WebSocket(
        (window.location.protocol === 'https:' ? 'wss:' : 'ws:') +
        '//' + window.location.host + '/ws/rooms/' + id
      ),
      onSnapshot: value => this.apply(value),
      onConnected: connected => this.connected.set(connected),
      onError: error => this.transportError.set(errorMessage(error)),
      onRevoked: () => {
        this.accessDenied.set(true);
        this.room.set(null);
        this.commandError.set('');
        this.transportError.set('Phiên hoặc quyền truy cập đã hết hiệu lực. Hãy quay lại trang tham gia.');
        clearInterval(this.clock);
      },
      isTerminal: error => error instanceof HttpErrorResponse && [401, 403, 404].includes(error.status)
    });
    this.clock = setInterval(() => this.now.set(Date.now() + this.offset), 100);
    void this.connection.start();
  }

  private apply(value: Room) {
    if (!isRoomSnapshot(value, this.id)) { this.transportError.set('INVALID_SNAPSHOT'); return; }
    if (this.disposed || value.id !== this.id || !shouldApply(this.room(), value)) return;
    this.offset = value.serverTime - Date.now();
    this.now.set(Date.now() + this.offset);
    this.room.set(value);
    this.transportError.set('');
    const pending = this.pendingAnswer();
    if (pending && (value.question?.roundId !== pending.roundId || value.me.answer?.option === pending.option)) {
      this.pendingAnswer.set(null);
    }
  }

  async refresh() { await this.connection?.refresh(); }

  async answer() {
    if (this.busy() || this.accessDenied() || this.disposed) return;
    const q = this.room()?.question;
    const option = this.selection();
    let pending = this.pendingAnswer();
    if (!pending) {
      if (!q || option === null) return;
      pending = {roundId: q.roundId, option, commandId: crypto.randomUUID()};
      this.pendingAnswer.set(pending);
    }
    const connection = this.connection;
    const id = this.id;
    this.busy.set(true);
    this.commandError.set('');
    try {
      await this.api.post<Receipt>('/api/rooms/' + id + '/answers', pending);
      if (this.disposed || connection !== this.connection) return;
      this.pendingAnswer.set(null);
      await this.refresh();
    } catch (error) {
      if (this.disposed || connection !== this.connection) return;
      this.commandError.set(errorMessage(error));
      if (error instanceof HttpErrorResponse && error.status >= 400 && error.status < 500 &&
          ![408, 429].includes(error.status)) this.pendingAnswer.set(null);
      await this.refresh();
    } finally {
      if (!this.disposed && connection === this.connection) this.busy.set(false);
    }
  }

  async control(action: 'start' | 'reveal' | 'next' | 'kick', participantId?: string) {
    if (this.busy() || this.accessDenied() || this.disposed) return;
    const connection = this.connection;
    const id = this.id;
    this.busy.set(true);
    this.commandError.set('');
    const roundId = this.room()?.question?.roundId;
    const key = action + ':' + (roundId ?? '') + ':' + (participantId ?? '');
    const commandId = this.controls.get(key) ?? crypto.randomUUID();
    this.controls.set(key, commandId);
    try {
      const room = await this.api.post<Room>('/api/rooms/' + id + '/' + action, {roundId, commandId, participantId});
      if (this.disposed || connection !== this.connection) return;
      this.apply(room);
      this.controls.delete(key);
    } catch (error) {
      if (this.disposed || connection !== this.connection) return;
      this.commandError.set(errorMessage(error));
      await this.refresh();
    } finally {
      if (!this.disposed && connection === this.connection) this.busy.set(false);
    }
  }
}
