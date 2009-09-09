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
mbox: our mailbox
missing: set of neighboring philosophers holding our fork
-}
def philosopher(name, mbox, missing) =
  val send = mbox.put
  val receive = mbox.get
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
    def on(("rumble", _)) =
      println(name + " hungry") >>
      map(requestFork, missing) >>
      hungry()
    def on(("request", p)) =
      sendFork(p) >>
      thinking()
    on(receive())

  def hungry() =
    def on(("fork", p)) =
      missing.remove(p) >>
      if missing.isEmpty()
      then
        println(name + " eating") >>
        eating()
      else hungry()
    def on(("request", p)) =
      if clean.contains(p)
      then
        deferred.put(p) >>
        hungry()
      else
        sendFork(p) >>
        requestFork(p) >>
        hungry()
    on(receive())

  def eating() =
    clean.clear() >>
    Rtimer(random(10)) >>
    map(sendFork, deferred.getAll()) >>
    digesting()

  digesting()

{-
Create an NxN 4-connected grid of philosophers.  Each philosopher holds the
fork for the connections below and to the right (so the top left philosopher
holds both its forks).
-}
def philosophers(n) =
  {- channels -}
  val cs = uncurry(IArray(n, lambda (_) = IArray(n, ignore(Buffer))))

  {- first row -}
  philosopher((0,0), cs(0,0), makeSet([]))
  | for(1, n) >j>
    philosopher((0,j), cs(0,j), makeSet([cs(0,j-1).put]))

  {- remaining rows -}
  | for(1, n) >i> (
      philosopher((i,0), cs(i,0), makeSet([cs(i-1,0).put]))
      | for(1, n) >j>
        philosopher((i,j), cs(i,j), makeSet([cs(i-1,j).put, cs(i,j-1).put]))
    )

philosophers(2)
