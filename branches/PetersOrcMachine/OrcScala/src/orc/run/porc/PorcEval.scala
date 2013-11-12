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
import orc.run.extensions.RwaitEvent
import orc.values.sites.HaltedException
import orc.values.sites.DirectSite
import scala.util.parsing.input.NoPosition
import orc.CaughtEvent
import orc.error.runtime.JavaException

// TODO: Read changes and try to recreate last clean version.
// TODO: Implement debug information for PorcEval

// Value is an alias for AnyRef. It exists to make this code more self documenting.

class KilledException extends RuntimeException("Group killed")

object KilledException extends KilledException

// ==================== Values ===================
case class Var(index: Int) extends Value {
  def identityHashCode = System.identityHashCode(this)  
}
//case object Unit

// Scala's values are used for others: true, false

// ==================== CORE ===================

sealed abstract class Expr {
  def identityHashCode = System.identityHashCode(this)
  
  def prettyprint() = (new PrettyPrint()).reduce(this)
  def prettyprint(i: InterpreterContext) = (new PrettyPrint(i.engine.debugTable)).reduce(this)
  override def toString() = prettyprint()

  def eval(ctx: Context, interp: InterpreterContext): Value

  val HaltedException = new HaltedException()

  final protected def dereference(v: AnyRef, ctx: Context): AnyRef = v match {
    case Var(i) => ctx(i)
    case v => v
  }

  final protected def dereferenceSeq(vs: List[Value], ctx: Context): List[Value] = vs map { x => dereference(x, ctx) }

  /*
  sealed trait CallResult
  final case class CallValue(v: AnyRef) extends CallResult
  final case object CallHalt extends CallResult
  final case object CallNotImmediate extends CallResult
  */

  // TODO: Implement notifying site calls when they are killed. I need to register a special kill handler.

  final protected def invokeExternal(callable: AnyRef, arguments: List[AnyRef], pc: Closure, ctx: Context, interp: InterpreterContext): Unit = {
    val t = ctx.terminator
    val hc = ctx.counter.haltHandler
    //var result: CallResult = CallNotImmediate
    //val startingThread = Thread.currentThread
    ctx.counter.increment() // We are spawning a process effectively

    val handle = new PorcHandle(pc, hc, t, ctx, interp)
    Logger.finer(s"Site call started: $callable $arguments   $handle")

    if (t.addTerminable(handle)) {
      interp.invoke(handle, callable, arguments)
    } else {
      Logger.finer(s"Site call killed immediately. $this")
      handle.kill()
    }

    /*
    result match {
      case CallValue(v) =>
        val ctx1 = pc.ctx.pushValues(List(v, hc))
        pc.body.eval(ctx1, interp)
      case CallHalt =>
        hc.body.eval(hc.ctx, interp)
      case CallNotImmediate => 
        ()
    }
    */
  }

  final protected def forceFutures(vs: List[Value], bb: Closure, ctx: Context, interp: InterpreterContext): Unit = {
    val fs = vs.zipWithIndex.collect { case (f: Future, i) => (i, f) }.toMap
    if (fs.isEmpty) {
      Call(bb, vs).eval(ctx, interp)
    } else {
      ctx.counter.increment() // We are now spawning something that will call the halt handler
      val hb = ctx.counter.haltHandler
      val j = new Join(fs, ctx.terminator) {
        def halt() {
          Logger.finer(s"Future halted: calling $hb")
          InterpreterContext.current(interp).schedule(hb)
        }
        def bound(nvs: Map[Int, AnyRef]) {
          val args = vs.zipWithIndex.map { p =>
            nvs.getOrElse(p._2, p._1)
          }
          Logger.finer(s"Future bound: calling $bb ($args)")
          InterpreterContext.current(interp).schedule(bb, args, halt = hb)
        }
      }
      Logger.finer(s"force started: $j")
      if (ctx.terminator.addTerminable(j)) {
        for ((_, f) <- fs) {
          f.addBlocked(j)
        }
      } else {
        Logger.finer(s"Future killed immediately: calling $hb")
        interp.schedule(hb)        
      }
    }
  }
}

case class ValueExpr(v: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    v
  }
}

case class Call(target: Value, arguments: List[Value]) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val clos = dereference(target, ctx).asInstanceOf[Closure]
    Logger.finer(s"closcall: $target $arguments { ${clos} }: ${prettyprint(interp)}")
    val ctx1 = clos.ctx.pushValues(arguments.map(dereference(_, ctx)))
    clos.body.eval(ctx1, interp)
  }
}
case class Let(v: Expr, body: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val ctx1 = ctx.pushValue(v.eval(ctx, interp))
    body.eval(ctx1, interp)
  }
}

case class Sequence(es: List[Expr]) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    assert(es.size != 0)
    var v: Value = null
    for (e <- es) {
      v = e.eval(ctx, interp)
    }
    v
  }
}

case class Lambda(arity: Int, body: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Closure(body, ctx)
  }
}

case class Site(defs: List[SiteDef], body: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val closures = defs map {
      case SiteDef(_, _, b) => Closure(b, null)
    }
    val ctx1: Context = ctx.pushValues(closures)
    for (c <- closures) {
      c.ctx = ctx1
    }
    body.eval(ctx1, interp)
  }
}

case class SiteDef(name: Option[String], arity: Int, body: Expr)

case class SiteCall(target: Value, arguments: List[Value], pArg: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    // TODO: The 2 choices here should be encoded in Porc directly to allow optimization. Closure calls and bare site calls are quite different.
    dereference(target, ctx) match {
      case clos: Closure =>
        Logger.finer(s"sitecall: $target $arguments { $clos }")
        val ctx1 = clos.ctx.pushValues(arguments.map(dereference(_, ctx)))
          .pushValue(dereference(pArg, ctx))
          .copy(terminator = ctx.terminator, counter = ctx.counter, oldCounter = ctx.oldCounter)
        clos.body.eval(ctx1, interp)
      case v =>
        Logger.finer(s"sitecall: $target $arguments { $v }: ${prettyprint(interp)}")
        val args = (0 until arguments.size) map { x => Var(x) } toList
        val bb = Closure(ExternalCall(v, args, Var(args.size)),
          ctx.copy(valueStack = List(dereference(pArg, ctx))))

        val vs = dereferenceSeq(arguments, ctx)
        forceFutures(vs, bb, ctx, interp)
        Unit
    }
  }
}

case class DirectSiteCall(target: Value, arguments: List[Value]) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val s = target.asInstanceOf[DirectSite]
    Logger.finer(s"directsitecall: $target $arguments")
    try {
      s.directcall(dereferenceSeq(arguments, ctx))
    } catch {
      case e: HaltedException => throw e
      case e: OrcException => {
        ctx.eventHandler(CaughtEvent(e))
        throw new HaltedException
      }
      case e: Throwable => {
        ctx.eventHandler(CaughtEvent(new JavaException(e)))
        throw new HaltedException
      }
    }
  }
}

case class If(b: Value, thn: Expr, els: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    if (dereference(b, ctx).asInstanceOf[java.lang.Boolean]) {
      thn.eval(ctx, interp)
    } else {
      els.eval(ctx, interp)
    }
  }
}

// ==================== PROCESS ===================

case class Spawn(target: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val clos = dereference(target, ctx).asInstanceOf[Closure]
    Logger.finer(s"Spawning: $target ${ctx.counter} { $clos }: ${prettyprint(interp)}")
    ctx.counter.increment()
    interp.schedule(clos, List(), halt = ctx.counter.haltHandler)
    Unit
  }
}

case class NewCounter(k: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val c = new Counter()
    Logger.finest(s"NewCounter ${c}: ${prettyprint(interp).replace('\n',' ').substring(0, 35)}")
    ctx.counter.increment()
    k.eval(ctx.copy(counter = c, oldCounter = ctx.counter), interp)
  }
}

case class RestoreCounter(zeroBranch: Expr, nonzeroBranch: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.finest(s"RestoreCounter ${ctx.counter}: ${prettyprint(interp)}")
    if (ctx.counter.decrementAndTestZero())
      zeroBranch.eval(ctx.copy(counter = ctx.oldCounter, oldCounter = null), interp)
    else
      nonzeroBranch.eval(ctx.copy(counter = null, oldCounter = null), interp)
  }
}
case class SetCounterHalt(haltCont: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val clos = dereference(haltCont, ctx).asInstanceOf[Closure]
    ctx.counter.haltHandler = clos
    Unit
  }
}
case object DecrCounter extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.finest(s"DecrCounter ${ctx.counter}: ${prettyprint(interp)}")
    val iz = ctx.counter.decrementAndTestZero()
    assert(!iz)
    Unit
  }
}
case class CallCounterHalt() extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val h = ctx.counter.haltHandler
    Logger.finer(s"Calling counter halt: ${ctx.counter}, $h: ${prettyprint(interp)}")
    Call(h, List()).eval(ctx, interp)
    Unit
  }
}
case object CallParentCounterHalt extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val h = ctx.oldCounter.haltHandler
    Logger.finer(s"Calling parent counter halt: ${ctx.counter}, parent ${ctx.oldCounter}, $h")
    Call(h, List()).eval(ctx, interp)
    Unit
  }
}

case class NewTerminator(k: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    CheckKilled.eval(ctx, interp)
    val t = new Terminator()
    k.eval(ctx.copy(terminator = t), interp)
  }
}
case object GetTerminator extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    ctx.terminator
  }
}
case object SetKill extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    ctx.terminator.setIsKilled() match {
      case Some(khs) => {
        Logger.finest(s"Killed terminator ${ctx.terminator}, calling kill handlers: ${khs}")
        khs.foreach(c => try {
          c.body.eval(c.ctx, interp)
        } catch {
          case _: KilledException => () // Ignore killed exceptions and go on to the next handler
        })
        true: java.lang.Boolean
      }
      case None => {
        Logger.finest(s"Already killed terminator ${ctx.terminator}")
        false: java.lang.Boolean
      }
    }
  }
}
case object Killed extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.fine("Throwing killed exception")
    throw KilledException
  }
}
case object CheckKilled extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    if (ctx.terminator.isKilled) {
      Logger.fine("Throwing killed exception")
      throw KilledException
    }
    Unit
  }
}
case class AddKillHandler(u: Value, m: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val t = dereference(u, ctx).asInstanceOf[Terminator]
    val kh = dereference(m, ctx).asInstanceOf[Closure]
    Logger.finest(s"Adding kill handler: ${t} ${kh}")
    t.addKillHandler(kh)
    Unit
  }
}

case class TryOnKilled(e: Expr, f: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    try {
      e.eval(ctx, interp)
    } catch {
      case _: KilledException =>
        f.eval(ctx, interp)
    }
  }
}

case class IsKilled(t: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    dereference(t, ctx).asInstanceOf[Terminator].isKilled: java.lang.Boolean
  }
}

case object Halted extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.fine("Throwing halted exception")
    throw HaltedException
  }
}

case class TryOnHalted(e: Expr, f: Expr) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    try {
      e.eval(ctx, interp)
    } catch {
      case _: HaltedException =>
        f.eval(ctx, interp)
    }
  }
}

// ==================== FUTURE ===================

case object NewFuture extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    new Future()
  }
}
case class Force(futures: List[Value], boundBranch: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.finest("Force: " + prettyprint(interp))
    val vs = futures map { x => dereference(x, ctx) }
    val bb = dereference(boundBranch, ctx).asInstanceOf[Closure]
    forceFutures(vs, bb, ctx, interp)
    Unit
  }
}
case class Bind(future: Value, value: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    dereference(future, ctx).asInstanceOf[Future].bind(dereference(value, ctx))
    Unit
  }
}
case class Stop(future: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    dereference(future, ctx).asInstanceOf[Future].halt()
    Unit
  }
}

// ==================== FLAG ===================

case object NewFlag extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    new Flag()
  }
}
case class SetFlag(flag: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    dereference(flag, ctx).asInstanceOf[Flag].set()
    Unit
  }
}
case class ReadFlag(flag: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    val f = dereference(flag, ctx).asInstanceOf[Flag]
    f.get: java.lang.Boolean
  }
}

// ==================== EXT ====================

case class ExternalCall(site: AnyRef, arguments: List[Value], p: Value) extends Expr {
  def eval(ctx: Context, interp: InterpreterContext) = {
    Logger.finest("ExternalCall: " + prettyprint(interp))
    val pc = dereference(p, ctx).asInstanceOf[Closure]
    invokeExternal(site, arguments map { x => dereference(x, ctx) }, pc, ctx, interp)
    Unit
  }
}
