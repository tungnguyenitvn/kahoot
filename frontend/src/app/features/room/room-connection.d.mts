import type { Room } from '../../shared/models/room';
export interface RoomConnectionOptions {
  roomId: string;
  loadSnapshot: () => Promise<unknown>;
  openSocket: () => WebSocket;
  onSnapshot: (room: Room) => void;
  onConnected: (connected: boolean) => void;
  onError: (error: unknown) => void;
  onRevoked: (reason: unknown) => void;
  isTerminal: (error: unknown) => boolean;
}
export class RoomConnection {
  constructor(options: RoomConnectionOptions);
  start(): Promise<void>;
  refresh(): Promise<void>;
  stop(): void;
}
