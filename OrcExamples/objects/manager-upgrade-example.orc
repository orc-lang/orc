{- supervisor-example.orc -- Supervisor example
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "debug.inc"
include "supervisor.inc"

class Proc extends Supervisable {
  val running = Ref(true)
  val shuttingDownSemaphore = Semaphore(0)
  def monitorUsefulness() = false

  def shutdown() = (Println("Shutting down"), running := false, shuttingDownSemaphore.release()) >> signal
  val _ = Println("Starting up")
}

class def Proc1() :: Proc extends Proc {
  val _ = repeat({ Rwait(400) >> Ift(running?) >> Println("Ping") })
  val _ = shuttingDownSemaphore.acquire() >> seqMap(ignore({ Rwait(400) >> Println("Pong") }), [1, 2, 3])
}
class def Proc2() :: Proc extends Proc {
  val _ = repeat({ Rwait(400) >> Ift(running?) >> Println("Ping!") })
  val _ = shuttingDownSemaphore.acquire() >> seqMap(ignore({ Rwait(400) >> Println("Pong!") }), [1])
}

val m = ManagerUpgradable(Proc1)
m.start() >>
Rwait(1000) >>
m.upgrade(Proc2, 1000) >>
Rwait(1000) >>
m.shutdown(1000) >> stop
--}
--| Rwait(5000) >> DumpState()

{-
OUTPUT:
Starting up
Ping
Ping
Shutting down
Pong
Pong
Starting up
Ping!
Ping!
Shutting down
Pong!
-}
