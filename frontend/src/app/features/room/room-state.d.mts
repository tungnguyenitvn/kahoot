import type { Player, Room } from '../../shared/models/room';
export function shouldApply(current: Room | null, incoming: Room): boolean;
export function isRoomSnapshot(value: unknown, roomId: string): value is Room;
export function leaderboard(players: Record<string, Player>): (Player & {rank:number})[];
