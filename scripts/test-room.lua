-- Offline logic smoke test: executes the production Lua file with in-memory Redis/cjson doubles.
-- Does NOT verify Redis concurrency, durability, serialization or the Java client. Run ./scripts/verify for those integration tests.
local root=arg[1] or '.'
local script=assert(loadfile(root..'/backend/src/main/resources/redis/room.lua'))
local encoded,serial={},0
local null={}
local function clone(v)
  if v==null then return null end
  if type(v)~='table' then return v end
  local c={};for k,x in pairs(v)do c[k]=clone(x)end;return c
end
cjson={null=null,encode=function(v)serial=serial+1;local k='json:'..serial;encoded[k]=clone(v);return k end,
 decode=function(k)assert(encoded[k],'bad JSON handle');return clone(encoded[k])end}
local data,types,clock={},{},1000000
redis={call=function(cmd,k,...)
 local a={...};cmd=string.upper(cmd)
 if cmd=='TYPE' then return {ok=types[k] or 'none'} end
 if cmd=='TIME' then return {tostring(math.floor(clock/1000)),tostring(clock%1000*1000)}end
 if cmd=='GET' then return data[k] or false end
 if cmd=='SET' then data[k]=a[1];types[k]='string';return 'OK' end
 if cmd=='HGET' then return data[k] and data[k][a[1]] or false end
 if cmd=='HSET' then types[k]='hash';data[k]=data[k] or {};data[k][a[1]]=a[2];return 1 end
 if cmd=='HLEN' then local n=0;for _ in pairs(data[k] or {})do n=n+1 end;return n end
 if cmd=='HVALS' then local v={};for _,x in pairs(data[k] or {})do v[#v+1]=x end;return v end
 if cmd=='HEXISTS' then return data[k] and data[k][a[1]] and 1 or 0 end
 if cmd=='ZADD' then types[k]='zset';data[k]=data[k] or {};if not data[k][a[3]]then data[k][a[3]]=a[2];return 1 end;return 0 end
 if cmd=='ZINCRBY' then types[k]='zset';data[k]=data[k] or {};data[k][a[2]]=(data[k][a[2]] or 0)+a[1];return tostring(data[k][a[2]])end
 if cmd=='ZSCORE' then return data[k] and data[k][a[1]] and tostring(data[k][a[1]]) or false end
 if cmd=='ZUNIONSTORE' then data[k]=clone(data[a[2]] or {});types[k]='zset';return 1 end
 if cmd=='XADD' then types[k]='stream';data[k]=data[k] or {};data[k][#data[k]+1]=a[3];return #data[k]..'-0' end
 error('Unsupported fake command '..cmd)
end}
KEYS={'meta','members','names','answers','scores','visible','commands','events'}
local function call(op,who,input)ARGV={op,who,cjson.encode(input or {})};return cjson.decode(script())end
local function ok(op,who,input)local v=call(op,who,input);assert(v.ok,v.code);return v.result end
local function bad(code,op,who,input)local v=call(op,who,input);assert(not v.ok and v.code==code,'Expected '..code..', got '..tostring(v.code))end
local function question(id)return {roundId=id,text='Question',options={'a','b','c','d'},correctOption=0,seconds=15}end
local function init()data={};types={};clock=1000000;ok('init','host',{id='r',pin='123456',quizId='q',owner='host',title='Room',questions={question('one'),question('two')}})end
local function join(p,name)return ok('join',p,{participantId=p,name=name or p,normalizedName=string.lower(name or p)})end
local function answer(p,round,option)return {roundId=round,option=option,commandId='cmd-'..p}end
local function start()ok('start','host',{commandId='start'})end
local passed=0
local function test(name,fn)init();fn();passed=passed+1;print('PASS '..name)end

test('LIVE-03 retries preserve receipt, score, sequence and event count',function()
 join('p');start();local receipt=ok('answer','p',answer('p','one',0));local n=#data.events
 for i=1,100 do local retry=ok('answer','p',answer('p','one',0));assert(retry.answerId==receipt.answerId and retry.acceptedAt==receipt.acceptedAt)end
 assert(#data.events==n and data.scores.p==1000 and cjson.decode(data.meta).correctCount==1)
 bad('ALREADY_ANSWERED','answer','p',answer('p','one',1))
end)
test('LIVE-03 wrong answers do not consume a correct rank; tiers reach the floor',function()
 for i=1,8 do join('p'..i)end;start();ok('answer','p1',answer('p1','one',1))
 for i=2,8 do ok('answer','p'..i,answer('p'..i,'one',0));assert(data.scores['p'..i]==math.max(500,1200-100*i))end
 assert(data.scores.p1==0 and cjson.decode(data.meta).correctCount==7)
end)
test('LIVE-04 answer receipt and live snapshot hide result until reveal',function()
 join('p');start();local receipt=ok('answer','p',answer('p','one',0));assert(receipt.points==nil and receipt.correctOrder==nil)
 local s=ok('snapshot','p');assert(s.question.correctOption==null and s.players.p.score==0)
 ok('reveal','host',{roundId='one',commandId='reveal'});s=ok('snapshot','p');assert(s.question.correctOption==0 and s.players.p.score==1000)
end)
test('ROOM-03 LIVE-03 retry of previous round cannot score or advance the new round',function()
 join('p');start();local receipt=ok('answer','p',answer('p','one',0));ok('reveal','host',{roundId='one',commandId='r'})
 ok('next','host',{roundId='one',commandId='n'});ok('next','host',{roundId='one',commandId='n'})
 assert(ok('answer','p',answer('p','one',0)).answerId==receipt.answerId and cjson.decode(data.meta).index==2)
 bad('STALE_ROUND','next','host',{roundId='one',commandId='new'})
 bad('COMMAND_CONFLICT','reveal','host',{roundId='two',commandId='n'})
end)
test('LIVE-02 deadline equality rejects even before the timer runs',function()
 join('p');start();clock=clock+15000;bad('DEADLINE_PASSED','answer','p',answer('p','one',0))
 ok('tick','system');assert(ok('snapshot','p').phase=='REVEAL')
end)
test('LIVE-01 JOIN-02 JOIN-04 membership, host authority, revoked access and name uniqueness',function()
 join('p','Alice');bad('NAME_TAKEN','join','other',{participantId='other',name='alice',normalizedName='alice'})
 bad('ROOM_ACCESS_DENIED','snapshot','outsider');bad('HOST_REQUIRED','start','p',{commandId='s'})
 ok('kick','host',{participantId='p',commandId='kick'});bad('MEMBERSHIP_REVOKED','join','p',{})
 bad('ROOM_ACCESS_DENIED','snapshot','p');bad('ROOM_ACCESS_DENIED','answer','p',answer('p','one',0))
 bad('PLAYERS_REQUIRED','start','host',{commandId='s'})
end)
test('LIVE-07 corrupt type fails before writes',function()
 join('p');start();local before=data.meta;types.scores='string';data.scores='bad'
 bad('STATE_CORRUPT','answer','p',answer('p','one',0));assert(data.meta==before and data.answers==nil)
end)
test('LIVE-02 room expiry finalizes once; terminal snapshots reveal scores',function()
 join('p');start();ok('answer','p',answer('p','one',0));clock=clock+7200000
 ok('tick','system');local n=#data.events;ok('tick','system');assert(#data.events==n)
 local s=ok('snapshot','p');assert(s.phase=='FINISHED' and s.players.p.score==1000)
end)
test('room capacity bounds work',function()
 for i=1,100 do join('p'..i)end;bad('ROOM_FULL','join','extra',{participantId='extra',name='Extra',normalizedName='extra'})
end)
test('host command ledger capacity is a 409 conflict, not a rate limit',function()
 join('p');for i=1,500 do ok('kick','host',{participantId='p',commandId='k'..i}) end
 local v=call('kick','host',{participantId='p',commandId='k501'})
 assert(not v.ok and v.code=='COMMAND_LIMIT' and v.status==409,'Expected 409 COMMAND_LIMIT, got '..tostring(v.status)..' '..tostring(v.code))
 assert(ok('kick','host',{participantId='p',commandId='k1'}).accepted,'Replay of a ledgered command must still be accepted when the ledger is full')
end)
print(passed..' offline Lua logic checks passed. Real Redis/HTTP integration is separate.')
