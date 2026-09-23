import { isRoomSnapshot } from './room-state.mjs';

// Route-scoped transport. No mutation retries and no framework/browser dependency in tests.
export class RoomConnection {
  constructor(options) {
    // Browser timer functions must be called without an object receiver: `this.io.setInterval(...)` on a
    // bare `window.setInterval` reference throws "Illegal invocation" in browsers, so wrap the globals.
    const timers = {
      setInterval: (fn, ms) => setInterval(fn, ms),
      clearInterval: id => clearInterval(id),
      setTimeout: (fn, ms) => setTimeout(fn, ms),
      clearTimeout: id => clearTimeout(id),
      random: () => Math.random()
    };
    this.io = { ...timers, ...options };
    this.stopped = false;
    this.delay = 500;
  }
  start() {
    if (this.stopped) return Promise.resolve();
    this.poll ??= this.io.setInterval(() => this.refresh(), 5000);
    return this.refresh();
  }
  refresh() {
    if (this.stopped) return Promise.resolve();
    if (this.loading) return this.loading;
    this.loading = this.load().finally(() => { this.loading = undefined; });
    return this.loading;
  }
  async load() {
    try {
      const value = await this.io.loadSnapshot();
      if (this.stopped) return;
      this.apply(value);
      this.connect();
    } catch (error) {
      if (this.stopped) return;
      this.io.onError(error);
      if (this.io.isTerminal(error)) this.revoke(error);
    }
  }
  apply(value) {
    if (!isRoomSnapshot(value, this.io.roomId)) throw new Error('INVALID_SNAPSHOT');
    this.io.onSnapshot(value);
  }
  connect() {
    if (this.stopped || this.socket || this.retry !== undefined) return;
    let socket;
    try { socket = this.io.openSocket(); }
    catch (error) { this.io.onError(error); this.scheduleRetry(); return; }
    this.socket = socket;
    const current = () => !this.stopped && this.socket === socket;
    socket.onopen = () => {
      if (!current()) return;
      this.delay = 500;
      this.io.onConnected(true);
      socket.send(JSON.stringify({type:'SYNC'}));
    };
    socket.onmessage = event => {
      if (!current()) return;
      try {
        const message = JSON.parse(event.data);
        switch (message?.type) {
          case 'STATE': this.apply(message.room); break;
          case 'PONG': break;
          case 'REVOKED': this.revoke(message.code ?? 'ROOM_ACCESS_DENIED'); break;
          case 'ERROR': this.io.onError(new Error(message.code ?? 'REALTIME_UNAVAILABLE')); break;
          default: throw new Error('INVALID_MESSAGE');
        }
      } catch (error) { this.io.onError(error); void this.refresh(); }
    };
    socket.onerror = () => { if (current()) this.io.onConnected(false); };
    socket.onclose = event => {
      if (!current()) return;
      this.socket = undefined;
      this.io.onConnected(false);
      // 1012: the server refused this connection for capacity. Do not race it with a timer;
      // the next REST reconciliation calls connect() again (ROOM-07).
      if (event && event.code === 1012) return;
      this.scheduleRetry();
    };
  }
  scheduleRetry() {
    if (this.stopped || this.retry !== undefined) return;
    const delay = Math.min(10000, Math.round(this.delay * (0.8 + this.io.random() * 0.4)));
    this.delay = Math.min(10000, this.delay * 2);
    this.retry = this.io.setTimeout(() => { this.retry = undefined; this.connect(); }, delay);
  }
  revoke(reason) {
    this.stop();
    this.io.onRevoked(reason);
  }
  stop() {
    this.stopped = true;
    this.io.clearInterval(this.poll);
    this.io.clearTimeout(this.retry);
    this.poll = this.retry = undefined;
    const socket = this.socket;
    this.socket = undefined;
    if (socket) {
      socket.onopen = socket.onmessage = socket.onerror = socket.onclose = null;
      socket.close();
    }
    this.io.onConnected(false);
  }
}
