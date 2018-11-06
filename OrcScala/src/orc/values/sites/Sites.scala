//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import java.io.InvalidClassException
import java.lang.reflect.Modifier

import orc.{ DirectInvoker, Invoker, OrcRuntime, SiteResponseSet, VirtualCallContext }
import orc.error.OrcException
import orc.error.compiletime.typing.TypeException
import orc.error.runtime.HaltException
import orc.types.{ Bot, RecordType, Type }
import orc.values.{ FastObject, OrcValue }

trait Site extends OrcValue with SiteMetadata {
  /** Get an invoker for this target type and argument types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an
    *         instance of InvokerError if there is no invoker.
    *
    * @see UncallableValueInvoker
    */
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker

  override def toOrcSyntax() = this.name
}

abstract class SiteBase extends Site {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: SiteBase](exampleTarget: T, examplesArguments: AnyRef*)
        (_impl: (VirtualCallContext, T, Array[AnyRef]) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, examplesArguments.toArray)
          with SiteBase.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object SiteBase {
  trait ImplInvoker[T] extends Invoker {
    val impl: (VirtualCallContext, T, Array[AnyRef]) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      try {
        orc.run.StopWatches.implementation {
          impl(ctx, target.asInstanceOf[T], arguments)
        }
      } catch {
        case e: OrcException =>
          ctx.halt(e)
      }
    }
  }
}

trait DirectSite extends Site with DirectSiteMetadata {
  /** Get an invoker for this target type and argument types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an
    *         instance of InvokerError if there is no invoker.
    *
    * @see UncallableValueInvoker
    */
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker
}

/* A site which provides type information. */
trait TypedSite extends Site {
  @throws(classOf[TypeException])
  def orcType(): Type
}

/* A site which explicitly lacks type information. */
/* Use sparingly; this is equivalent to using a type override */
trait UntypedSite extends TypedSite {
  def orcType() = Bot
}

trait SpecificArity extends Site {
  val arity: Int
}

// TODO: TotalSite and PartialSite will not work correctly if they are DirectSites and any site is actually blocking.

/** Enforce totality
  */
trait TotalSite extends DirectSite with EffectFreeAfterPubSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker

  override def publications: Range = super.publications intersect Range(0, 1)
}

abstract class TotalSiteBase extends TotalSite {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSiteBase](exampleTarget: T, examplesArguments: AnyRef*)
        (_impl: (T, Array[AnyRef]) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, examplesArguments.toArray)
          with TotalSiteBase.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object TotalSiteBase {
  trait ImplInvoker[T] extends DirectInvoker {
    val impl: (T, Array[AnyRef]) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

/** Enforce nonblocking, but do not enforce totality
  */
trait PartialSite extends DirectSite with EffectFreeAfterPubSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker

  override def publications: Range = super.publications intersect Range(0, 1)
}

abstract class PartialSiteBase extends PartialSite {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSiteBase](exampleTarget: T, examplesArguments: AnyRef*)
        (_impl: (T, Array[AnyRef]) =>  Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, examplesArguments.toArray)
          with PartialSiteBase.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object PartialSiteBase {
  trait ImplInvoker[T] extends DirectInvoker {
    val impl: (T, Array[AnyRef]) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments)
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }) match {
        case Some(v) => v.asInstanceOf[AnyRef]
        case None => throw new HaltException()
      }
    }
  }
}

object StructurePairSite {
  val members = FastObject.members("apply", "unapply")
}

/* Template for building values which act as constructor-extractor sites,
 * such as the Some site.
 */
class StructurePairSite(
  applySite: TotalSite with TypedSite,
  unapplySite: PartialSite1 with TypedSite) extends
  FastObject(StructurePairSite.members) {
  protected val values = Array(applySite, unapplySite)

  // FIXME: This should be a TypedSite or something similar, but that makes it callable, so breaks runtime checks.

  def orcType() = new RecordType(
    "apply" -> applySite.orcType(),
    "unapply" -> unapplySite.orcType())
}

trait NonBlockingSite extends SiteMetadata {
  override def timeToPublish: Delay = Delay.NonBlocking
  override def timeToHalt: Delay = Delay.NonBlocking
}

trait EffectFreeSite extends SiteMetadata {
  override def effects: Effects = Effects.None
}
trait EffectFreeAfterPubSite extends SiteMetadata {
  override def effects: Effects = Effects.BeforePub
}

trait TalkativeSite extends SiteMetadata {
  override def publications: Range = super.publications intersect Range(1, None)
}

trait FunctionalSite extends SiteMetadata with NonBlockingSite with EffectFreeSite

/** A Site that is a Scala object (singleton), but only needs to be a
  * singleton per JVM/Orc runtime, not globally for the program.  For example,
  * a Site that is an object simply because it carries no state.
  */
trait LocalSingletonSite {
  _: java.io.Serializable =>
  @throws(classOf[java.io.ObjectStreamException])
  protected def writeReplace(): AnyRef = {
      if (!Modifier.isStatic(this.getClass.getField("MODULE$").getModifiers)) {
        throw new InvalidClassException("A LocalSingletonSite must be a Scala object")
      }
      new LocalSingletonSiteMarshalingReplacement(this.getClass)
  }
}

protected case class LocalSingletonSiteMarshalingReplacement(singletonClass: Class[_ <: java.lang.Object]) {
  @throws(classOf[java.io.ObjectStreamException])
  protected def readResolve(): AnyRef = singletonClass.getField("MODULE$").get(null)
}
