//
// PorcAST.scala -- Scala class/trait/object PorcAST

// Project OrcScala
//
// $Id$
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import orc.values.sites
import orc.Handle
import orc.OrcEvent
import orc.error.OrcException
import sun.security.util.DerValue
import orc.lib.str.PrintEvent
import orc.PublishedEvent
import orc.values.Format
import orc.values.OrcRecord

// Value is an alias for AnyRef. It exists to make this code more self documenting.

// ==================== Values ===================
case class Tuple(values: List[AnyRef]) extends Value {
  def arity = values.size
}
object Tuple {
  def apply(values: AnyRef*): Tuple = Tuple(values.toList)
}

case class Var(index: Int, optionalName: Option[String]) extends Value
// Cases which would be Var are instead just Int.

// ==================== CORE ===================

sealed abstract class Command {
  def eval(ctx: Context, interp: InterpreterContext): Unit
  
  final protected def dereferenceTuple(v: AnyRef, ctx: Context): List[AnyRef] = {
    val v1 = v match {
      case Var(i, _) => ctx(i)
      case v => v
    }
    v1 match {
      case Tuple(vs) => vs.toList.map(dereference(_, ctx))
      case _ =>
        throw new Error(s"Non-tuple in dereferenceTuple: $v1 from $v in $ctx")
    }
  }
  
  final protected def dereference(v: AnyRef, ctx: Context): AnyRef = v match {
    case t@Tuple(_) => Tuple(dereferenceTuple(t, ctx))
    case Var(i, _) => ctx(i)
    case v => v
  }

  final protected def invokeExternal(ctx: Context, interp: InterpreterContext, callable: AnyRef, arguments: List[AnyRef], pc: Closure, hc: Closure): Unit = {
    val t = ctx.terminator
    Logger.finer(s"Site call started: $callable $arguments")
    val handle = new Handle {
      def notifyOrc(event: OrcEvent) {
        event match {
          case PublishedEvent(v) => println(s"publication: ${Format.formatValue(v)}")
          case PrintEvent(s) => print(s)
          case _ =>
            Logger.info(event.toString)
        }
      }
    
      // FIXME: It would be much better to be able to make the call directly here. However 
      // that would require differentiating calls in the same thread and those in other threads.
      def publish(v: AnyRef) {
        Logger.finer(s"Site call published: $callable $arguments -> $v")
        InterpreterContext.current.schedule(pc, List(v, hc))
      }
      def halt {
        Logger.finer(s"Site call halted: $callable $arguments")
        InterpreterContext.current.schedule(hc)
      }
      
      def !!(e: OrcException) {
        Logger.severe(s"ORC EXCEPTION: $e")
      }
    
      def hasRight(rightName: String): Boolean = {
        true // FIXME: TEMPORARY HACK
      }
    
      def isLive: Boolean = {
        !t.isKilled
      }
    }
    
    interp.invoke(handle, callable, arguments)
  }

  final protected def forceFutures(interp: InterpreterContext, vs: List[AnyRef], bb: Closure, hb: Closure): Unit = {
    val fs = vs.zipWithIndex.collect{case (f:Future, i) => (i, f)}.toMap
    if(fs.isEmpty) {
      interp.schedule(bb, List(Tuple(vs)))
    } else {
      val j = new Join(fs) {
        def halt() {
          Logger.finer(s"Future halted: calling $hb")
          InterpreterContext.current.schedule(hb)
        }
        def bound(nvs: Map[Int, AnyRef]) {
          val args = vs.zipWithIndex.map { p =>
            nvs.getOrElse(p._2, p._1)
          }
          Logger.finer(s"Future bound: calling $bb ($args)")
          InterpreterContext.current.schedule(bb, List(Tuple(args)))
        }
      }
      for ((_, f) <- fs) {
        f.addBlocked(j)
      }
    }
  }
}

case class Let(d: ClosureDef, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val ctx1 = ctx.pushValues(List(Closure(d.body, ctx)))
    k.eval(ctx1, interp)
  }  
}
case class Site(defs: List[SiteDef], k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val closures = defs map { 
      case SiteDef(_, _, b) => Closure(b, null)
    }
    val ctx1:Context = ctx.pushValues(closures)
    for(c <- closures) {
      c.ctx = ctx1
    }
    k.eval(ctx1, interp)
  }  
}

case class SiteDef(name: Option[String], arity: Int, body: Command)
case class ClosureDef(name: Option[String], arity: Int, body: Command)

case class ClosureCall(target: Var, arguments: List[Value]) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val clos = dereference(target, ctx).asInstanceOf[Closure]
    Logger.finer(s"closcall: $target $arguments { $clos }" )
    val ctx1 = clos.ctx.pushValues(arguments.map(dereference(_, ctx)))
    clos.body.eval(ctx1, interp)
  }
}
case class SiteCall(target: Var, arguments: List[Value]) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    dereference(target, ctx) match {
      case clos : Closure =>
        Logger.finer(s"sitecall: $target $arguments { $clos }" )
        val ctx1 = clos.ctx.pushValues(arguments.map(dereference(_, ctx))).copy(terminator = ctx.terminator, counter = ctx.counter, oldCounter = ctx.oldCounter)
        clos.body.eval(ctx1, interp)
      /*case record: OrcRecord if record.entries.contains("apply") =>
        val v = record.entries("apply")
        Logger.finer(s"sitecall: $target $arguments { $v }" )
        val List(arg: Value, pc: Closure, hc: Closure) = arguments.map(dereference(_, ctx))
        
        val bb = Closure(ExternalCall(v, Var(0, None), 1, 2), Context(List(pc, hc), ctx.terminator, ctx.counter, ctx.oldCounter))
        val hb = hc
        
        val vs = dereferenceTuple(arg, ctx)
        forceFutures(interp, vs, bb, hb)*/
      case v =>
        Logger.finer(s"sitecall: $target $arguments { $v }" )
        val List(arg: Value, pc: Closure, hc: Closure) = arguments.map(dereference(_, ctx))
        
        val bb = Closure(ExternalCall(v, Var(0, None), 1, 2), Context(List(pc, hc), ctx.terminator, ctx.counter, ctx.oldCounter))
        val hb = hc
        
        val vs = dereferenceTuple(arg, ctx)
        forceFutures(interp, vs, bb, hb)
    }
  }
}

case class Unpack(arity: Int, v: Value, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val t = dereference(v, ctx).asInstanceOf[Tuple]
    assert(t.arity == arity)
    val ctx1 = ctx.pushValues(t.values.map(dereference(_, ctx)))
    k.eval(ctx1, interp)
  }
}

// ==================== PROCESS ===================

case class Spawn(target: Int, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val clos = ctx(target).asInstanceOf[Closure]
    Logger.finer(s"Spawning: $target ${ctx.counter} { $clos }")
    ctx.counter.increment()
    interp.schedule(clos, List(ctx.counter.haltHandler))
    k.eval(ctx, interp)
  }
}
case class Die() extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    /*val size = interp.queue.size
    Logger.finer(s"Process Died: $size")
    if(size == 0) {
      Logger.fine("We are done")
    }*/
  }
}

case class NewCounter(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val c = new Counter()
    ctx.counter.increment()
    k.eval(ctx.copy(counter = c, oldCounter = ctx.counter), interp)
  }
}
case class RestoreCounter(zeroBranch: Command, nonzeroBranch: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    if( ctx.counter.decrementAndTestZero() )
      zeroBranch.eval(ctx.copy(counter = ctx.oldCounter, oldCounter = null), interp)
    else
      nonzeroBranch.eval(ctx.copy(counter = null, oldCounter = null), interp)
  }
}
case class SetCounterHalt(haltCont: Int, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val clos = ctx(haltCont).asInstanceOf[Closure]
    ctx.counter.haltHandler = clos
    k.eval(ctx, interp)
  }  
}
case class GetCounterHalt(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    k.eval(ctx.pushValues(List(ctx.counter.haltHandler)), interp)
  }  
}
case class DecrCounter(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val iz = ctx.counter.decrementAndTestZero()
    assert(!iz)
    k.eval(ctx, interp)
  }
}

case class NewTerminator(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val t = new Terminator()
    k.eval(ctx.copy(terminator = t), interp)
  }
}
case class GetTerminator(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    k.eval(ctx.pushValues(List(ctx.terminator)), interp)
  }  
}
case class Kill(killedBranch: Command, alreadykilledBranch: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    if( ctx.terminator.setIsKilled() ) {
      Logger.finest(s"Killed terminator ${ctx.terminator}")
      killedBranch.eval(ctx, interp)
    } else {
      Logger.finest(s"Already killed terminator ${ctx.terminator}")
      alreadykilledBranch.eval(ctx, interp)
    }
  }
}
case class IsKilled(killedBranch: Command, notKilledBranch: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    if( ctx.terminator.isKilled )
      killedBranch.eval(ctx, interp)
    else
      notKilledBranch.eval(ctx, interp)
  }
}
case class AddKillHandler(u: Int, m: Int, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val t = ctx(u).asInstanceOf[Terminator]
    val kh = ctx(m).asInstanceOf[Closure]
    Logger.finest(s"Adding kill handler: ${t} ${kh}")
    t.addKillHandler(kh)
    k.eval(ctx, interp)
  }
}
case class CallKillHandlers(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    Logger.finest(s"Calling kill handlers: ${ctx.terminator} ${ctx.terminator.killHandlers}")
    //ctx.terminator.killHandlers.foreach(interp.schedule(_))
    ctx.terminator.killHandlers.foreach(c => c.body.eval(c.ctx, interp))
    k.eval(ctx, interp)
  }
}

// ==================== FUTURE ===================

case class NewFuture(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    k.eval(ctx.pushValues(List(new Future())), interp)
  }  
}
case class Force(futures: Value, boundBranch: Int, haltedBranch: Int) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val vs = dereferenceTuple(futures, ctx)
    val hb = ctx(haltedBranch).asInstanceOf[Closure]
    val bb = ctx(boundBranch).asInstanceOf[Closure]
    forceFutures(interp, vs, bb, hb)
  }  
}
case class Bind(future: Int, value: Value, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    ctx(future).asInstanceOf[Future].bind(dereference(value, ctx))
    k.eval(ctx, interp)
  }  
}
case class Stop(future: Int, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    ctx(future).asInstanceOf[Future].halt()
    k.eval(ctx, interp)
  }  
}

// ==================== FLAG ===================

case class NewFlag(k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    k.eval(ctx.pushValues(List(new Flag())), interp)
  }  
}
case class SetFlag(flag: Int, k: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    ctx(flag).asInstanceOf[Flag].set()
    k.eval(ctx, interp)
  }  
}
case class ReadFlag(flag: Int, trueBranch: Command, falseBranch: Command) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val f = ctx(flag).asInstanceOf[Flag]
    if( f.get )
      trueBranch.eval(ctx, interp)
    else
      falseBranch.eval(ctx, interp)
  }  
}

// ==================== EXT ====================

case class ExternalCall(site: AnyRef, arguments: Value, p: Int, h: Int) extends Command {
  def eval(ctx: Context, interp: InterpreterContext) {
    val pc = ctx(p).asInstanceOf[Closure]
    val hc = ctx(h).asInstanceOf[Closure]
    invokeExternal(ctx, interp, site, dereferenceTuple(arguments, ctx), pc, hc)
  }
}
