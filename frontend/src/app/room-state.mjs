// Pure policies shared by the signal store and Node's test runner.
export function shouldApply(current, incoming) {
  return !current || (current.id === incoming.id && incoming.version >= current.version);
}
export function isRoomSnapshot(value, roomId) {
  const object = v => v !== null && typeof v === 'object' && !Array.isArray(v);
  const integer = n => Number.isSafeInteger(n) && n >= 0;
  if (!object(value) || value.id !== roomId || !integer(value.version) ||
      !['LOBBY','QUESTION','REVEAL','FINISHED'].includes(value.phase) ||
      !integer(value.serverTime) || !integer(value.deadline) ||
      typeof value.title !== 'string' || typeof value.pin !== 'string' ||
      !integer(value.questionNumber) || !integer(value.questionCount) || value.questionCount < 1 ||
      value.questionNumber > value.questionCount ||
      !object(value.players) || !object(value.me) || !['HOST','PLAYER'].includes(value.me.role)) return false;
  if (value.me.participantId !== null && typeof value.me.participantId !== 'string') return false;
  if (!Object.values(value.players).every(p => object(p) && typeof p.id === 'string' &&
      typeof p.name === 'string' && integer(p.score) && typeof p.active === 'boolean' && typeof p.answered === 'boolean')) return false;
  const q = value.question;
  if (q !== null && (!object(q) || typeof q.roundId !== 'string' || typeof q.text !== 'string' ||
      !Array.isArray(q.options) || q.options.length !== 4 || !q.options.every(o => typeof o === 'string') ||
      !integer(q.seconds) || q.seconds < 5 || q.seconds > 120 ||
      (q.correctOption !== null && (!integer(q.correctOption) || q.correctOption > 3)))) return false;
  if (['QUESTION','REVEAL'].includes(value.phase) && q === null) return false;
  if (value.phase === 'QUESTION' && q.correctOption !== null) return false;
  const answer = value.me.answer;
  return answer === null || (object(answer) && typeof answer.answerId === 'string' &&
      integer(answer.option) && answer.option < 4 && integer(answer.acceptedAt));
}
export function leaderboard(players) {
  const sorted = Object.values(players).sort((a,b) => b.score-a.score || a.id.localeCompare(b.id));
  let rank=0;
  return sorted.map((p,i) => { if(i===0 || p.score !== sorted[i-1].score) rank=i+1; return {...p,rank}; });
}
