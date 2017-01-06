import class ArrayList = "java.util.ArrayList"

include "gui.inc"

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
  
class MonitorLock {
  val lock = Semaphore(1)
  def synchronized[T](f :: lambda() :: T) = 
	  acquireReleaseOnKill(lock.acquire, lock.release) >earlyRelease> (
	    val r = f() #
	    (r ; signal) >> earlyRelease() >> r
	  )
  def synchronizedUntilHalt[T](f :: lambda() :: T) = 
	  acquireReleaseOnKill(lock.acquire, lock.release) >earlyRelease> (
	    val r = Channel() #
	    (f() >v> r.put(v) >> stop ; r.close() | earlyRelease()) >> stop |
	    repeat(r.get)
	  )
}

def randomSleep() = 
  Rwait(Random(5000))

class MutableList extends MonitorLock {
  val data = ArrayList()
  
  def add(v) = synchronized({ data.add(v) })
  def each() = synchronizedUntilHalt({
    val iter = data.iterator()
    repeat({ iter.hasNext() >true> iter.next() })
  })
}

class GUI {
  val searchEntry = TextField()
  val searchButton = Button("Search")
  
  val resultsList = ListComponent()
  
  val frame = Frame([searchEntry, searchButton, resultsList])
  val _ = resultsList.add("Test") | searchButton.setEnabled(false) >> db.completed >> searchButton.setEnabled(true)
  
  val db :: Database
    
  def addResult(r) = resultsList.add(r)
  
  val _ = repeat({
    {| searchButton.onAction() | searchEntry.onAction() |} >> 
    searchButton.setEnabled(false) >>
    resultsList.clear() >> 
    db.search(".*" + searchEntry.getText() + ".*") >r> addResult(r) >> stop ;
    searchButton.setEnabled(true)
  })
  
  def onExit() = frame.onClosing()
}

class Result {
  def toString() = name
  
  val name :: String
}
def Result(n :: String) :: Result = new Result { val name = n }

class Database {
  val data = new MutableList
  val completed = loadData() >s> data.add(s) >> stop ; signal
  def loadData() = "test1" | "ABC" | "ab"
  
  def search(query) = completed >> data.each() >s> s.matches(query) >true> Result(s) 
}

class DatabaseSlow extends Database {
  def loadData() = randomSleep() >> super.loadData() >s> randomSleep() >> s
  
  def search(query) = completed >> data.each() >s> randomSleep() >> s.matches(query) >true> (Println(s) >> stop | Result(s))
}


class GUIRating extends GUI {
  def addResult(r) = super.addResult(r) | r.rating >> resultsList.elementsUpdated()
}

class ResultRating extends Result {
  val rating :: Integer
  -- Vtime would be useful here, I think.
  def toString() = {| name + ", Rating: " + rating | Rwait(10) >> super.toString() |}
}

class DatabaseRating extends DatabaseSlow {
  def search(query) = completed >> data.each() >s> randomSleep() >> s.matches(query) >true> new ResultRating {
	val rating = randomSleep() >> Random(100)
	val name = s
  }
}

{|
val gui = new GUIRating {
  val db = new DatabaseRating
}
gui.onExit()
|} >> stop
