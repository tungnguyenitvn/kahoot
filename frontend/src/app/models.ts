export interface Identity { id: string; name: string; host: boolean }
export interface Question { text: string; options: string[]; correctOption: number; seconds: number }
export interface Quiz { id: string; title: string; status: 'DRAFT' | 'PUBLISHED'; questions: Question[] }
export interface Player { id: string; name: string; active: boolean; answered: boolean; score: number }
export interface Room {
  id: string; pin: string; title: string; phase: 'LOBBY' | 'QUESTION' | 'REVEAL' | 'FINISHED';
  version: number; serverTime: number; deadline: number; questionNumber: number; questionCount: number;
  question: (Omit<Question, 'correctOption'> & { roundId: string; correctOption: number | null }) | null;
  players: Record<string, Player>;
  me: { role: 'HOST' | 'PLAYER'; participantId: string | null; answer: Receipt | null };
}
export interface Receipt { answerId: string; roundId?: string; acceptedAt: number; option: number }
export interface HistoryRoom { id: string; title: string; phase: string; archivedVersion: number; createdAt: string; finishedAt: string | null }
