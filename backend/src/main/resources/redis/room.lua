-- KEYS: meta, members, names, answers, scores, visibleScores, commands, events.
-- ARGV: operation, authenticated principal ID, JSON payload. All keys share room hash tag.
-- One bounded room operation. Atomic execution is NOT rollback on runtime errors.
local op, principal = ARGV[1], ARGV[2]
local input = cjson.decode(ARGV[3])
local expected = {'string','hash','hash','hash','zset','zset','hash','stream'}
for i=1,8 do
  local t=redis.call('TYPE',KEYS[i]); if type(t)=='table' then t=t.ok end
  if t~='none' and t~=expected[i] then return cjson.encode({ok=false,status=503,code='STATE_CORRUPT'}) end
end
local function fail(status,code) return cjson.encode({ok=false,status=status,code=code}) end
local time=redis.call('TIME'); local now=tonumber(time[1])*1000+math.floor(tonumber(time[2])/1000)
local raw=redis.call('GET',KEYS[1])
local meta=raw and cjson.decode(raw) or nil
local function emit(kind,data)
  meta.version=meta.version+1
  redis.call('SET',KEYS[1],cjson.encode(meta))
  redis.call('XADD',KEYS[8],'*','event',cjson.encode({roomId=meta.id,version=meta.version,type=kind,at=now,data=data}))
end
local function success(data) return cjson.encode({ok=true,result=data}) end
if op=='init' then
  if meta then return success({roomId=meta.id}) end
  meta=input; meta.phase='LOBBY'; meta.index=0; meta.version=0; meta.correctCount=0
  meta.createdAt=now; meta.expiresAt=now+7200000; meta.deadline=0
  emit('CREATED',{quizId=meta.quizId,owner=meta.owner})
  return success({roomId=meta.id})
end
if not meta then return fail(404,'ROOM_NOT_FOUND') end
local memberRaw=redis.call('HGET',KEYS[2],principal)
local member=memberRaw and cjson.decode(memberRaw) or nil
local isHost=principal==meta.owner
local function canRead() return isHost or (member and member.active) end
local function reveal()
  meta.phase='REVEAL'; meta.deadline=0
  redis.call('ZUNIONSTORE',KEYS[6],1,KEYS[5])
  emit('REVEALED',{roundId=meta.questions[meta.index].roundId})
end
if op=='tick' then
  if meta.phase~='FINISHED' and now>=meta.expiresAt then
    meta.phase='FINISHED'; meta.deadline=0
    redis.call('ZUNIONSTORE',KEYS[6],1,KEYS[5]); emit('FINISHED',{reason='EXPIRED'})
  elseif meta.phase=='QUESTION' and now>=meta.deadline then reveal() end
  return success({phase=meta.phase})
end
if op=='join' then
  if member and member.active then return success({roomId=meta.id,participantId=member.id}) end
  if member then return fail(403,'MEMBERSHIP_REVOKED') end
  if isHost then return fail(409,'HOST_CANNOT_JOIN') end
  if meta.phase~='LOBBY' or now>=meta.expiresAt then return fail(409,'JOIN_CLOSED') end
  if redis.call('HLEN',KEYS[2])>=100 then return fail(409,'ROOM_FULL') end
  if redis.call('HGET',KEYS[3],input.normalizedName) then return fail(409,'NAME_TAKEN') end
  member={id=input.participantId,name=input.name,principal=principal,active=true}
  redis.call('HSET',KEYS[2],principal,cjson.encode(member))
  redis.call('HSET',KEYS[3],input.normalizedName,principal)
  redis.call('ZADD',KEYS[5],'NX',0,member.id); redis.call('ZADD',KEYS[6],'NX',0,member.id)
  emit('JOINED',member)
  return success({roomId=meta.id,participantId=member.id})
end
if op=='snapshot' then
  if not canRead() then return fail(403,'ROOM_ACCESS_DENIED') end
  local players={}
  for _,value in ipairs(redis.call('HVALS',KEYS[2])) do
    local p=cjson.decode(value)
    local answered=false
    if meta.index>0 then answered=redis.call('HEXISTS',KEYS[4],meta.questions[meta.index].roundId..':'..p.principal)==1 end
    players[p.id]={id=p.id,name=p.name,active=p.active,answered=answered,score=tonumber(redis.call('ZSCORE',KEYS[6],p.id) or '0')}
  end
  local question=cjson.null; local own=cjson.null
  if meta.index>0 then
    local q=meta.questions[meta.index]
    question={roundId=q.roundId,text=q.text,options=q.options,seconds=q.seconds,correctOption=cjson.null}
    if meta.phase=='REVEAL' or meta.phase=='FINISHED' then question.correctOption=q.correctOption end
    if member then
      local a=redis.call('HGET',KEYS[4],q.roundId..':'..principal)
      if a then local receipt=cjson.decode(a); own={option=receipt.option,answerId=receipt.answerId,acceptedAt=receipt.acceptedAt} end
    end
  end
  return success({id=meta.id,pin=meta.pin,title=meta.title,phase=meta.phase,version=meta.version,
    serverTime=now,deadline=meta.deadline,questionNumber=meta.index,questionCount=#meta.questions,
    question=question,players=players,me={role=isHost and 'HOST' or 'PLAYER',participantId=member and member.id or cjson.null,answer=own}})
end
if op=='answer' then
  if not member or not member.active then return fail(403,'ROOM_ACCESS_DENIED') end
  local answerKey=input.roundId..':'..principal
  local previous=redis.call('HGET',KEYS[4],answerKey)
  local function publicReceipt(a) return {answerId=a.answerId,roundId=a.roundId,acceptedAt=a.acceptedAt,option=a.option} end
  if previous then
    local old=cjson.decode(previous)
    if old.option~=input.option then return fail(409,'ALREADY_ANSWERED') end
    return success(publicReceipt(old))
  end
  if meta.phase~='QUESTION' then return fail(409,'ROUND_CLOSED') end
  local q=meta.questions[meta.index]
  if q.roundId~=input.roundId then return fail(409,'STALE_ROUND') end
  if now>=meta.deadline or now>=meta.expiresAt then return fail(409,'DEADLINE_PASSED') end
  if type(input.option)~='number' or input.option~=math.floor(input.option) or input.option<0 or input.option>=#q.options then return fail(400,'INVALID_OPTION') end
  local rank=0; local points=0
  if input.option==q.correctOption then meta.correctCount=meta.correctCount+1; rank=meta.correctCount; points=math.max(500,1100-100*rank) end
  local receipt={answerId=q.roundId..':'..member.id,roundId=q.roundId,participantId=member.id,option=input.option,
    acceptedAt=now,correctOrder=rank,points=points,commandId=input.commandId}
  redis.call('HSET',KEYS[4],answerKey,cjson.encode(receipt))
  redis.call('ZINCRBY',KEYS[5],points,member.id)
  emit('ANSWERED',receipt)
  return success(publicReceipt(receipt))
end
if not isHost then return fail(403,'HOST_REQUIRED') end
local commandKey=principal..':'..input.commandId
local fingerprint=op..':'..(input.roundId or '')..':'..(input.participantId or '')
local previous=redis.call('HGET',KEYS[7],commandKey)
if previous then
  if previous~=fingerprint then return fail(409,'COMMAND_CONFLICT') end
  return success({accepted=true})
end
if redis.call('HLEN',KEYS[7])>=500 then return fail(409,'COMMAND_LIMIT') end -- ledger capacity is a state conflict, not rate limiting
if op=='start' then
  if meta.phase~='LOBBY' or now>=meta.expiresAt then return fail(409,'INVALID_PHASE') end
  local count=0; for _,v in ipairs(redis.call('HVALS',KEYS[2])) do if cjson.decode(v).active then count=count+1 end end
  if count==0 then return fail(409,'PLAYERS_REQUIRED') end
  meta.index=1; meta.phase='QUESTION'; meta.deadline=now+meta.questions[1].seconds*1000; meta.correctCount=0
  emit('OPENED',{roundId=meta.questions[1].roundId,deadline=meta.deadline})
elseif op=='reveal' then
  if meta.phase~='QUESTION' or meta.questions[meta.index].roundId~=input.roundId then return fail(409,'STALE_ROUND') end
  reveal()
elseif op=='next' then
  if meta.phase~='REVEAL' or meta.questions[meta.index].roundId~=input.roundId then return fail(409,'STALE_ROUND') end
  if meta.index==#meta.questions then meta.phase='FINISHED'; emit('FINISHED',{reason='COMPLETED'})
  else meta.index=meta.index+1; meta.phase='QUESTION'; meta.correctCount=0; meta.deadline=now+meta.questions[meta.index].seconds*1000
    emit('OPENED',{roundId=meta.questions[meta.index].roundId,deadline=meta.deadline}) end
elseif op=='kick' then
  if meta.phase=='FINISHED' then return fail(409,'INVALID_PHASE') end
  local target=nil
  for _,v in ipairs(redis.call('HVALS',KEYS[2])) do local p=cjson.decode(v); if p.id==input.participantId then target=p; break end end
  if not target then return fail(404,'PLAYER_NOT_FOUND') end
  target.active=false; redis.call('HSET',KEYS[2],target.principal,cjson.encode(target)); emit('KICKED',target)
else return fail(400,'UNKNOWN_COMMAND') end
redis.call('HSET',KEYS[7],commandKey,fingerprint)
return success({accepted=true})
