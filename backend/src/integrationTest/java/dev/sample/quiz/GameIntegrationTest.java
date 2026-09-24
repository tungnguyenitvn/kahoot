package dev.sample.quiz;

import dev.sample.quiz.archive.application.Projection;
import dev.sample.quiz.archive.domain.RoomEvent;
import dev.sample.quiz.gameplay.infrastructure.LuaRooms;
import dev.sample.quiz.shared.ApiException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameIntegrationTest {
    @Autowired LuaRooms rooms; // the composition-root test drives the Lua adapter directly for fixtures
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired Projection projection;
    @Value("${local.server.port}") int port;
    private final List<String> fixtures=new ArrayList<>();
    private final String host="U:"+UUID.randomUUID();
    private String create() {
        String id=UUID.randomUUID().toString();fixtures.add(id);
        rooms.command(id,"init",host,Map.of("id",id,"pin","123456","quizId",UUID.randomUUID().toString(),"owner",host,"title","Contract",
            "questions",List.of(question("one"),question("two"))));return id;
    }
    private Map<String,Object> question(String round){return Map.of("roundId",round,"text","Question?","options",List.of("a","b","c","d"),"correctOption",0,"seconds",120);}
    private void join(String id,String principal){rooms.command(id,"join",principal,Map.of("name",principal,"normalizedName",principal.toLowerCase(Locale.ROOT),"participantId",UUID.randomUUID().toString()));}
    private JsonNode control(String id,String action,String round,String command){return rooms.command(id,action,host,Map.of("roundId",round,"commandId",command));}
    private JsonNode answer(String id,String principal,String round,int option){return rooms.command(id,"answer",principal,Map.of("roundId",round,"option",option,"commandId",UUID.randomUUID().toString()));}
    private JsonNode snapshot(String id,String principal){return rooms.command(id,"snapshot",principal,Map.of());}
    @AfterEach void clean(){for(String id:fixtures){redis.opsForSet().remove(LuaRooms.ACTIVE,id);redis.delete(LuaRooms.keys(id));}}

    @Test @DisplayName("LIVE-03 parallel retries produce one receipt, one score and one event")
    void parallelRetriesProduceOneReceiptOneScoreAndOneAnswerEvent() throws Exception {
        String id=create();join(id,"G:p");control(id,"start","","s");
        List<Callable<String>> jobs=new ArrayList<>();for(int i=0;i<32;i++)jobs.add(()->answer(id,"G:p","one",0).toString());
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
            var results=pool.invokeAll(jobs);Set<String> receipts=new HashSet<>();for(var f:results)receipts.add(f.get());assertEquals(1,receipts.size());
        }
        assertEquals(1L,redis.opsForHash().size(LuaRooms.keys(id).get(3)));
        assertEquals(4L,redis.opsForStream().size(LuaRooms.keys(id).get(7))); // create, join, start, one answer
        assertEquals(1,rooms.meta(id).path("correctCount").asInt());
        assertEquals(1000d,redis.opsForZSet().score(LuaRooms.keys(id).get(4),snapshot(id,"G:p").path("me").path("participantId").asText()));
    }
    @Test @DisplayName("LIVE-03 concurrent correct answers get distinct ranks and tier scores")
    void parallelCorrectPlayersGetDistinctRanksAndTierScores() throws Exception {
        String id=create();for(int i=0;i<20;i++)join(id,"G:p"+i);control(id,"start","","s");
        List<Callable<JsonNode>> jobs=new ArrayList<>();for(int i=0;i<20;i++){String p="G:p"+i;jobs.add(()->answer(id,p,"one",0));}
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){for(var f:pool.invokeAll(jobs))f.get();}
        Set<Integer> ranks=new HashSet<>();int total=0;
        for(Object raw:redis.opsForHash().values(LuaRooms.keys(id).get(3))){var receipt=json.readTree(raw.toString());ranks.add(receipt.path("correctOrder").asInt());total+=receipt.path("points").asInt();}
        assertEquals(20,ranks.size());assertTrue(ranks.contains(1)&&ranks.contains(20));assertEquals(11500,total);
    }
    @Test @DisplayName("LIVE-03 ROOM-03 receipt survives round transition; conflicting answer and stale round are rejected")
    void receiptSurvivesRoundTransitionAndConflictingAnswerIsRejected(){
        String id=create();join(id,"G:p");control(id,"start","","s");var first=answer(id,"G:p","one",0);
        control(id,"reveal","one","r");control(id,"next","one","n");
        assertEquals(first,answer(id,"G:p","one",0));
        assertEquals("ALREADY_ANSWERED",assertThrows(ApiException.class,()->answer(id,"G:p","one",1)).code);
        assertEquals("STALE_ROUND",assertThrows(ApiException.class,()->control(id,"next","one","new-command")).code);
        control(id,"next","one","n"); // same command returns its original acceptance, never advances again
        assertEquals(2,rooms.meta(id).path("index").asInt());
    }
    @Test @DisplayName("LIVE-04 score and correct option stay hidden until reveal")
    void scoreAndCorrectOptionStayHiddenUntilReveal(){
        String id=create();join(id,"G:wrong");join(id,"G:right");control(id,"start","","s");
        answer(id,"G:wrong","one",1);var receipt=answer(id,"G:right","one",0);
        assertFalse(receipt.has("points"));assertFalse(receipt.has("correctOrder"));
        var state=snapshot(id,"G:right");assertTrue(state.path("question").path("correctOption").isNull());
        state.path("players").forEach(p->assertEquals(0,p.path("score").asInt()));
        assertEquals(1,rooms.meta(id).path("correctCount").asInt());
        control(id,"reveal","one","r");state=snapshot(id,"G:right");assertEquals(0,state.path("question").path("correctOption").asInt());
        assertEquals(1000,state.path("players").path(state.path("me").path("participantId").asText()).path("score").asInt());
    }
    @Test @DisplayName("LIVE-01 LIVE-02 JOIN-02 deadline, membership and name checks do not depend on PostgreSQL")
    void deadlineMembershipAndNameChecksDoNotDependOnPostgres(){
        String id=create();join(id,"G:A");
        assertEquals("NAME_TAKEN",assertThrows(ApiException.class,()->join(id,"g:a")).code);
        assertEquals("ROOM_ACCESS_DENIED",assertThrows(ApiException.class,()->snapshot(id,"G:intruder")).code);
        control(id,"start","","s");
        var meta=(tools.jackson.databind.node.ObjectNode)rooms.meta(id);meta.put("deadline",0);redis.opsForValue().set(LuaRooms.keys(id).get(0),meta.toString());
        assertEquals("DEADLINE_PASSED",assertThrows(ApiException.class,()->answer(id,"G:A","one",0)).code);
        rooms.command(id,"tick","system",Map.of());assertEquals("REVEAL",snapshot(id,host).path("phase").asText());
        String participant=snapshot(id,"G:A").path("me").path("participantId").asText();
        rooms.command(id,"kick",host,Map.of("participantId",participant,"commandId","kick"));
        assertEquals("ROOM_ACCESS_DENIED",assertThrows(ApiException.class,()->snapshot(id,"G:A")).code);
        assertEquals("ROOM_ACCESS_DENIED",assertThrows(ApiException.class,()->answer(id,"G:A","one",0)).code);
    }
    @Test @DisplayName("LIVE-07 corrupt key type is rejected before any mutation")
    void corruptKeyTypeIsRejectedBeforeAnyMutation(){
        String id=create();join(id,"G:p");control(id,"start","","s");
        var before=rooms.meta(id);redis.delete(LuaRooms.keys(id).get(4));redis.opsForValue().set(LuaRooms.keys(id).get(4),"wrong-type");
        assertEquals("STATE_CORRUPT",assertThrows(ApiException.class,()->answer(id,"G:p","one",0)).code);
        assertEquals(before,rooms.meta(id));assertEquals(0L,redis.opsForHash().size(LuaRooms.keys(id).get(3)));
    }
    @Test @DisplayName("STUDIO-05 active registration repairs live state but never resurrects a finished room")
    void activeRegistrationRepairsLiveStateButNeverResurrectsFinishedRoom(){
        String id=create();rooms.registerActive(id);
        assertEquals(Boolean.TRUE,redis.opsForSet().isMember(LuaRooms.ACTIVE,id));
        redis.opsForSet().remove(LuaRooms.ACTIVE,id);rooms.registerActive(id);
        assertEquals(Boolean.TRUE,redis.opsForSet().isMember(LuaRooms.ACTIVE,id));
        var meta=(tools.jackson.databind.node.ObjectNode)rooms.meta(id);meta.put("expiresAt",0);
        redis.opsForValue().set(LuaRooms.keys(id).get(0),meta.toString());rooms.command(id,"tick","",Map.of());
        redis.opsForSet().remove(LuaRooms.ACTIVE,id);rooms.registerActive(id);
        assertEquals(Boolean.FALSE,redis.opsForSet().isMember(LuaRooms.ACTIVE,id));
    }
    @Test void hostCommandLedgerCapacityIsAConflictNotARateLimit(){
        String id=create();join(id,"G:p");
        String participant=snapshot(id,"G:p").path("me").path("participantId").asText();
        for(int i=0;i<500;i++)rooms.command(id,"kick",host,Map.of("participantId",participant,"commandId","ledger-"+i));
        var rejected=assertThrows(ApiException.class,()->rooms.command(id,"kick",host,Map.of("participantId",participant,"commandId","ledger-overflow")));
        assertEquals("COMMAND_LIMIT",rejected.code);assertEquals(409,rejected.status); // capacity is a state conflict, not RFC 6585 rate limiting
        assertTrue(rooms.command(id,"kick",host,Map.of("participantId",participant,"commandId","ledger-0")).path("accepted").asBoolean()); // replay of a ledgered command still works when the ledger is full
    }
    @Test @DisplayName("LIVE-01 LIVE-05 WebSocket authenticates by cookie and Origin, pushes state and rejects mutations")
    void websocketAuthenticatesByCookiePushesStateAndRejectsMutations() throws Exception {
        Browser owner=new Browser();Browser guest=new Browser();owner.init();guest.init();
        var login=owner.client.send(owner.builder("/api/auth/login").header("Content-Type","application/x-www-form-urlencoded").header("X-XSRF-TOKEN",owner.csrf)
            .POST(HttpRequest.BodyPublishers.ofString("username=host%40example.test&password=local-quiz-only")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,login.statusCode());owner.init();
        String quizId=owner.get("/api/quizzes").get(0).path("id").asText();
        var room=owner.post("/api/rooms",Map.of("quizId",quizId,"commandId",UUID.randomUUID()));
        guest.post("/api/rooms/join",Map.of("pin",room.path("pin").asText(),"name","Socket guest"));
        String id=room.path("id").asText();
        URI uri=URI.create("ws://localhost:"+port+"/ws/rooms/"+id);
        assertThrows(ExecutionException.class,()->HttpClient.newHttpClient().newWebSocketBuilder()
            .header("Origin","http://localhost:4200").buildAsync(uri,new SocketListener()).get(5,TimeUnit.SECONDS));
        assertThrows(ExecutionException.class,()->guest.client.newWebSocketBuilder()
            .header("Origin","https://untrusted.example").buildAsync(uri,new SocketListener()).get(5,TimeUnit.SECONDS));
        SocketListener listener=new SocketListener();
        var ws=guest.client.newWebSocketBuilder()
            .header("Origin","http://localhost:4200")
            .buildAsync(URI.create("ws://localhost:"+port+"/ws/rooms/"+id),listener).get(5,TimeUnit.SECONDS);
        JsonNode initial=json.readTree(listener.awaitState());
        assertEquals("STATE",initial.path("type").asText());assertEquals(id,initial.path("room").path("id").asText());
        listener.nextError();
        ws.sendText("{\"type\":\"ANSWER\",\"option\":0}",true).get(5,TimeUnit.SECONDS);
        assertEquals("READ_ONLY_CHANNEL",json.readTree(listener.awaitError()).path("code").asText());
        listener.nextState();
        owner.post("/api/rooms/"+id+"/start",Map.of("commandId",UUID.randomUUID()));
        assertEquals("QUESTION",json.readTree(listener.awaitPhase("QUESTION")).path("room").path("phase").asText());
        ws.sendClose(WebSocket.NORMAL_CLOSURE,"done").get(5,TimeUnit.SECONDS);
        SocketListener reconnected=new SocketListener();
        var second=guest.client.newWebSocketBuilder().header("Origin","http://localhost:4200")
            .buildAsync(uri,reconnected).get(5,TimeUnit.SECONDS);
        assertEquals("QUESTION",json.readTree(reconnected.awaitState()).path("room").path("phase").asText());
        String participant=guest.get("/api/rooms/"+id).path("me").path("participantId").asText();
        owner.post("/api/rooms/"+id+"/kick",Map.of("participantId",participant,"commandId",UUID.randomUUID()));
        assertEquals("ROOM_ACCESS_DENIED",json.readTree(reconnected.revoked.get(5,TimeUnit.SECONDS)).path("code").asText());
        second.abort();
    }
    @Test @DisplayName("LOGIN-01 LOGIN-03 JOIN-03 JOIN-04 STUDIO-05 LIVE-01 LIVE-06 real cookies, CSRF, authorization, idempotent create and archive replay")
    void realCookiesCsrfAuthorizationReconnectAndArchiveReplay() throws Exception {
        Browser owner=new Browser();Browser guest=new Browser();Browser stranger=new Browser();
        owner.init();guest.init();stranger.init();
        var csrfFailure=guest.request("POST","/api/rooms/join", "{}",false);
        assertEquals(403,csrfFailure.statusCode());assertEquals("CSRF_REQUIRED",json.readTree(csrfFailure.body()).path("code").asText());
        assertEquals(401,guest.request("GET","/api/quizzes",null,false).statusCode());
        var login=owner.client.send(owner.builder("/api/auth/login").header("Content-Type","application/x-www-form-urlencoded").header("X-XSRF-TOKEN",owner.csrf)
            .POST(HttpRequest.BodyPublishers.ofString("username=host%40example.test&password=local-quiz-only")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,login.statusCode());owner.init();
        assertTrue(owner.get("/api/auth/me").path("host").asBoolean());
        String quizId=owner.get("/api/quizzes").get(0).path("id").asText();
        String command=UUID.randomUUID().toString();var room=owner.post("/api/rooms",Map.of("quizId",quizId,"commandId",command));
        String id=room.path("id").asText();String base="/api/rooms/"+id;
        assertEquals(id,owner.post("/api/rooms",Map.of("quizId",quizId,"commandId",command)).path("id").asText());
        guest.post("/api/rooms/join",Map.of("pin",room.path("pin").asText(),"name","Guest"));
        assertEquals(403,stranger.request("GET",base,null,false).statusCode());
        assertEquals(403,guest.request("POST",base+"/start",json.writeValueAsString(Map.of("commandId",UUID.randomUUID())),true).statusCode());
        room=owner.post(base+"/start",Map.of("commandId",UUID.randomUUID()));
        String round=room.path("question").path("roundId").asText();
        var receipt=guest.post(base+"/answers",Map.of("roundId",round,"option",0,"commandId",UUID.randomUUID()));
        assertEquals(receipt.path("answerId"),guest.get(base).path("me").path("answer").path("answerId"));
        while(!room.path("phase").asText().equals("FINISHED")){
            round=room.path("question").path("roundId").asText();
            owner.post(base+"/reveal",Map.of("roundId",round,"commandId",UUID.randomUUID()));
            room=owner.post(base+"/next",Map.of("roundId",round,"commandId",UUID.randomUUID()));
        }
        long deadline=System.nanoTime()+Duration.ofSeconds(15).toNanos();
        while(System.nanoTime()<deadline && !"FINISHED".equals(db.queryForObject("select phase from game_room where id=?",String.class,UUID.fromString(id))))Thread.sleep(100);
        assertEquals("FINISHED",db.queryForObject("select phase from game_room where id=?",String.class,UUID.fromString(id)));
        JsonNode listed=null;for(JsonNode row:owner.get("/api/history"))if(row.path("id").asText().equals(id))listed=row;
        assertNotNull(listed,"history list must contain the finished room");
        System.out.println("history row: "+listed);
        assertEquals("FINISHED",listed.path("phase").asText());
        assertTrue(listed.path("archivedVersion").isNumber(),listed.toString());
        assertFalse(listed.path("createdAt").isMissingNode()||listed.path("createdAt").isNull(),listed.toString());
        assertFalse(listed.path("finishedAt").isMissingNode()||listed.path("finishedAt").isNull(),listed.toString());
        assertFalse(listed.has("archived_version")||listed.has("created_at")||listed.has("finished_at"),listed.toString()); // camelCase per conventions
        var event=db.queryForMap("select stream_id,payload from game_event where room_id=? and kind='ANSWERED'",UUID.fromString(id));
        projection.apply(id,event.get("stream_id").toString(),RoomEvent.of(json.readValue(event.get("payload").toString(),Map.class)));
        assertEquals(1,db.queryForObject("select count(*) from answer where room_id=?",Integer.class,UUID.fromString(id)));
        assertEquals(1000,owner.get("/api/history/"+id).get(0).path("score").asInt());
        assertEquals(204,owner.request("POST","/api/auth/logout","{}",true).statusCode());
        assertEquals(401,owner.request("GET","/api/quizzes",null,false).statusCode());
    }
    private class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String csrf;
        HttpRequest.Builder builder(String path){return HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(10));}
        HttpResponse<String> request(String method,String path,String body,boolean token) throws Exception {
            var b=builder(path);if(token)b.header("X-XSRF-TOKEN",csrf);
            if(body!=null)b.header("Content-Type","application/json");
            return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
        JsonNode get(String path)throws Exception{var r=request("GET",path,null,false);assertEquals(200,r.statusCode(),r.body());return json.readTree(r.body());}
        JsonNode post(String path,Object body)throws Exception{var r=request("POST",path,json.writeValueAsString(body),true);assertEquals(200,r.statusCode(),r.body());return json.readTree(r.body());}
        void init()throws Exception{csrf=get("/api/auth/csrf").path("token").asText();get("/api/auth/me");}
    }
    private static final class SocketListener implements WebSocket.Listener {
        private final CompletableFuture<String> revoked=new CompletableFuture<>();
        private final BlockingQueue<String> states=new LinkedBlockingQueue<>();
        private volatile CompletableFuture<String> error=new CompletableFuture<>();
        private final StringBuilder buffer=new StringBuilder();
        private final Object lock=new Object();
        @Override public void onOpen(WebSocket webSocket){webSocket.request(1);}
        @Override public CompletionStage<?> onText(WebSocket webSocket,CharSequence data,boolean last){
            synchronized(lock){buffer.append(data);if(last){String value=buffer.toString();buffer.setLength(0);try{JsonNode message=new ObjectMapper().readTree(value);if("STATE".equals(message.path("type").asText()))states.offer(value);if("ERROR".equals(message.path("type").asText()))error.complete(value);if("REVOKED".equals(message.path("type").asText()))revoked.complete(value);}catch(Exception ignored){}}}
            webSocket.request(1);return CompletableFuture.completedFuture(null);
        }
        String awaitState() throws Exception{String value=states.poll(5,TimeUnit.SECONDS);assertNotNull(value,"Timed out waiting for STATE");return value;}
        String awaitPhase(String phase) throws Exception {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<end){
                String value=states.poll(Math.max(1,end-System.nanoTime()),TimeUnit.NANOSECONDS);
                if(value==null)break;
                if(new ObjectMapper().readTree(value).path("room").path("phase").asText().equals(phase))return value;
            }
            throw new AssertionError("Timed out waiting for phase "+phase);
        }
        String awaitError() throws Exception{return error.get(5,TimeUnit.SECONDS);}
        void nextState(){states.clear();}
        void nextError(){error=new CompletableFuture<>();}
    }
}
