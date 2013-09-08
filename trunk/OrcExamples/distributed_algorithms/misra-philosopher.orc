{- misra-philosopher.orc
 -
 - The "hygenic solution to the diners problem", described in
 - K. M. Chandy and J. Misra. 1984. The drinking philosophers problem.
 - ACM Trans. Program. Lang. Syst. 6, 4 (October 1984), 632-646.
 -}

-- Use a Scala set implementation.
-- Operations on this set are _not_ synchronized.
import class ScalaSet = "scala.collection.mutable.HashSet"

{-
Make a set initialized to contain
the items in the given list.
-}
def Set[A](items :: List[A]) = ScalaSet[A]() >s> joinMap(s.add, items) >> s


type Message = (String, lambda((String, lambda(Top) :: Signal)) :: Signal)
type Xmitter = lambda(Message) :: Signal

{-
Start a philosopher process; never publishes.

name: identify this process in status messages
mbox: our mailbox
missing: set of neighboring philosophers holding our fork
-}
def philosopher(name :: (Integer, Integer), mbox :: Channel[Message], missing :: ScalaSet[Xmitter]) :: Bot =
  val send = mbox.put
  val receive = mbox.get
  -- deferred requests for forks
  val deferred = Channel[Xmitter]()
  -- forks we hold which are clean
  val clean = Set[Xmitter]([])

  def sendFork(p :: Xmitter) =
    missing.add(p) >>
    p(("fork", send))

  def requestFork(p :: Xmitter) =
    clean.add(p) >>
    p(("request", send))

  -- While thinking, start a timer which
  -- will tell us when we're hungry
  def digesting() :: Bot =
      Println(name + " thinking") >>
      thinking()
    | Rwait(Random(30)) >>
      send(("rumble", send)) >>
      stop

  def thinking() :: Bot =
    def on(("rumble", _) :: Message) =
      Println(name + " hungry") >>
      map(requestFork, missing.toList() :!: List[Xmitter]) >>
      hungry()
    def on(("request", p)) =
      sendFork(p :!: Xmitter) >>
      thinking()
    on(receive())

  def hungry() :: Bot =
    def on(("fork", p) :: Message) =
      missing.remove(p :!: Xmitter) >>
      (
        if missing.isEmpty() then
          Println(name + " eating") >>
          eating()
        else hungry()
      )
    def on(("request", p)) =
      if clean.contains(p :!: Xmitter) then
        deferred.put(p :!: Xmitter) >>
        hungry()
      else
        sendFork(p :!: Xmitter) >>
        requestFork(p :!: Xmitter) >>
        hungry()
    on(receive())

  def eating() :: Bot =
    clean.clear() >>
    Rwait(Random(10)) >>
    map(sendFork, deferred.getAll()) >>
    digesting()

  digesting()

{-
Create an NxN 4-connected grid of philosophers.  Each philosopher holds the
fork for the connections below and to the right (so the top left philosopher
holds both its forks).
-}
def philosophers(n :: Integer) =
  {- channels -}
  val cs = uncurry(Table(n, lambda (_::Top) = Table(n, lambda(_::Top) = Channel[Message]())))

  {- first row -}
  philosopher((0,0), cs(0,0), Set[Xmitter]([]))
  | for(1, n) >j>
    philosopher((0,j), cs(0,j), Set[Xmitter]([cs(0,j-1).put]))

  {- remaining rows -}
  | for(1, n) >i> (
      philosopher((i,0), cs(i,0), Set[Xmitter]([cs(i-1,0).put]))
      | for(1, n) >j>
        philosopher((i,j), cs(i,j), Set[Xmitter]([cs(i-1,j).put, cs(i,j-1).put]))
    )

philosophers(2)
