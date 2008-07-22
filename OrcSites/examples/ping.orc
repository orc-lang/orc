include "net.inc"

-- Ping a server and return
-- "success" or "fail"
def PingTest(server, t) = pong
  <pong<   Ping(server, t) >> "success"
         | Rtimer(t) >> "fail"

-- Every 5 seconds, check if the localhost
-- responds within 2 seconds
MetronomeT(5000) >> PingTest("localhost", 2000)