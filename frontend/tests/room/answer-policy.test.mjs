import test from 'node:test';
import assert from 'node:assert/strict';
import { nextPending, keepsPending, reconcilePending } from '../../src/app/features/room/answer-policy.mjs';

const question = { roundId: 'r1' };
const snapshot = (roundId, acceptedOption = null) => ({ question: roundId ? { roundId } : null, me: { answer: acceptedOption === null ? null : { option: acceptedOption } } });

test('ANSWER-04 a retry reuses the original option, round and commandId; a new answer gets a fresh commandId', () => {
  const first = nextPending(null, question, 2, () => 'cmd-1');
  assert.deepEqual(first, { roundId: 'r1', option: 2, commandId: 'cmd-1' });
  assert.equal(nextPending(first, question, 3, () => 'cmd-2'), first, 'the pending answer wins over a new selection');
  assert.equal(nextPending(null, question, null, () => 'cmd-3'), null, 'nothing to send without a selection');
  assert.equal(nextPending(null, null, 1, () => 'cmd-4'), null, 'nothing to send without a question');
});

test('ANSWER-04 a new round drops a stale pending answer; an accepted receipt in the snapshot clears it', () => {
  const pending = { roundId: 'r1', option: 2, commandId: 'cmd-1' };
  assert.equal(reconcilePending(pending, snapshot('r1')), pending, 'same round, not yet accepted: keep');
  assert.equal(reconcilePending(pending, snapshot('r2')), null, 'round moved on');
  assert.equal(reconcilePending(pending, snapshot(null)), null, 'no question any more');
  assert.equal(reconcilePending(pending, snapshot('r1', 2)), null, 'the server already holds this option');
  assert.equal(reconcilePending(pending, snapshot('r1', 1)), pending, 'a different accepted option is a conflict the server reports, not a silent clear');
  assert.equal(reconcilePending(null, snapshot('r1')), null);
});

test('ANSWER-07 network failures, 5xx, 408 and 429 keep the pending answer; validation and authority errors drop it', () => {
  for (const status of [null, 0, 500, 502, 503, 408, 429]) assert.equal(keepsPending(status), true, `status ${status} keeps`);
  for (const status of [400, 401, 403, 404, 409, 422]) assert.equal(keepsPending(status), false, `status ${status} drops`);
});
