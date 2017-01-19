//
// SiteAdaptor.scala -- Scala class ClassAdaptor
// Project OrcScala
//
// Created by jthywiss on Jun 2, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility

import orc.values.sites.Site
import orc.values.sites.UntypedSite
import orc.values.sites.{ Range, Delay, Effects }
import orc.Handle
import orc.values.Signal
import orc.values.OrcTuple
import orc.error.runtime.TokenException
import orc.types.Type
import orc.values.sites.FunctionalSite
import orc.values.sites.DirectSite

/** Adapts old OrcJava sites to the new OrcScala site interface
  *
  * @author jthywiss
  */
abstract class SiteAdaptor extends Site {
  import SiteAdaptor._

  def call(args: Array[AnyRef], h: Handle) {
    callSite(convertArgs(args), h)
  }

  /** Must be implemented by subclasses to implement the site behavior
    * @param args          list of argument values
    * @param caller    where the result should be sent
    */
  @throws(classOf[TokenException])
  def callSite(args: Args, h: Handle): Unit

  def nonBlocking() = false
  def minPublications() = 0
  def maxPublications() = -1
  def effectFree() = false

  override def publications: Range = {
    val maxP = maxPublications()
    Range(minPublications(), if (maxP >= 0) Some(maxP) else None)
  }
  override def timeToPublish: Delay = if (nonBlocking()) Delay.NonBlocking else Delay.Blocking
  override def timeToHalt: Delay = if (nonBlocking()) Delay.NonBlocking else Delay.Blocking
  override def effects: Effects = if (effectFree()) Effects.None else Effects.Anytime
}

object SiteAdaptor {
  import scala.collection.JavaConversions._

  def object2value(o: java.lang.Object): AnyRef = o match {
    case s: Site => s
    case i: java.math.BigInteger => new scala.math.BigInt(i)
    case d: java.math.BigDecimal => new scala.math.BigDecimal(d)
    case _ => o
  }

  def signal() = Signal

  def makePair(left: AnyRef, right: AnyRef) = OrcTuple(Array(left, right))

  def makeCons[T](head: T, tail: List[T]) = head :: tail

  def makeListFromArray(array: AnyRef) = array.asInstanceOf[Array[_]].toList

  def makeList(javaIterable: java.lang.Iterable[_]) = javaIterable.toList

  def nilList[T](): List[T] = Nil

  /** Convert the array to the format needed for external calls.
    *
    * This will mutate the original array.
    */
  def convertArgs(args: Array[AnyRef]) = {
    val jl = new java.util.ArrayList[Object](args.size)
    for (ind <- 0 until args.length) {
      args(ind) match {
        case i: scala.math.BigInt => args(ind) = i.bigInteger
        case d: scala.math.BigDecimal => args(ind) = d.bigDecimal
        case _ => {}
      }
    }
    new Args(args)
  }
}

abstract class SiteAdaptorFunctional extends SiteAdaptor with FunctionalSite

abstract class EvalSite extends SiteAdaptor with DirectSite {
  import SiteAdaptor._

  @throws(classOf[TokenException])
  def callSite(args: Args, caller: Handle): Unit = {
    caller.publish(object2value(evaluate(args)))
  }

  @throws(classOf[TokenException])
  def evaluate(args: Args): Object

  def calldirect(args: Array[AnyRef]): AnyRef = {
    object2value(evaluate(convertArgs(args)))
  }
}
