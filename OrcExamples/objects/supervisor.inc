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

{--
Call callable when this def call is killed.
As a side effect this will never halt.
-}
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)

site acquireReleaseOnKillHandler(check :: lambda() :: Top, acquire :: lambda() :: Signal, release :: lambda() :: Top) =
  acquire() >> ((check() ; release()) >> stop | signal)

{-- 
Call acquire then publish a callable that will call release and also call release if this 
is killed. Release will never be called more than once.
-}
def acquireReleaseOnKill(acquire :: lambda() :: Signal, release :: lambda() :: Signal) = 
  val f = Semaphore(0)
  val check = Cell()
  {|
  site c() :: Top = Block()
  check := c >> f.acquire()
  |} >> stop |
  acquireReleaseOnKillHandler(check.read(), acquire, release) >> f.release
  
-- Convert a function with 2 arguments into a function with a single pair argument.
def uncurryTuple[A, B, T](f :: lambda(A, B) :: T) :: lambda((A, B)) :: T = 
  def unpackFunc((a, b) :: (A, B)) :: T = f(a, b)
  unpackFunc
  
class MonitorLock {
  val lock = Semaphore(1)
  def synchronized[T](f :: lambda() :: T) = 
	  acquireReleaseOnKill(lock.acquire, lock.release) >earlyRelease> (
	    val r = f() #
	    (r ; signal) >> earlyRelease() >> r
	  )
}

import class JavaConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
class def Map() :: Map {
  val impl = JavaConcurrentHashMap()
  site get(k) = impl.get(k) >v> Ift(v /= null) >> v
  val apply = get
  site put(k, v) = impl.put(k, v)
  site contains(k) = impl.contains(k)
  site remove(k) = impl.remove(k)
  site keys() = iterableToList(impl.keySet())
}

  
 
class def Notifier() :: Notifier extends MonitorLock {
  -- A list of output ("listener") channels
  val listeners = Ref[List[Semaphore]]([])
  
  -- Put an value on every listener channel
  site notify() = (each(listeners?) >c> c.release()) ; signal
  
  -- Return a listener semaphore that will be active until this call is killed
  def newListener() = synchronized({
    val c = Semaphore(0)
    runOnKill({ removeListener(c) }) >>
    listeners := c : listeners? >>
    c
  })
  
  -- Remove a listener semaphore
  def removeListener(c :: Channel) = synchronized({
    listeners := filter({ _ /= c }, listeners?)
  })
  
  -- Publish each time this is notified while this call is active
  def listen() =
    val c = newListener()
    repeat(c.acquire)
  -- Publish when this is notified then halt
  def wait() =
    {| newListener().acquire() |}
}

-- TODO: This is actually a clearable cell not a clearable ref.
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

{--
The interface for objects that can be monitored to detect shutdown or failure.

This interface is an alternative to detecting halting in many cases. However it allows
"doneness" to be detected on objects that have site members and as such will never halt.
-}
class Monitorable {
  {--
  Publish when this object is not longer useful either because it has shutdown or because a fault has occured.
  This may still respond to queries, but if this has published those results are in some way undesirable.
  If the object is already useless when this is called it will immediately publish.
  -}
  def onUseless() = {| monitorUsefulness() >b> (if b then signal else Block()) ; signal |}
  
  {--
  Publish true OR halt without publishing if this object is no longer useful.
  Publish false if monitoring failed and the object may well still be useful.
  This is what objects should implement but external usage should be through
  -}
  site monitorUsefulness() :: Boolean
}

{--
The interface for objects that can be requested to shutdown.
-}
class Shutdownable {
  {--
  Politely request that this object halt.
  The shutdown may occur asynchrously.
  -}
  def shutdown() :: Signal
}

{--
A combination of shutdown and monitoring.
-}
class Supervisable extends Monitorable with Shutdownable {
  --def shutdownAndWait() = shutdown() >> stop | onUseless()  
}


type T = Top

class IdentityComparable {}

class ManagementInterface extends IdentityComparable {
  site start() :: Supervisable
  site shutdown(Integer) :: Signal
  site kill() :: Signal
  
  val haltedNotifier :: Notifier
  def onHalted() :: Signal
}

{--
Manage an running object.

This class provides methods to:
* Get the current instance (possibly blocking until one is available)
* Start an instance if needed
* Shutdown and kill the running instance
* Detect when the instance halts (or is killed)
-}
class Manager extends MonitorLock {
  val constructor :: lambda() :: T

  -- Get the currently running instance of T block if there is none
  def apply() :: T = current()
  -- Nonblocking get for the currently running instance
  def readD() :: T = current.readD()
  def isRunning() :: Boolean = readD() >> true ; false
  
  val current = ClearableRef()
  val haltedNotifier = Notifier()
  val killSwitch = Notifier()
  
  -- Return the currently running instance starting one if needed
  site start() :: T = synchronized({
    Ift(~(current.isDefined())) >>
    ({| (current := constructor() >> stop ; signal) | killSwitch.wait() | current().onUseless() |} >> stop ; halted()) >> stop 
    | current()
  })
  -- Politely shutdown the currently running instance
  site shutdown(timeout :: Integer) = synchronized({
  	current.readD() >s> {| 
  	  haltedNotifier.newListener() >l>
  	  (
  	    s.shutdown() >> stop | 
  	    current.clear() >> stop | 
  	    Rwait(timeout) >> killSwitch.notify() >> stop |
  	    l.acquire()
  	  )
  	|}
  })
  -- Kill the currently running instance
  site kill() = synchronized({ current.readD() >> (killSwitch.notify(), current.clear()) >> signal })
  
  -- Publish whenever the instance halts (including when it is killed or shutdown)
  def onHalted() = haltedNotifier.listen()
  
  def halted() = current.clear() >> haltedNotifier.notify()
}
def Manager(c :: lambda() :: T) :: Manager = new Manager with {val constructor = c}

class def ManagerUpgradable(initialConstructor :: lambda() :: T) :: ManagerUpgradable extends Manager {
  val constructorRef = Ref(initialConstructor)
  
  def constructor() = constructorRef.read()()
  
  site upgrade(newConstructor :: lambda() :: T, timeout :: Integer) =
    synchronized({ constructorRef := newConstructor }) >>
    (shutdown(timeout) ; signal) >> start() >> signal
}


class Supervisor extends Supervisable {
  val killTime :: Integer
  {--
  Shutdown this supervisor and all of the managers it is supervising
  -}
  site shutdown() :: Signal
  {--
  Add a manager to this supervisor.
  -}
  site add(ManagementInterface) :: ManagementInterface
  {--
  Remove a manager from this supervisor. This does not shutdown the process in the manager.
  This publishes once the manager will nolonger be called by this supervisor.
  -}
  site remove(ManagementInterface) :: Signal
}

class StaticSupervisor extends Supervisor {
  -- A list of managers that the supervisor should use
  val managers :: Set[ManagementInterface]
  
  val _ = each(managers) >m> add(m)
}

class SupervisorBase extends Supervisor {
  val startAllowed = Ref(true)
  val finalShutdownSwitch = Cell[Signal]()
  val shutdownComplete = Cell[Signal]()
 
  site monitorUsefulness() = shutdownComplete.read() >> true
  
  val managerMap = Map[ManagementInterface, Cell[Semaphore]]()
  val managerChannel = Channel[ManagementInterface]()
  val managerMapLock = Semaphore(1)
  
  site add(m) = withLock(managerMapLock, {
    managerMap.put(m, Cell()) >> managerChannel.put(m) >> m
  })
  site remove(m) = withLock(managerMapLock, {
    val s = Semaphore(0)
    managerMap(m).write(s) >> s.acquire()
  })
  val _ = repeat(managerChannel.get) >m> {|
    finalShutdownSwitch.read() >> None | 
    managerMap(m).read() >s> managerMap.remove(m) >> Some(s) | 
    monitorManager(m) >> start(m) >> stop
  |} >Some(s)> s.release()
  
  {-- 
  Called to monitor the manager. This should publish once when it is setup to monitor.
  The call will be killed if the supervisor is shutdown or if the manager is removed.
  -}
  def monitorManager(ManagementInterface) :: Top

  def start(m) = if startAllowed? then m.start() else stop
  
  def startAll() = withLock(managerMapLock, { 
    map(start, managerMap.keys()) >> signal
  })
 
  site shutdownAll() = withLock(managerMapLock, {
    def f(m) = m.shutdown(killTime) ; signal -- If shutdown fails (due to it not currently running) just keep going
    map(f, managerMap.keys()) >> signal
  })
  
  site shutdown() = startAllowed := false >> finalShutdownSwitch.write(signal) >> shutdownAll() >> shutdownComplete.write(signal)
}

class OneForOneSupervisor extends SupervisorBase {
  val _ = startAll()
  def monitorManager(m) =
	m.haltedNotifier.newListener() >l> (signal |
    repeat(l.acquire) >> start(m) >> stop)
}

class AllForOneSupervisor extends SupervisorBase {  
  val _ = startAll()
  def monitorManager(m) =
	m.haltedNotifier.newListener() >l> (signal |
	repeat(l.acquire) >> restartNotifier.notify() >> stop)
  
  val restartNotifier = Notifier()
  val _ = {| repeat({ restartNotifier.wait() >> shutdownAll() >> startAll() }) >> stop | finalShutdownSwitch.read() |}
}

class def FrequencyCounter(period :: Integer) :: FrequencyCounter {
  val times = Ref[List[Integer]]([])
  val lock = Semaphore(1)
  site mark() = withLock(lock, {
    times := Rtime() : times? 
  })
  site count() = withLock(lock, {
    val cutoff = Rtime() - period
    times := filter({ _ :> cutoff }, times?) >>
    length(times?)
  })
}

class LimitStarts extends Supervisor {
  val startsLimit :: Integer
  val timePeriod :: Integer

  val startCounterMap = Map[ManagementInterface, FrequencyCounter]()
  site add(m) = startCounterMap.put(m, FrequencyCounter(timePeriod)) >> super.add(m)

  def start(m) = 
    val counter = startCounterMap(m)
    counter.mark() >> 
    (
      if counter.count() :> startsLimit then shutdown() else super.start(m)
    )
}

