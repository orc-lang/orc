-- Runnable version of OrcO: A Concurrency-First Approach to Objects Figure 7
-- A more advanced version is in objects/supervisors.inc

import site Block = "orc.lib.util.Block"

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

{--
Call callable when this def call is killed.
As a side effect this will never halt.
-}
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)

-- Supervisor library
class ClearableRef {
  val ref = Ref(Cell())
  val clearLock = Semaphore(1)
  
  -- Calling the object is a blocking read
  val apply = read
  -- Blocking read
  def read() = ref.read().read()
  -- Nonblocking read
  def readD() = ref.read().readD()
  -- Set the value of this
  site write(v) = ref.read().write(v)
  -- True if this is currently set
  def isDefined() = readD() >> true ; false
  -- Clear this into the unset state (future calls to read will block until it is set again)
  def clear() = withLock(clearLock, { 
    -- To avoid a read blocking indefinitely this can never go form one empty Cell to another.
    if isDefined() then ref.write(Cell()) else signal
  })
}

class SubordinateRef {
  val constructor
  
  val killSwitch = Semaphore(0)
  val current = new ClearableRef
  
  def get() = current.read()

  def run() = {|
      (current.write(constructor()) >> stop ; signal) |
      killSwitch.acquire() |
      runOnKill({ current.clear() }) >> stop
    |}

  def kill() = killSwitch.release()
}
def SubordinateRef(c) = new SubordinateRef { val constructor = c }

class Supervisor {
  val subordinates
}

class AllForOneSupervisor extends Supervisor {
  val _ = repeat({
    {| each(subordinates) >m> (m.run() >> stop ; signal) |}
  })
}

class OneForOneSupervisor extends Supervisor {
  val _ =
    each(subordinates) >m> repeat({
      m.run() >> stop ; signal
    })
}

-- Simulators

class DB {
  val _ = Println("DB Starting")
  val data = Ref[List[Top]]([1,2,3])

  site update(v) = data := (v : data?)
  site get(i) = Println("Getting " + i) >> index(data?, i)
}

class Server {
  val db :: DB

  val _ = Println("Server Starting")

  site request(n) = db.get(n)
}

-- Application

class App extends AllForOneSupervisor {
  val subordinates = [webServers, dbServer]
  val webServers = SubordinateRef({
    new OneForOneSupervisor {
      val subordinates = [webSv1, webSv2]
      val webSv1 = SubordinateRef({ new Server { val db = dbServer.get() } })
      val webSv2 = SubordinateRef({ new Server { val db = dbServer.get() } })
    }
  })
  val dbServer = SubordinateRef({ new DB })
}

{|

val s = new App #

repeat({
  s.dbServer.get().update(42) >>
  Println(s.webServers.get().webSv1.get().request(0)) >>
  Rwait(500)
}) >> stop
|
(
Rwait(2000) >> s.dbServer.kill() >>
Rwait(1000)
)

|}
