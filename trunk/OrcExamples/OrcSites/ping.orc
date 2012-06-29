{- ping.orc -- Orc program that 'ping's the local host every 5 seconds
 -}

include "net.inc"

-- Ping a server and return
-- "success" or "fail"
def PingTest(server, t) = pong
  <pong<   Ping(server, t) >> "success"
         | Rwait(t) >> "fail"

-- Every 5 seconds, check if the localhost
-- responds within 2 seconds
metronome(5000) >> PingTest("localhost", 2000)
