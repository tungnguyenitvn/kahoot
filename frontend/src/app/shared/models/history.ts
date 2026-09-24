/** Wire shape of GET /api/history (docs/contracts/rest-api.md, History). */
export interface HistoryRoom { id: string; title: string; phase: string; archivedVersion: number; createdAt: string; finishedAt: string | null }
