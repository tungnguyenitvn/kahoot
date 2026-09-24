// Answer pending/retry policy (docs/features/live-room.md ANSWER-04, ANSWER-07). Framework-free: the store
// passes plain values in and applies the result; no Angular, no DOM, no HttpErrorResponse here.

/** The pending answer to send: the one already pending (retry keeps option, round and commandId), or a new one for the current question. */
export function nextPending(pending, question, option, newCommandId) {
  if (pending) return pending;
  if (!question || option === null || option === undefined) return null;
  return { roundId: question.roundId, option, commandId: newCommandId() };
}

/** Whether a failed send keeps the pending answer for a retry. status is the HTTP status, 0 for a network failure, null when the error carried none. */
export function keepsPending(status) {
  if (status === null || status === undefined || status === 0) return true;
  if (status >= 500) return true;
  if (status === 408 || status === 429) return true;
  return status < 400;
}

/** The pending answer after a snapshot: dropped when the round moved on or the snapshot already shows this option accepted. */
export function reconcilePending(pending, snapshot) {
  if (!pending) return null;
  if (snapshot.question?.roundId !== pending.roundId) return null;
  if (snapshot.me?.answer?.option === pending.option) return null;
  return pending;
}
