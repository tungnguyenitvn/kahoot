/** Wire shapes of the catalog endpoints (docs/contracts/rest-api.md, Catalog). */
export interface Question { text: string; options: string[]; correctOption: number; seconds: number }
export interface Quiz { id: string; title: string; status: 'DRAFT' | 'PUBLISHED'; questions: Question[] }
