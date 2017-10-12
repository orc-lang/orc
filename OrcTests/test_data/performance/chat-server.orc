{-
A chat server that stores a backlog of messages and sends them out to new clients on a given room.
-}

include "net.inc"

import class Queue = "java.util.concurrent.ConcurrentLinkedQueue"
import class Map = "java.util.concurrent.ConcurrentHashMap"

val orcPage = "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'><head><title>Orc Language</title></head><body><h2>Orc is ...</h2><p>... a novel language for distributed and concurrent programming which provides uniform access to computational services, including distributed communication and data manipulation, through <span class='highlight'>sites</span>. Using four simple concurrency primitives, the programmer <span class='highlight'>orchestrates</span> the invocation of sites to achieve a goal, while managing timeouts, priorities, and failures.</p></body></html>"

val roomPageP1 = "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>"+
  "<head><meta http-equiv='refresh' content='3'><title>Chat Room</title></head><body><h2>"
val roomPageP2 = "</h2><ul>"
val roomPageItemP1 = "<li>"
val roomPageItemP2 = "</li>"
val roomPageP3 = "</ul><br/><form action='"
val roomPageP4 = "'><input type='text' name='msg' autofocus /><input type='submit' value='Say' /></form></body></html>"

val rooms = Map()
val roomsLock = Semaphore(1)

def newRoom() = {. messages = Queue(), count = Counter(0) .}

val MAX_MESSAGES = 1000
  
-- Rwait to simulate a lookup time.
def getMessages(room) = Rwait(5) >> iterableToList(room.messages)

def addMessage(room, msg) = 
  (room.messages.add(msg), room.count.inc()) >>
  (if room.count.value() :> MAX_MESSAGES then
    (room.messages.poll(), room.count.dec() ; signal)
  else
    signal) >> signal

def getRoom(name) = 
  (if rooms.get(name) = null then
    val r = newRoom() #
    (rooms.putIfAbsent(name, r),
     addMessage(r, "Welcome to " + name)) >> signal
  else
    signal) >>
  rooms.get(name)
  

val server = ServletServer(8080)
val orcServlet = server.newServlet(["/orc"]) 
val chatServlet = server.newServlet(["/chat/*"]) #

repeat(orcServlet.get) >ctx> (
  val resp = ctx.getResponse()
  resp.setContentType("text/html") >>
  resp.getOutputStream().print(orcPage) >> 
  stop ;
  ctx.complete()
) >> stop |

repeat(chatServlet.get) >ctx> (
  val req = ctx.getRequest()
  val resp = ctx.getResponse()
  val out = resp.getOutputStream()
  val roomname = req.getPathInfo()
  val room = getRoom(roomname)

  val msg = req.getParameter("msg")
  
  def displayMode() = 
    val messages = getMessages(room)
    
    resp.setContentType("text/html") >>
    out.print(roomPageP1) >> 
    out.print(roomname) >> 
    out.print(roomPageP2) >> 
    out.print(
      afold(lambda(x, y) = x+y, 
            map(lambda(m) = roomPageItemP1 + m + roomPageItemP2, 
                messages))
    ) >>
    out.print(roomPageP3) >> 
    out.print(req.getRequestURL().toString()) >> 
    out.print(roomPageP4)
    
  def sayMode() = 
    addMessage(room, msg) >>
    resp.sendRedirect(req.getRequestURL().toString()) #
    
  (if msg = null || msg = "" then
    displayMode()
  else
    sayMode()) >> 
  
  stop ;
  ctx.complete()
) >> stop

