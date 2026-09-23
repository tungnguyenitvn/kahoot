# Minimal Lua 5.4 runner for scripts/test-room.lua; used by compose.test.yaml (lua-smoke).
FROM alpine:3.21
RUN apk add --no-cache lua5.4
