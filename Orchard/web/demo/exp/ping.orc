include "net.inc"

-- Check if a server responds to
-- a ping within t milliseconds
def PingTest(t, server) = pong
  <pong<   Ping(server) >> "success"
         | Rtimer(t) >> "error"

-- Every 10 seconds, check if the server responds
-- within 5 seconds
MetronomeT(10000) >> PingTest(5000, "localhost")
