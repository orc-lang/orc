package orc.scalabenchmarks

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.HttpServlet
import orc.lib.net.ServletServer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object ChatServer {
  val orcPage = "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'><head><title>Orc Language</title></head><body><h2>Orc is ...</h2><p>... a novel language for distributed and concurrent programming which provides uniform access to computational services, including distributed communication and data manipulation, through <span class='highlight'>sites</span>. Using four simple concurrency primitives, the programmer <span class='highlight'>orchestrates</span> the invocation of sites to achieve a goal, while managing timeouts, priorities, and failures.</p></body></html>"

  val roomPageP1 = "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>" +
    "<head><meta http-equiv='refresh' content='3'><title>Chat Room</title></head><body><h2>"
  val roomPageP2 = "</h2><ul>"
  val roomPageItemP1 = "<li>"
  val roomPageItemP2 = "</li>"
  val roomPageP3 = "</ul><br/><form action='"
  val roomPageP4 = "'><input type='text' name='msg' autofocus /><input type='submit' value='Say' /></form></body></html>"

  val MAX_MESSAGES = 1000

  class Room() {
    private val _messages = new ConcurrentLinkedQueue[String]()
    private var count = new AtomicInteger(0)

    def messages: Seq[String] = {
      Thread.sleep(5)
      _messages.toSeq
    }

    def addMessage(msg: String): Unit = {
      _messages.add(msg)
      val n = count.incrementAndGet()
      if (n > MAX_MESSAGES) {
        _messages.poll()
        count.decrementAndGet()
      }
    }
  }

  val rooms = new ConcurrentHashMap[String, Room]()

  def getRoom(name: String) = {
    synchronized {
      if (rooms.get(name) == null) {
        val r = new Room()
        r.addMessage(s"Welcome to $name")
        rooms.put(name, r)
      }
    }
    rooms.get(name)
  }

  private class OrcPage extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
      resp.setContentType("text/html")
      resp.getOutputStream().print(orcPage)
    }
  }

  private class ChatPage extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
      val out = resp.getOutputStream()
      val roomname = req.getPathInfo()
      val room = getRoom(roomname)
      val msg = req.getParameter("msg")

      if (msg == null || msg == "") {
        val messages = room.messages
        resp.setContentType("text/html")
        out.print(roomPageP1)
        out.print(roomname)
        out.print(roomPageP2)
        out.print(messages.map(roomPageItemP1 + _ + roomPageItemP2).mkString(""))
        out.print(roomPageP3)
        out.print(req.getRequestURL().toString())
        out.print(roomPageP4)
      } else {
        room.addMessage(msg)
        resp.sendRedirect(req.getRequestURL().toString())
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val server = ServletServer(8080)

    server.addServlet(new OrcPage(), Seq("/orc"))
    server.addServlet(new ChatPage(), Seq("/chat/*"))

    server.server.join()
  }

}
