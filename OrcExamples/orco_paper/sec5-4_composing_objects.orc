-- Runnable version of OrcO: A Concurrency-First Approach to Objects Figure 7--9

-- Utilities
import class ArrayList = "java.util.ArrayList" 
import class Collections = "java.util.Collections" 

class MutableList {
  val underlying = Collections.synchronizedList(ArrayList())
  def add(v) = underlying.add(v)
  def each() = repeat(IterableToStream(underlying))
}

-- Framework
val Logger = new {
  -- Actual log message formatting is left out.
  site submit(r) = Rwait(5000) >> (r.each() >l> Println("Log message") >> stop ; signal)
}

def TimeoutError() = "TIMEOUT"

class Query {
  val query
  val reply = Cell()
}
class Handler {
  val db
  val handle
  
  def sendQuery(q) =
    val msg = new Query { val query = q }
    db.query(msg) >> stop |
    msg.reply.read()
}
class Database {
  val queryChannel = Channel()
  def query(q) = queryChannel.put(q)
  val _ = repeat(queryChannel.get) >q>
          q.reply.write(doQuery(q))
  def doQuery(q) = new { val results = q.query }
}

-- Application
class HandlerImpl extends Handler {
  def extractQuery(r) = r.query
  def displayResults(r) = r.results
  def handle(r) =
        val res = sendQuery(extractQuery(r))
        displayResults(res)
}

-- Superposed computations
class LogRecord extends MutableList {
}
class LoggingQuery extends Query {
  val logRec
}
class LoggingDatabase extends Database {
  def doQuery(q) =
    q.logRec.add(q) |
    (val v = super.doQuery(q)
    q.logRec.add(v) >> stop | v)
}
class LoggingHandler extends Handler {
  val logRec = new LogRecord
  val _ = Logger.submit(logRec)
  def handle(r) =
    logRec.add(r) >> stop |
    (val v = super.handle(r)
    logRec.add(v) >> stop | v)
  def sendQuery(q) =
  	val outerLogRec = logRec
  	val msg = new LoggingQuery {
        val query = q
        val logRec = outerLogRec
    }
    db.query(msg) >> stop |
    msg.reply.read()
}

class Timeout extends Handler {
  def handle(r) = {|
    super.handle(r) |
    Rwait(10000) >> TimeoutError()
  |}
}

-- Compose and run it
class LoggingHandlerImpl extends HandlerImpl with Timeout with LoggingHandler {}
val database = new LoggingDatabase

def onRequest() = repeat({ 
    Prompt("Enter request:") >q>
    new {
      val query = q
      def onDisconnect() = stop
      def reply(v) = Println(v)
    }
  })

onRequest() >r> {|
  r.reply((new LoggingHandlerImpl {
      val db = database
    }).handle(r)) |
  r.onDisconnect()
|}

