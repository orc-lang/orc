//
// EvalContext.scala -- Scala class/trait/object EvalContext
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 14, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.ArrayList
import orc.run.StandardInvocationBehavior
import java.util.concurrent.LinkedBlockingDeque

final case class Closure(body: Expr, var ctx: Context) {
  override def toString = s"Clos(${body.toString.replace('\n', ' ').take(100)})"
}

final class Terminator {
  val _isKilled = new AtomicBoolean(false)
  def isKilled = _isKilled.get()
  def setIsKilled() : Boolean = _isKilled.getAndSet(true) == false

  Logger.fine(s"created $this")

  var _killHandlers = new ArrayList[Closure]()
  def addKillHandler(kh: Closure) = _killHandlers synchronized { 
    //assert(!isKilled)
    _killHandlers.add(kh)
  }
  def killHandlers = _killHandlers synchronized { 
    assert(isKilled)
    import collection.JavaConversions._
    _killHandlers.toList
  }
}

final class Counter {
  val _count: AtomicInteger = new AtomicInteger(1)

  Logger.fine(s"created $this")

  def increment() {
    val v = _count.get()
    if( v <= 0 ) {
      assert(v > 0, "Resurrecting old Counter") 
    }
    Logger.finer(s"incr $this ($v)")
    _count.incrementAndGet()
  }
  def decrementAndTestZero() = {
    val v = _count.get()
    if( v <= 0 ) {
      assert(v > 0, "Uberkilling old Counter") 
    }
    Logger.finer(s"decr $this ($v)")
    _count.decrementAndGet() == 0
  }

  @volatile
  var haltHandler: Closure = null 
  /*
  def haltHandler = synchronized {
    assert(_haltHandler != null)
    Logger.finest(s"Getting halt $this, ${_haltHandler}")
    _haltHandler
  }
  def haltHandler_=(v: Closure) = synchronized { 
    assert(_haltHandler == null)
    Logger.finest(s"Setting halt $this, $v")
    _haltHandler = v
  }
  */
}

/**
  * A store that encodes the state of a process in the interpreter.
  * @author amp
  */
final case class Context(_valueStack: List[AnyRef], terminator: Terminator, counter: Counter, oldCounter: Counter) {
  def pushValue(v: Value) = copy(_valueStack = v :: _valueStack)
  def pushValues(vs: Seq[Value]) = copy(_valueStack = vs.foldRight(_valueStack)(_::_))
  @inline def getValue(i: Int) = _valueStack(i)
  @inline def apply(i: Int) = getValue(i)
  
  override def toString = {
    s"Context(terminator = $terminator, counter = $counter, oldCounter = $oldCounter, \n" ++
    _valueStack.zipWithIndex.flatMap{ v => 
      val v1s = if(v._1 == null) "null" else v._1.toString.replace('\n', ' ')
      s"${v._2}: $v1s\n" 
    } ++ ")"
  }
}

final class InterpreterContext(val engine: Interpreter) extends StandardInvocationBehavior {
  def schedule(clos: Closure, args: List[AnyRef] = Nil, halt: Closure = null) {
    queue.add((clos, args, halt))
  }
  def schedule(t: (Closure, List[AnyRef], Closure)) {
    queue.add(t)
  }
  def dequeue(): Option[(Closure, List[AnyRef], Closure)] = {
    Option(queue.poll())
  }
  def steal(): Option[(Closure, List[AnyRef], Closure)] = {
    Option(queue.pollLast())
  }
  
  // TODO: This needs to be a special dequeue implementation.
  val queue = new LinkedBlockingDeque[(Closure, List[AnyRef], Closure)]()
} 

object InterpreterContext {
  private val currentInterpreterContext = new ThreadLocal[InterpreterContext]()
  def current_=(v: InterpreterContext) = currentInterpreterContext.set(v)
  def current: InterpreterContext = {
    Option(currentInterpreterContext.get()) getOrElse defaultCtx
  }
  
  private var defaultCtx: InterpreterContext = null
  
  def default_=(v: InterpreterContext) {
    assert(defaultCtx == null)
    defaultCtx = v
  }
  def default = defaultCtx
}
