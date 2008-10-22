{-
The "hygenic solution to the diners problem",
described in "The Drinking Philosophers Problem", by
K. M. Chandy and J. Misra.
-}

-- Having a set data structure is convenient,
-- although Orc lists would do in a pinch
class Set = java.util.HashSet

{-
Make a set initialized to contain
the items in the given list.
-}
def makeSet(items) =
  val s = Set()
  map(s.add, items) >> s

{-
Start a philosopher process; never publishes.

name: identify this process in status messages
send: sending end of our mailbox
receive: receiving end of our mailbox
missing: set of neighboring philosophers holding our fork
-}
def philosopher(name, send, receive, missing) =
  -- deferred requests for forks
  val deferred = Buffer()
  -- forks we hold which are clean
  val clean = Set()

  def sendFork(p) =
    missing.add(p) >>
    p(("fork", send))
 
  def requestFork(p) =
    clean.add(p) >>
    p(("request", send))
  
  -- While thinking, start a timer which
  -- will tell us when we're hungry
  def digesting() =
      println(name + " thinking") >>
      thinking()
    | Rtimer(random(30)) >>
      send(("rumble", send)) >>
      stop

  def thinking() =
    def match(("rumble", _)) =
      println(name + " hungry") >>
      map(requestFork, missing) >>
      hungry()
    def match(("request", p)) =
      sendFork(p) >>
      thinking()
    match(receive())

  def hungry() =
    def match(("fork", p)) =
      missing.remove(p) >>
      if missing.isEmpty()
      then
        println(name + " eating") >>
        eating()
      else hungry()
    def match(("request", p)) =
      if clean.contains(p)
      then
        deferred.put(p) >>
        hungry()
      else
        sendFork(p) >>
        requestFork(p) >>
        hungry()
    match(receive())

  def eating() =
    clean.clear() >>
    Rtimer(random(10)) >>
    map(sendFork, deferred.getAll()) >>
    digesting()

  digesting()

{-
Create the graph of philosophers:
Vertices = a, b, c
Edges = a->b, a->c, b->c
-}
val a = Buffer()
val b = Buffer()
val c = Buffer()
  philosopher("A", a.put, a.get, makeSet([b.put, c.put]))
| philosopher("B", b.put, b.get, makeSet([c.put]))
| philosopher("C", c.put, c.get, makeSet([]))