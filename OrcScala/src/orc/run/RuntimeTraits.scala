package orc.run

import orc.values.sites.Site
import orc.values.OrcValue
import orc.error.OrcException
import orc.values.sites.compatibility.ArrayProxy
import orc.values.sites.JavaObjectProxy
import orc.error.runtime.JavaException
import orc.error.runtime.UncallableValueException
import orc.TokenAPI
import orc.values.Format
import orc.oil.nameless.Expression
import orc.OrcRuntime
import orc.OrcEvent
import orc.Publication
import orc.Halted

trait InvocationBehavior extends OrcRuntime {
  /* By default, an invocation halts silently. This will be overridden by other traits. */
  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit = { t.halt }
}


trait ErrorOnUndefinedInvocation extends InvocationBehavior {
  /* This replaces the default behavior because it does not call super */
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) {
    val error = "You can't call the "+v.getClass().getName()+" \" "+Format.formatValue(v)+" \""
    t !! new UncallableValueException(error)
  }
}


trait SupportForJavaObjectInvocation extends InvocationBehavior {
  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case v : OrcValue => super.invoke(t, v, vs)
      case obj => invoke(t, JavaObjectProxy(obj), vs)
    }
  }

}


trait SupportForJavaArrayAccess extends InvocationBehavior {
  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case l : Array[Any] => invoke(t, JavaObjectProxy(new ArrayProxy(l)), vs)
      case other => super.invoke(t, other, vs)
    }
  }

}
  

trait SupportForSiteInvocation extends InvocationBehavior {  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) {
    v match {
      case (s: Site) => 
        try {
          s.call(vs, t)
        }
        catch {
          case e: OrcException => t !! e
          case e: Exception => t !! new JavaException(e)
        }
      case _ => super.invoke(t, v, vs)
    }
  }
}



import scala.actors.Actor
import scala.actors.Actor._
  
trait ActorBasedScheduler extends Orc {
  
  val worker = new Worker()
  worker.start
  
  override def schedule(ts: List[Token]) { for (t <- ts) worker ! Some(t) }
  
  /* Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  override def stop = { worker ! None ; super.stop }
  
  class Worker extends Actor {
    def act() {
      loop {
        react {
          case Some(x:Token) => x.run
          // machine has stopped
          case None => exit
        }
      }
    }
  }
  
}

/* The first behavior in the trait list will be tried last */
trait StandardInvocationSemantics extends InvocationBehavior
with ErrorOnUndefinedInvocation
with SupportForSiteInvocation
with SupportForJavaObjectInvocation
with SupportForJavaArrayAccess

  
class StandardOrcRuntime extends OrcRuntime
with Orc
with StandardInvocationSemantics
with ActorBasedScheduler
with SupportForCapsules
with SupportForSynchronousExecution
with SupportForRtimer
with SupportForStdout
with ExceptionReportingOnConsole



trait ExceptionReportingOnConsole extends OrcRuntime {
  def caught(e: Throwable) { 
    e match {
      case (ex: OrcException) => println(ex.getPosition().longString) 
    }
    e.printStackTrace() 
  }
}

trait SupportForRtimer extends Orc {
  
  val timer: java.util.Timer = new java.util.Timer()
  
  def getTimer() = timer
  
  override def stop = { timer.cancel() ; super.stop }
  
}


trait SupportForStdout extends OrcRuntime {
  def printToStdout(s: String) { print(s) }
}


trait SupportForCapsules extends Orc {
  
  def runEncapsulated(node: Expression, caller: Token) = {
    val host = caller.group.root
    val exec = new CapsuleExecution(caller, host)
    val t = new Token(node, exec)
    schedule(t)
  }
  
  class CapsuleExecution(caller: Token, host: Group) extends Subgroup(host) {
    
    var listener: Option[Token] = Some(caller)
    
    override def publish(t: Token, v: AnyRef) { 
      listener match {
        case Some(l) => {
          listener = None
          l.publish(v)
        }
        case None => { } 
      }
      t.halt
    }
    
    override def onHalt {
      listener match {
        case Some(l) => {
          listener = None
          l.halt
        }
        case None => { } 
      }
      host.remove(this)
    }
    
  }
  
}


trait SupportForSynchronousExecution extends OrcRuntime {
  
  /* Wait for execution to complete, rather than dispatching asynchronously.
   * The continuation takes only values, not events.
   */
  def runSynchronous(node: Expression, k: AnyRef => Unit) {
    val done: scala.concurrent.SyncVar[Unit] = new scala.concurrent.SyncVar()
    def ksync(event: OrcEvent): Unit = {
      event match {
        case Publication(v) => k(v)
        case Halted => { done.set({}) }
      }
    }
    this.run(node, ksync)
    done.get
  }
  
    /* If no continuation is given, discard published values and run silently to completion. */
  def runSynchronous(node: Expression) {
    runSynchronous(node, { v: AnyRef => { /* suppress publications */ } })
  }
 
}


