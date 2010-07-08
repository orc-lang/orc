package orc.run

import orc.values.sites.Site
import orc.values.OrcValue
import orc.error.OrcException
import orc.values.sites.compatibility.ArrayProxy
import orc.values.sites.JavaObjectProxy
import orc.error.runtime.JavaException
import orc.error.runtime.UncallableValueException
import orc.TokenAPI
import orc.OrcExecutionAPI
import orc.values.Format
import orc.oil.nameless.Expression


trait InvocationBehavior extends OrcExecutionAPI {
  /* By default, an invocation halts silently. This will be overridden by other traits. */
  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit = { t.halt }
}


trait DefaultInvocationRaisesError extends InvocationBehavior {
  /* This replaces the default behavior because it does not call super */
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) {
    val error = "You can't call the "+v.getClass().getName()+" \" "+Format.formatValue(v)+" \""
    t !! new UncallableValueException(error)
  }
}


// TODO: Move functionality of JavaObjectProxy class into this trait
trait JavaObjectInvocation extends InvocationBehavior {
  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case v : OrcValue => super.invoke(t, v, vs) // TODO: Make this orcvalue/javavalue distinction clearer
      case l : Array[Any] => invoke(t, JavaObjectProxy(new ArrayProxy(l)), vs)
      case obj => invoke(t, JavaObjectProxy(obj), vs)
   }
  }

}
  

trait SiteInvocation extends InvocationBehavior {  
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
  
trait ActorScheduler extends Orc {
  val worker = new Worker()
  worker.start
  
  override def schedule(ts: List[Token]) { for (t <- ts) worker ! Some(t) }
  
  def stop = worker ! None
  
  class Worker extends Actor {
    def act() {
      loop {
        react {
          case Some(x:Token) => x.run
          // machine has stopped
          case None => {
            timer.cancel()
            exit 
          }
        }
      }
    }
  }
}

/* The first behavior in the trait list will be tried last */
trait StandardOrcInvoke extends InvocationBehavior
with DefaultInvocationRaisesError
with SiteInvocation
with JavaObjectInvocation

  
class StandardOrcExecution extends Orc 
with StandardOrcInvoke
with ActorScheduler
with SupportForCapsules
{
  def expressionPrinted(s: String) { print(s) }
  def caught(e: Throwable) { 
    e match {
      case (ex: OrcException) => println(ex.getPosition().longString) 
    }
    e.printStackTrace() 
  }
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