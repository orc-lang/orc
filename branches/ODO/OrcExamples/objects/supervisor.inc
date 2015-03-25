{- supervisor.inc -- The supervisor pattern
 -
 - $Id$
 -
 - This library is inspired by Erlang/OTP but uses object orientation to make it easier for 
 - objects under the same superviser to communicate. This approach is type-safe, but requires
 - that the user implement a group object that generates all the required managers. So without
 - some change this system cannot support dynamic addition and removal of processes.
 -
 - For an example of usage see supervisor-example.orc.
 - 
 - Created by amp on Mar 19, 2015 7:37:18 PM
 -}


import site Block = "orc.lib.util.Block"

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

-- Call callable when this def call is killed.
-- As a side effect this will never halt.
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)

-- Convert a function with 2 arguments into a function with a single pair argument.
def uncurryTuple[A, B, T](f :: lambda(A, B) :: T) :: lambda((A, B)) :: T = 
  def unpackFunc((a, b) :: (A, B)) :: T = f(a, b)
  unpackFunc
 
class def Notifier() :: Notifier {
  -- A lock to protect internal data structure
  val lock = Semaphore(1)
  -- A list of output ("listener") channels
  val listeners = Ref[List[Semaphore]]([])
  
  -- Put an value on every listener channel
  site notify() = (each(listeners?) >c> c.release()) ; signal
  
  -- Return a listener semaphore that will be active until this call is killed
  def newListener() = withLock(lock, {
    val c = Semaphore(0)
    runOnKill({ removeListener(c) }) >>
    listeners := c : listeners? >>
    c
  })
  
  -- Remove a listener semaphore
  def removeListener(c :: Channel) = withLock(lock, {
    listeners := filter(listeners?, { _ /= c })
  })
  
  -- Publish each time this is notified while this call is active
  def listen() =
    val c = newListener()
    repeat(c.acquire)
  -- Publish when this is notified then halt
  def wait() =
    {| newListener().acquire() |}
}

class def ClearableRef() :: ClearableRef {
  val ref = Ref(Cell())
  --val setNotifier = Notifier()
  val clearLock = Semaphore(1)
  
  -- Calling the object is a blocking read
  val apply = read
  -- Blocking read
  def read() = ref.read().read()
  -- Nonblocking read
  def readD() = ref.read().readD()
  -- Set the value of this
  def write(v) = ref.read().write(v) -->> setNotifier.notify()
  -- True if this is currently set
  def isDefined() = readD() >> true ; false
  -- Clear this into the unset state (future calls to read will block until it is set again)
  def clear() = withLock(clearLock, { 
    -- To avoid a read blocking indefinitely this can never go form one empty Cell to another.
    if isDefined() then ref.write(Cell()) else signal
  })
}

type T = Top

{--
Manage an running object.

This class provides methods to:
* Get the current instance (possibly blocking until one is available)
* Start an instance if needed
* Shutdown and kill the running instance
* Detect when the instance halts (or is killed)
-}
class def Manager(constructor :: lambda() :: T) :: Manager {
  -- Get the currently running instance of T block if there is none
  def apply() :: T = current()
  -- Nonblocking get for the currently running instance
  def readD() :: T = current.readD()
  
  val current = ClearableRef[T]()
  val haltedChannel = Notifier()
  val killSwitch = Notifier()
  
  -- Return the currently running instance starting one if needed
  site start() :: T =
    Ift(~(current.isDefined())) >>
    ({| (current := constructor() >> stop ; signal) | killSwitch.wait() |} >> stop ; halted()) >> stop 
    | current()
  -- Politely shutdown the currently running instance
  site shutdown(timeout :: Integer) = 
  	current.readD() >s> {| 
  	  haltedChannel.newListener() >l>  
  	  (
  	    s.shutdown() >> stop | 
  	    current.clear() >> stop | 
  	    Rwait(timeout) >> killSwitch.notify() >> stop |
  	    l.acquire()
  	  )
  	|}
  -- Kill the currently running instance
  site kill() = current.readD() >> (killSwitch.notify(), current.clear()) >> signal
  
  -- Publish whenever the instance halts (including when it is killed or shutdown)
  def onHalted() = haltedChannel.listen()
  
  def halted() = current.clear() >> haltedChannel.notify()
}

class Supervisable {
  -- Politely request that this halts
  def shutdown() :: Signal
}

type ID = Top

{--
A SupervisableGroup represents a group of Supervisables that are supervised
by the same superviser.

This provides information to the supervisor that allows it to start managers for 
objects and restart them as needed. The managers are communicated back to this group.
The group should implement a type-safe interface to the elements that can be used by
classes that are aware of the groups full type.
-}
class SupervisableGroup {
  -- A list of managers that the supervisor should use
  val managers :: Set[Manager[S]]
}

type S = Top
type G = Top

class Supervisor {
  val group :: G
  val killTime :: Integer 
  site shutdown() :: Signal
}

type SupervisableBuilder[S, G] = lambda(Supervisor) :: Supervisable

class SupervisorBase extends Supervisor with Supervisable {
  val groupConstructor :: lambda() :: G
  val group = groupConstructor()
  val startAllowed = Ref(true)
  val finalShutdownSwitch = Notifier()

  val managers = group.managers
  def start(m) = if startAllowed? then m.start() else stop
  def startAll() = map(start, managers) >> signal
 
  site shutdownAll() =
    def f(m) = m.shutdown(killTime) ; signal -- If shutdown fails (due to it not currently running) just keep going
    map(f, managers) >> signal
  site shutdown() = startAllowed := false >> finalShutdownSwitch.notify() >> shutdownAll()
}

class def OneForOneSupervisor(groupConstructor :: lambda() :: G, killTime :: Integer) :: Supervisor extends SupervisorBase {  
  val _ = startAll()
  val _ = {| finalShutdownSwitch.wait() | each(managers) >m> m.onHalted() >> start(m) >> stop |}
}

class def AllForOneSupervisor(groupConstructor :: lambda() :: G, killTime :: Integer) :: Supervisor extends SupervisorBase {  
  val _ = startAll()
  val _ = repeat({ {| each(managers) >m> m.onHalted() |} >> 
    shutdownAll() >> startAll() })
}