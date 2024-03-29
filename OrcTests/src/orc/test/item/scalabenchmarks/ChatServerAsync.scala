//
// ChatServerAsync.scala -- Scala benchmark servlet ChatServerAsync
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.util.concurrent.{ ConcurrentHashMap, ConcurrentLinkedQueue }
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

import orc.lib.net.ServletServer

import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

object ChatServerAsync {
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
    private val count = new AtomicInteger(0)

    def messages: Seq[String] = {
      Thread.sleep(5)
      _messages.asScala.toSeq
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
      val ctxt = req.startAsync();
      ctxt.start(new Runnable() {
        def run(): Unit = {
          resp.setContentType("text/html")
          resp.getOutputStream().print(orcPage)
          ctxt.complete()
        }
      })
    }
  }

  private class ChatPage extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
      val ctxt = req.startAsync();
      ctxt.start(new Runnable() {
        def run(): Unit = {
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
          ctxt.complete()
        }
      })
    }
  }

  def main(args: Array[String]): Unit = {
    val server = ServletServer(8080)

    server.addServlet(new OrcPage(), Seq("/orc"))
    server.addServlet(new ChatPage(), Seq("/chat/*"))

    server.server.join()
  }

}
