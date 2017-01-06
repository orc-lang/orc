-- active_objects.orc -- Building an active object using Orc Objects
-- Runnable version of OrcO: A Concurrency-First Approach to Objects Figure 6
-- A more advanced version is in objects/active_objects.orc
 
import class LinkedBlockingQueue = "java.util.concurrent.LinkedBlockingQueue"

class def SequentialExecutor() {
  val queue = LinkedBlockingQueue()
  
  def schedule(f) = queue.put(f)
  
  val _ = repeat({
    val f = queue.take()
    f() >> stop ; signal
  })
}

class ActiveObjectBase {
  val exec = SequentialExecutor()

  def scheduleMethod(f) =
    val c = Channel()
    exec.schedule({
      f() >x> c.put(x) >> stop ; c.close()
    }) >> stop | repeat(c.get)
}
-- A variant of ActiveObjectBase with multiple executors would allow methods to be grouped based on conflicts.

class def ActiveObject() extends ActiveObjectBase {
  val v = Ref(0)
  def read() = v.read()
  def incr(x) = scheduleMethod({ v.write(v.read() + x) })
}


{|
val o = ActiveObject()
val N = 100

upto(N) >x> o.incr(x) >> stop |
upto(N*3) >> o.read() >> stop |
Rwait(3000) >> o.read()
|}

{-
OUTPUT:
4950
-}
