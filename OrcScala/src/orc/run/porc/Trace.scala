//
// Trace.scala -- Scala class/trait/object Trace
// Project OrcScala
//
// $Id$
//
// Created by amp on Dec 20, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import scala.collection.mutable

/** An execution trace representing what happened to get to this point in the 
 *  program. Like a generalized stack trace that goes through process spawns.
  *
  * @author amp
  */
case class Trace(frames: Seq[Expr] = List()) {
  def +(p: Expr) = Trace(p +: frames) 
  
  override def toString = {
    "  "+frames.map(_.toStringShort).mkString("", "\n  ", "")
  }
  def toString(interp: InterpreterContext): String = {
    "  "+frames.map(e => s"${interp.positionOf(e).debugString} ${e.toStringShort}").mkString("", "\n  ", "")
  }
}

object Trace {
  /*private val currentLocalTrace = new ThreadLocal[Trace]()
  def current_=(v: Trace) = currentLocalTrace.set(v)
  def current: Trace = {
    Option(currentLocalTrace.get()) getOrElse Trace()
  }
  
  def +=(p: PorcPosition) = current += p
  
  def stash(k: AnyRef, current: Trace): Unit = synchronized {
    traceStash(System.identityHashCode(k)) = current
  }
  def unstash(k: AnyRef) = synchronized {
    val kh = System.identityHashCode(k)
    val t = traceStash(kh)
    traceStash.remove(kh)
    t
  }
  
  private val traceStash: mutable.Map[Int, Trace] = mutable.Map()
  */ 
}
