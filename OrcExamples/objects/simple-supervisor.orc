class Manager {
  val constructor :: lambda() :: Top
  
  val killSwitch = Semaphore(0)
  val current = Ref()
  
  def get() = current.read()

  def run() = {|
    ((constructor() >o> current.write(o) >> stop) ; signal) |
    killSwitch.acquire()
  |}

  def kill() = killSwitch.release()
}
def Manager(c) = new Manager { val constructor = c }

class Supervisor {
  val managers :: Top
}

class AllForOneSupervisor extends Supervisor {
  val _ = repeat({
    def kill(m) = m.kill()
    {| each(managers) >m> (m.run() >> stop ; signal) |}
  })
}

class OneForOneSupervisor extends Supervisor {
  val _ =
    def manage(m) = repeat({
      m.run() >> stop ; signal
    })
    map(manage, managers)
}

class def DB() :: DB {
  val _ = Println("DB Starting")
  val data = Ref[List[Top]]([1,2,3])
  site c() = stop

  def update(v) = data := (v : data?)
  def get(i) = Println("Getting " + i) >> index(data?, i)
}

class def Server(db :: DB) :: Server {
  val _ = Println("Server Starting")
  site c() = stop
  def request(n) = db.get(n)
}

class S extends AllForOneSupervisor {
  val managers = [servers, db]
  val servers = Manager({
    new OneForOneSupervisor {
      def newServer(i) = Manager({ Server(db.get()) })
      val managers = [s1, s2]
      val s1 = newServer(1)
      val s2 = newServer(2)
    }
  })
  val db = Manager({ DB() })
}
val s = new S #

(
Println(s.db) >>
Println(s.db.get()) >> 
Println(s.servers) >>
Println(s.servers.get()) >>
Println(s.servers.get().s1) >>
Println(s.servers.get().s1.get())
)
|
repeat({
  s.db.get().update(42) >>
  Println(s.servers.get().s1.get().request(0)) >>
  Rwait(500)
}) 
|
(
Rwait(1000) >> s.db.kill() >>
Rwait(1000) >> s.servers.get().s1.kill()
)
