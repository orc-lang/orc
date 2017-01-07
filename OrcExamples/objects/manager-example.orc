{- supervisor-example.orc -- Supervisor example
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "supervisor.inc"

class def Proc() :: Proc extends Supervisable {
  val running = Ref(true)
  val shuttingDownSemaphore = Semaphore(0)
  def monitorUsefulness() = false

  def shutdown() = (running := false, shuttingDownSemaphore.release()) >> signal
  
  val _ = repeat({ Rwait(400) >> Ift(running?) >> Println("Ping") })
  val _ = shuttingDownSemaphore.acquire() >> seqMap(ignore({ Rwait(400) >> Println("Pong") }), [1, 2, 3])
}

val m = Manager(Proc)
m.start() >>
Rwait(1000) >>
(m.shutdown(1000) | Rwait(900) >> Println("Not halted yet") >> stop) >>
Println("Test 1 done") >>
m.start() >>
Rwait(1000) >>
(m.shutdown(3000) | Rwait(1300) >> Println("Just halted (If this is before 'Test 2 done' then shutdown is not publishing when shutdown is complete naturally)") >> stop) >>
Println("Test 2 done") >>
m.start() >>
Rwait(1000) >>
m.kill() >>
Println("Test 3 done") >> stop

{-
OUTPUT:
Ping
Ping
Pong
Pong
Not halted yet
Test 1 done
Ping
Ping
Pong
Pong
Pong
Test 2 done
Just halted (If this is before 'Test 2 done' then shutdown is not publishing when shutdown is complete naturally)
Ping
Ping
Test 3 done
-}
