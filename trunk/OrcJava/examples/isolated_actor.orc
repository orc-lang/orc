{-
Create a new actor with two methods:
- do(task) executes the thunk task asynchronously.
  An actor only executes one task at a time, providing
  mutual exclusion.
- stop() halts the actor.
-}
def Actor() =
  val m = Buffer() -- the mailbox
  -- the message loop
  def loop() =
    m.get() >f> ( f() >> stop ; loop() )
  -- start the message loop and return
  -- a function used to send messages
  isolated (loop() | m.put)

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
-- start two tasks
( a(task1)
| Rtimer(10) >> a(task2) ) >>
stop
{-
OUTPUT:
hi there bob
-}