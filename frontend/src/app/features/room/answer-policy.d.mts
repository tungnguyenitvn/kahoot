import type { Room } from '../../shared/models/room';
export interface PendingAnswer { roundId: string; option: number; commandId: string }
export function nextPending(pending: PendingAnswer | null, question: { roundId: string } | null | undefined, option: number | null, newCommandId: () => string): PendingAnswer | null;
export function keepsPending(status: number | null): boolean;
export function reconcilePending(pending: PendingAnswer | null, snapshot: Room): PendingAnswer | null;
