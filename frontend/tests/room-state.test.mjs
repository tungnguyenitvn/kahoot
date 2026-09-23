import test from 'node:test';
import assert from 'node:assert/strict';
import { leaderboard, shouldApply, isRoomSnapshot } from '../src/app/room-state.mjs';
test('late HTTP response cannot roll back a WebSocket snapshot',()=>{
 assert.equal(shouldApply({id:'r',version:8},{id:'r',version:7}),false);
 assert.equal(shouldApply({id:'r',version:8},{id:'r',version:8}),true);
 assert.equal(shouldApply({id:'r',version:8},{id:'other',version:9}),false);
});
test('ties share a competition rank and stable participant ordering',()=>{
 const rows=leaderboard({c:{id:'c',score:800},b:{id:'b',score:1000},a:{id:'a',score:1000}});
 assert.deepEqual(rows.map(x=>[x.id,x.rank]),[['a',1],['b',1],['c',3]]);
});
test('LIVE-04 snapshot validator accepts a full room and rejects malformed or private QUESTION data',()=>{
 const room={id:'r',pin:'123456',title:'Quiz',phase:'QUESTION',version:1,serverTime:1000,deadline:2000,
  questionNumber:1,questionCount:1,question:{roundId:'q',text:'?',options:['a','b','c','d'],seconds:5,correctOption:null},
  players:{p:{id:'p',name:'P',active:true,answered:false,score:0}},me:{role:'PLAYER',participantId:'p',answer:null}};
 assert.equal(isRoomSnapshot(room,'r'),true);
 assert.equal(isRoomSnapshot(room,'other'),false);
 assert.equal(isRoomSnapshot({...room,version:NaN},'r'),false);
 assert.equal(isRoomSnapshot({...room,question:{...room.question,correctOption:0}},'r'),false);
 assert.equal(isRoomSnapshot({...room,players:[]},'r'),false);
 assert.equal(isRoomSnapshot({...room,me:{role:'OWNER'}},'r'),false);
 assert.equal(isRoomSnapshot({...room,phase:'FINISHED',question:null},'r'),true);
});
