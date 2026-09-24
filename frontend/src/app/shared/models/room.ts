/** Wire shapes of the room snapshot and receipt (docs/contracts/room-state.md); runtime JSON is validated by the room policy module before use. */
import { Question } from './catalog';
export interface Player { id: string; name: string; active: boolean; answered: boolean; score: number }
export interface Room {
  id: string; pin: string; title: string; phase: 'LOBBY' | 'QUESTION' | 'REVEAL' | 'FINISHED';
  version: number; serverTime: number; deadline: number; questionNumber: number; questionCount: number;
  question: (Omit<Question, 'correctOption'> & { roundId: string; correctOption: number | null }) | null;
  players: Record<string, Player>;
  me: { role: 'HOST' | 'PLAYER'; participantId: string | null; answer: Receipt | null };
}
export interface Receipt { answerId: string; roundId?: string; acceptedAt: number; option: number }
