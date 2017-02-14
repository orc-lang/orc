{- active_objects.orc -- Building an active object using Orc Objects
 -
 - Created by amp on Feb 20, 2015 3:22:13 PM
 -}
 
import class LinkedBlockingQueue = "java.util.concurrent.LinkedBlockingQueue"

class SequentialExecutor {
  val queue = LinkedBlockingQueue[lambda () :: Top]()
  val halted = Ref(false)
  
  def schedule(f :: lambda () :: Top) = queue.put(f)
  
  def halt() = schedule({ halted := true }) >> signal
  
  val _ = repeat({
      queue.take()() >> stop ; Iff(halted?)
    })
}

class ActiveObjectBase {
  val exec = new SequentialExecutor

  def scheduleMethod[A](f :: lambda () :: A) = 
    val c = Cell[A]()
    exec.schedule({ c.write(f()) }) >> stop |
    c
  
  val halt = exec.halt 
}
-- A variant of ActiveObjectBase with multiple executors would allow methods to be grouped based on conflicts.

class ActiveObject extends ActiveObjectBase {
  val a = Ref[Integer](0)
  val b = Ref[Integer](0)
  val c = Ref[Integer](0)

  def read() = scheduleMethod({ (a?, b?, c?) })
  
  def incr() = scheduleMethod({ (a := a? + 1, b := b? + 1, c := c? + 1) >> signal })
}

val o = new ActiveObject
val N = 100

upto(N) >> o.incr() >> stop |
upto(N) >> o.read()? >(a, b, c) as t> (if a = b && b = c then stop else Println("Fail " + t)) |
Rwait(1000) >> o.halt()

{-
OUTPUT:
signal
-}
