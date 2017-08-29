//
// ServletServer.scala -- Servlet server API for Orc
// Project OrcSites
//
// Created by amp on Oct, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net

import java.util.concurrent.ConcurrentLinkedQueue

import scala.reflect.ClassTag

import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException, NoSuchMemberException, UncallableValueException }
import orc.values.{ Field, HasMembers, Signal }
import orc.values.sites.{ Site, Site0, TotalSite, TotalSite0, TotalSite1 }

import javax.servlet.{ AsyncContext, Servlet }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ ServletContextHandler, ServletHolder, ServletMapping }
import org.eclipse.jetty.util.component.LifeCycle

trait CastArgumentSupport {
  def castArgument[T <: AnyRef: ClassTag](i: Int, a: AnyRef, typeName: String = null): T = {
    val tT = implicitly[ClassTag[T]]
    if (tT.runtimeClass.isInstance(a))
      a.asInstanceOf[T]
    else {
      val desiredTypeName = Option(typeName).getOrElse(tT.toString())
      val providedTypeName = Option(a).map(_.getClass().toString()).getOrElse("null")
      throw new ArgumentTypeMismatchException(i, desiredTypeName, providedTypeName)
    }
  }
}

class ServletWrapper(val servlet: ServletHolder, val server: ServletServerWrapper) extends HttpServlet with Site with HasMembers {
  val requestQueue = new ConcurrentLinkedQueue[AsyncContext]()
  val getQueue = new ConcurrentLinkedQueue[orc.CallContext]()

  /** Call to process one request if possible.
    *
    * This is called once from each service and get. This is enough
    * since those are the only places a single item could be added
    * to either list.
    */
  private def process(): Unit = synchronized {
    val handle = {
      var h = getQueue.peek()
      while (h != null && !h.isLive) {
        // Reject and remove any killed get calls.
        getQueue.remove(h)
        h = getQueue.peek()
      }
      h
    }
    val request = requestQueue.peek()
    if (handle != null && request != null) {
      getQueue.remove(handle)
      requestQueue.remove(request)

      handle.publish(request)
    }
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val ctx = req.startAsync()
    requestQueue.add(ctx)
    process()
  }

  object GetSite extends Site0 {
    def call(h: orc.CallContext): Unit = {
      getQueue.add(h)
      process()
    }
  }

  object JoinSite extends Site0 {
    def call(callContext: orc.CallContext): Unit = {
      servlet.addLifeCycleListener(new LifeCycle.Listener {
        def lifeCycleFailure(l: LifeCycle, e: Throwable): Unit = {}
        def lifeCycleStarted(l: LifeCycle): Unit = {}
        def lifeCycleStarting(l: LifeCycle): Unit = {}
        def lifeCycleStopped(l: LifeCycle): Unit = callContext.publish()
        def lifeCycleStopping(l: LifeCycle): Unit = {}
      })
    }
  }

  object StopSite extends TotalSite0 {
    def eval(): AnyRef = {
      server.removeServletHolder(servlet)
      Signal
    }
  }

  val fields = Map(
    Field("registration") -> servlet.getRegistration(),
    Field("server") -> server.server,
    Field("context") -> server.context,
    Field("get") -> GetSite,
    Field("join") -> JoinSite,
    Field("stop") -> StopSite)

  def getMember(f: Field) = {
    fields.get(f).getOrElse(throw new NoSuchMemberException(this, f.name))
  }

  override def hasMember(f: Field): Boolean = {
    fields contains f
  }

  def call(args: Array[AnyRef], h: orc.CallContext): Unit = {
    h !! new UncallableValueException("This site is not callable, but has fields.")
  }
}

class ServletServerWrapper(val server: Server, val context: ServletContextHandler) extends Site with HasMembers {
  private[this] var nextId = 0
  private def genId() = synchronized {
    nextId += 1
    nextId
  }

  def addServlet(servlet: Servlet, paths: Seq[String]): ServletHolder = synchronized {
    val h = new ServletHolder(servlet)
    h.setName(s"$servlet##${genId()}")
    h.setAsyncSupported(true)
    context.getServletHandler().addServlet(h)
    val mapping = new ServletMapping
    mapping.setPathSpecs(paths.toArray)
    mapping.setServletName(h.getName())
    context.getServletHandler().addServletMapping(mapping)
    h
  }

  def newServlet(paths: Seq[String]): ServletWrapper = synchronized {
    val h = new ServletHolder()
    val servlet = new ServletWrapper(h, this)
    h.setServlet(servlet)
    h.setName(s"$servlet##${genId()}")
    h.setAsyncSupported(true)
    context.getServletHandler().addServlet(h)
    val mapping = new ServletMapping
    mapping.setPathSpecs(paths.toArray)
    mapping.setServletName(h.getName())
    context.getServletHandler().addServletMapping(mapping)
    servlet
  }

  object NewServletSite extends TotalSite1 with CastArgumentSupport {
    def eval(a: AnyRef): AnyRef = {
      val paths = castArgument[List[String]](0, a)
      newServlet(paths)
    }
  }

  def removeServletHolder(servlet: ServletHolder) = synchronized {
    val handler = context.getServletHandler()

    val remainingServlets = for (s <- handler.getServlets() if s.getName != servlet.getName) yield s
    val remainingMappings = for (m <- handler.getServletMappings() if m.getServletName != servlet.getName) yield m

    handler.setServlets(remainingServlets)
    handler.setServletMappings(remainingMappings)

    servlet.stop()
  }

  object JoinSite extends Site0 {
    def call(callContext: orc.CallContext): Unit = ServletServerWrapper.this synchronized {
      server.addLifeCycleListener(new LifeCycle.Listener {
        def lifeCycleFailure(l: LifeCycle, e: Throwable): Unit = {}
        def lifeCycleStarted(l: LifeCycle): Unit = {}
        def lifeCycleStarting(l: LifeCycle): Unit = {}
        def lifeCycleStopped(l: LifeCycle): Unit = callContext.publish()
        def lifeCycleStopping(l: LifeCycle): Unit = {}
      })
    }
  }

  object StopSite extends TotalSite0 {
    def eval(): AnyRef = ServletServerWrapper.this synchronized {
      server.stop()
      Signal
    }
  }

  val fields = Map(
    Field("server") -> server,
    Field("context") -> context,
    Field("newServlet") -> NewServletSite,
    Field("join") -> JoinSite,
    Field("stop") -> StopSite)

  def getMember(f: Field) = {
    fields.get(f).getOrElse(throw new NoSuchMemberException(this, f.name))
  }

  override def hasMember(f: Field): Boolean = {
    fields contains f
  }

  def call(args: Array[AnyRef], h: orc.CallContext): Unit = {
    h !! new UncallableValueException("This site is not callable, but has fields.")
  }
}

object ServletServer extends TotalSite with CastArgumentSupport {
  def evaluate(args: Array[AnyRef]): AnyRef = {
    val port = args match {
      case Array(a) => {
        val p = castArgument[Number](0, a)
        p.intValue()
      }
      case _ => throw new ArityMismatchException(1, args.size)
    }
    ServletServer(port)
  }

  def apply(port: Int) = {
    val server = new Server(port)
    val context = new ServletContextHandler()
    context.setContextPath("/")
    server.setHandler(context)
    server.start()
    new ServletServerWrapper(server, context)
  }

  /** Servlet for testing */
  private class EmbeddedAsyncServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
      val ctxt = req.startAsync();
      ctxt.start(new Runnable() {
        def run(): Unit = {
          System.err.println("In AsyncContext / Start / Runnable / run");
          ctxt.getResponse().getOutputStream().println("In AsyncContext / Start / Runnable / run")
          ctxt.complete()
        }
      })
    }
  }

  /** Main for testing */
  def main(args: Array[String]): Unit = {
    val w = ServletServer(8080)

    Thread.sleep(5000)

    println("Add servlet")
    val h = w.addServlet(new EmbeddedAsyncServlet(), Seq("/async"))
    println("Servlet added")

    Thread.sleep(5000)

    println("Removing servlet")
    w.removeServletHolder(h)
    println("Servlet removed")

    w.server.join()
  }
}
