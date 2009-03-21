{-
Create a new actor with two methods:
- do(task) executes the thunk task asynchronously.
  An actor only executes one task at a time, providing
  mutual exclusion.
- stop() halts the actor.
-}
def Actor() =
  type Message = Do(_) | Stop()
  val m = Buffer() -- the mailbox
  def dof(f) = m.put(Do(f))
  def stopf() = m.put(Stop())
  -- the message loop
  def loop() =
    def case(Do(f)) = f() >> stop ; loop()
    def case(Stop()) = stop
    case(m.get())
  -- start the message loop and return an object
  -- with do and stop methods
  isolated (loop() | Record("do", dof, "stop", stopf))

-- create a new actor;
-- note that even though we use val,
-- the actor's message loop does not terminate
val a = Actor()
-- first task
def task1() = 
  print("hi ") >>
  Rtimer(50) >>
  print("there ")
-- second task
def task2() = println("bob")
-- start two tasks and then tell the actor to stop
( a.do(task1)
| Rtimer(10) >> a.do(task2)
| Rtimer(20) >> a.stop()) >>
stop
{-
OUTPUT:
hi there bob
-}