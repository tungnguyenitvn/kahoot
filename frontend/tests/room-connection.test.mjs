import test from 'node:test';
import assert from 'node:assert/strict';
import { RoomConnection } from '../src/app/room-connection.mjs';

const state = (version = 1) => ({id:'room-a',pin:'123456',title:'Quiz',phase:'LOBBY',version,
  serverTime:1000,deadline:0,questionNumber:0,questionCount:1,question:null,players:{},
  me:{role:'PLAYER',participantId:'p',answer:null}});
const deferred = () => { let resolve; const promise=new Promise(r=>resolve=r); return {promise,resolve}; };
function fixture(loadSnapshot = async()=>state()) {
  const intervals=new Map(), timeouts=new Map(), sockets=[], snapshots=[], errors=[], revoked=[];
  let serial=0;
  const options={roomId:'room-a',loadSnapshot,onSnapshot:s=>snapshots.push(s),onConnected:()=>{},
    onError:e=>errors.push(e),onRevoked:e=>revoked.push(e),isTerminal:e=>[401,403,404].includes(e.status),
    setInterval:(fn,ms)=>{const id=++serial;intervals.set(id,{fn,ms});return id;},clearInterval:id=>intervals.delete(id),
    setTimeout:(fn,ms)=>{const id=++serial;timeouts.set(id,{fn,ms});return id;},clearTimeout:id=>timeouts.delete(id),random:()=>0.5,
    openSocket:()=>{const s={sent:[],close(){this.closed=true;this.onclose?.();},send(data){this.sent.push(data);}};sockets.push(s);return s;}};
  const connection=new RoomConnection(options);
  return {connection,intervals,timeouts,sockets,snapshots,errors,revoked};
}
test('initial transient HTTP failure keeps polling and starts WS after recovery',async()=>{
  let calls=0;const f=fixture(async()=>{if(++calls===1)throw new Error('offline');return state();});
  await f.connection.start();assert.equal(f.sockets.length,0);assert.equal(f.intervals.size,1);
  await [...f.intervals.values()][0].fn();assert.equal(f.sockets.length,1);assert.equal(f.snapshots.length,1);
  f.connection.stop();
});
test('ANSWER-06 socket open sends only SYNC, close schedules one bounded backoff, REST cannot bypass it',async()=>{
  const f=fixture();await f.connection.start();const s=f.sockets[0];s.onopen();
  assert.deepEqual(JSON.parse(s.sent[0]),{type:'SYNC'});s.onclose();
  assert.equal(f.timeouts.size,1);assert.equal([...f.timeouts.values()][0].ms,500);
  await f.connection.refresh();assert.equal(f.sockets.length,1);
  const [id,job]=[...f.timeouts][0];f.timeouts.delete(id);job.fn();
  assert.equal(f.sockets.length,2);f.sockets[1].onclose();
  assert.equal([...f.timeouts.values()][0].ms,1000);f.connection.stop();
});
test('terminal access failure cancels scheduled reconnect and periodic reconciliation',async()=>{
  let denied=false;const f=fixture(async()=>{if(denied)throw {status:403};return state();});
  await f.connection.start();f.sockets[0].onclose();denied=true;await f.connection.refresh();
  assert.equal(f.timeouts.size,0);assert.equal(f.intervals.size,0);assert.equal(f.revoked.length,1);
});
test('ANSWER-06 REVOKED rejects late HTTP result and queued socket-open callbacks; nothing is resent',async()=>{
  const pending=deferred();let calls=0;const f=fixture(()=>++calls===1?Promise.resolve(state()):pending.promise);
  await f.connection.start();const socket=f.sockets[0], lateOpen=socket.onopen;
  const refresh=f.connection.refresh();socket.onmessage({data:JSON.stringify({type:'REVOKED',code:'SESSION_EXPIRED'})});
  pending.resolve(state(2));await refresh;lateOpen();
  assert.equal(f.snapshots.length,1);assert.equal(f.timeouts.size,0);assert.equal(socket.sent.length,0);
});
test('dispose during initial load never opens a socket or publishes a late snapshot',async()=>{
  const pending=deferred();const f=fixture(()=>pending.promise);const start=f.connection.start();
  f.connection.stop();pending.resolve(state());await start;
  assert.equal(f.sockets.length,0);assert.equal(f.snapshots.length,0);assert.equal(f.intervals.size,0);
});
test('malformed frame reconciles and never publishes unvalidated data',async()=>{
  const f=fixture();await f.connection.start();f.sockets[0].onmessage({data:'{"type":"STATE","room":{"version":3}}'});
  await f.connection.refresh();assert.ok(f.errors.length);assert.ok(f.snapshots.every(s=>s.id==='room-a'));
  f.connection.stop();
});
test('refresh requests are single-flight',async()=>{
  const pending=deferred();let calls=0;const f=fixture(()=>{calls++;return pending.promise;});
  const start=f.connection.start();const second=f.connection.refresh();assert.equal(calls,1);
  pending.resolve(state());await Promise.all([start,second]);assert.equal(f.sockets.length,1);f.connection.stop();
});
test('synchronous socket creation failure uses retry without stopping REST reconciliation',async()=>{
  const f=fixture();const open=f.connection.io.openSocket;let calls=0;
  f.connection.io.openSocket=()=>{if(++calls===1)throw new Error('socket unavailable');return open();};
  await f.connection.start();assert.equal(f.intervals.size,1);assert.equal(f.timeouts.size,1);
  const [id,job]=[...f.timeouts][0];f.timeouts.delete(id);job.fn();assert.equal(f.sockets.length,1);
  f.connection.stop();
});
test('ROOM-07 capacity refusal (close 1012) schedules no retry timer; the next reconciliation reopens the socket',async()=>{
  const f=fixture();await f.connection.start();const s=f.sockets[0];s.onopen();
  s.onclose({code:1012});
  assert.equal(f.timeouts.size,0);assert.equal(f.intervals.size,1);assert.equal(f.sockets.length,1);
  await [...f.intervals.values()][0].fn();
  assert.equal(f.sockets.length,2);f.connection.stop();
});
test('LIVE-05 default timers call the globals without an object receiver, as browsers require',async()=>{
  // Browsers throw "Illegal invocation" when window.setInterval runs with another object as `this`; emulate that.
  const original={setInterval,clearInterval,setTimeout,clearTimeout};const calls=[];
  const guard=name=>function(...args){if(this!==undefined&&this!==globalThis)throw new TypeError('Illegal invocation');calls.push(name);return original[name].call(globalThis,...args);};
  for(const name of Object.keys(original))globalThis[name]=guard(name);
  try{
    const connection=new RoomConnection({roomId:'room-a',loadSnapshot:async()=>state(),openSocket:()=>({send(){},close(){}}),
      onSnapshot:()=>{},onConnected:()=>{},onError:e=>{throw e;},onRevoked:()=>{},isTerminal:()=>false});
    await connection.start();connection.stop();
    assert.deepEqual(calls,['setInterval','clearInterval','clearTimeout']);
  }finally{Object.assign(globalThis,original);}
});
test('backoff grows to its cap and never creates duplicate retry timers',async()=>{
  const f=fixture();await f.connection.start();
  for(let i=0;i<12;i++) {
    f.sockets.at(-1).onclose();assert.equal(f.timeouts.size,1);
    const [id,job]=[...f.timeouts][0];assert.ok(job.ms<=10000);f.timeouts.delete(id);job.fn();
  }
  assert.equal(f.connection.delay,10000);f.connection.stop();assert.equal(f.timeouts.size,0);
});
