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

import scala.reflect.ClassTag

import orc.error.{ NotYetImplementedException, OrcException }
import orc.error.compiletime.typing.TypeException
import orc.error.runtime.{ ArityMismatchException, HaltException, RightException }
import orc.types.{ Bot, RecordType, Type }
import orc.util.ArrayExtensions.{ Array0, Array1, Array2, Array3 }
import orc.values.{ OrcRecord, OrcValue }
import orc.{ OrcRuntime, Invoker, DirectInvoker, VirtualCallContext }
import orc.SiteResponseSet

import InvocationBehaviorUtilities._
import orc.values.{ FastRecord, FastRecordFactory }

//FIXME:XXX: "Serializable" here is a temporary hack.  Sites are not all Serializable.
trait Site extends OrcValue with SiteMetadata with Serializable {
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

/*
/* Enforce arity only */
trait Site0 extends Site with SpecificArity {
  val arity = 0
}

abstract class Site0Base extends HasGetInvoker0 with Site0 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site0Base](exampleTarget: T)
        (_impl: (VirtualCallContext, T) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array())
          with Site0Base.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object Site0Base {
  trait ImplInvoker[T] extends Invoker {
    val impl: (VirtualCallContext, T) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      orc.run.StopWatches.implementation {
        impl(ctx, target.asInstanceOf[T])
      }
    }
  }
}

trait Site1 extends Site with SpecificArity {
  val arity = 1
}

abstract class Site1Base[A1 : ClassTag] extends HasGetInvoker1[A1] with Site1 {

  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site1Base[A1], AA1 <: A1](exampleTarget: T, example1: AA1)
        (_impl: (VirtualCallContext, T, AA1) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass))
          with Site1Base.ImplInvoker[T, AA1] {
      val impl = _impl
    }
  }
}

object Site1Base {
  trait ImplInvoker[T, AA1] extends Invoker {
    val impl: (VirtualCallContext, T, AA1) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      orc.run.StopWatches.implementation {
        impl(ctx, target.asInstanceOf[T], arguments(0).asInstanceOf[AA1])
      }
    }
  }
}

trait Site2 extends Site with SpecificArity {
  val arity = 2
}

abstract class Site2Base[A1 : ClassTag, A2 : ClassTag] extends HasGetInvoker2[A1, A2] with Site2 {

  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site2Base[A1, A2], AA1 <: A1, AA2 <: A2](exampleTarget: T, example1: AA1, example2: AA2)
        (_impl: (VirtualCallContext, T, AA1, AA2) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass, example2.getClass))
          with Site2Base.ImplInvoker[T, AA1, AA2] {
      val impl = _impl
    }
  }
}

object Site2Base {
  trait ImplInvoker[T, AA1, AA2] extends Invoker {
    val impl: (VirtualCallContext, T, AA1, AA2) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      orc.run.StopWatches.implementation {
        impl(ctx, target.asInstanceOf[T], arguments(0).asInstanceOf[AA1], arguments(1).asInstanceOf[AA2])
      }
    }
  }
}

/* Enforce arity and nonblocking, but do not enforce totality */
trait PartialSite0 extends PartialSite with SpecificArity {
  val arity = 0
}

abstract class PartialSite0Base extends HasGetDirectInvoker0 with PartialSite0 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: PartialSite0Base](exampleTarget: T)
        (_impl: (T) =>  Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array())
          with PartialSite0Base.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object PartialSite0Base {
  trait ImplInvoker[T] extends DirectInvoker {
    val impl: (T) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T])
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

abstract class PartialSite0Simple extends PartialSite0Base {
  def eval(): Option[Any]

  final def getInvoker(runtime: OrcRuntime) = invoker(this) { (self) => self.eval() }
}

trait PartialSite1 extends PartialSite with SpecificArity {
  val arity = 1
}

abstract class PartialSite1Base[A1 : ClassTag] extends HasGetDirectInvoker1[A1] with PartialSite1 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: PartialSite1Base[A1], AA1 <: A1](exampleTarget: T, example1: AA1)
        (_impl: (T, AA1) =>  Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass))
          with PartialSite1Base.ImplInvoker[T, AA1] {
      val impl = _impl
    }
  }
}

object PartialSite1Base {
  trait ImplInvoker[T, AA1] extends DirectInvoker {
    val impl: (T, AA1) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA1])
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

abstract class PartialSite1Simple[A1 : ClassTag] extends PartialSite1Base[A1] {
  def eval(arg1: A1): Option[Any]

  final def getInvoker(runtime: OrcRuntime, arg1: A1) = invoker(this, arg1) { (self, a) => self.eval(a) }
}


/* Enforce arity and totality */
trait TotalSite0 extends TotalSite with SpecificArity {
  val arity = 0
}

abstract class TotalSite0Base extends HasGetDirectInvoker0 with TotalSite0 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite0Base](exampleTarget: T)
        (_impl: (T) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array())
          with TotalSite0Base.ImplInvoker[T] {
      val impl = _impl
    }
  }
}

object TotalSite0Base {
  trait ImplInvoker[T] extends DirectInvoker {
    val impl: (T) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite0Simple extends TotalSite0Base {
  def eval(): Any

  final def getInvoker(runtime: OrcRuntime) = invoker(this) { (self) => self.eval() }
}

trait TotalSite1 extends TotalSite with SpecificArity {
  val arity = 1
}

abstract class TotalSite1Base[A1 : ClassTag] extends HasGetDirectInvoker1[A1] with TotalSite1 {

  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite1Base[A1], AA1 <: A1](exampleTarget: T, example1: AA1)
        (_impl: (T, AA1) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass))
          with TotalSite1Base.ImplInvoker[T, AA1] {
      val impl = _impl
    }
  }
}

object TotalSite1Base {
  trait ImplInvoker[T, AA1] extends DirectInvoker {
    val impl: (T, AA1) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA1]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite1Simple[A1 : ClassTag] extends TotalSite1Base[A1] {
  def eval(arg1: A1): Any

  final def getInvoker(runtime: OrcRuntime, arg1: A1) = invoker(this, arg1) { (self, a) => self.eval(a) }
}


trait TotalSite2 extends TotalSite with SpecificArity {
  val arity = 2
}

abstract class TotalSite2Base[A1 : ClassTag, A2 : ClassTag] extends HasGetDirectInvoker2[A1, A2] with TotalSite2 {

  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite2Base[A1, A2], AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example1: AA1, example2: AA2)
        (_impl: (T, AA1, AA2) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass, example2.getClass))
          with TotalSite2Base.ImplInvoker[T, AA1, AA2] {
      val impl = _impl
    }
  }
}

object TotalSite2Base {
  trait ImplInvoker[T, AA1, AA2] extends DirectInvoker {
    val impl: (T, AA1, AA2) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA1], arguments(1).asInstanceOf[AA2]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite2Simple[A1 : ClassTag, A2 : ClassTag] extends TotalSite2Base[A1, A2] {
  def eval(arg1: A1, arg2: A2): Any

  final def getInvoker(runtime: OrcRuntime, arg1: A1, arg2: A2) = invoker(this, arg1, arg2) { (self, a, b) => self.eval(a, b) }
}

trait TotalSite3 extends TotalSite with SpecificArity {
  val arity = 3
}

abstract class TotalSite3Base[A1 : ClassTag, A2 : ClassTag, A3 : ClassTag] extends HasGetDirectInvoker3[A1, A2, A3] with TotalSite3 {

  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite3Base[A1, A2, A3], AA1 <: A1, AA2 <: A2, AA3 <: A3]
        (exampleTarget: T, example1: AA1, example2: AA2, example3: AA3)
        (_impl: (T, AA1, AA2, AA3) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget.getClass, Array(example1.getClass, example2.getClass, example3.getClass))
          with TotalSite3Base.ImplInvoker[T, AA1, AA2, AA3] {
      val impl = _impl
    }
  }
}

object TotalSite3Base {
  trait ImplInvoker[T, AA1, AA2, AA3] extends DirectInvoker {
    val impl: (T, AA1, AA2, AA3) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA1],
              arguments(1).asInstanceOf[AA2], arguments(2).asInstanceOf[AA3]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite3Simple[A1 : ClassTag, A2 : ClassTag, A3 : ClassTag] extends TotalSite3Base[A1, A2, A3] {
  def eval(arg1: A1, arg2: A2, arg3: A3): Any

  final def getInvoker(runtime: OrcRuntime, arg1: A1, arg2: A2, arg3: A3) = invoker(this, arg1, arg2, arg3) { (self, a, b, c) => self.eval(a, b, c) }
}
*/

object StructurePairSite {
  val recordFactory = new FastRecordFactory(Array("apply", "unapply"))
}

/* Template for building values which act as constructor-extractor sites,
 * such as the Some site.
 */
class StructurePairSite(
  applySite: TotalSite with TypedSite,
  unapplySite: PartialSite1 with TypedSite) extends
  FastRecord(StructurePairSite.recordFactory.members, Array(applySite, unapplySite)) {

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


/*
abstract class HasGetInvoker0 {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      getInvoker(runtime)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime): Invoker
}

abstract class HasGetInvoker1[A1 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 1 && valueHasTypeByTag[A1](args(0))) {
      getInvoker(runtime, args(0).asInstanceOf[A1])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1): Invoker
}

abstract class HasGetInvoker2[A1 : ClassTag, A2 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 2 && valueHasTypeByTag[A1](args(0)) && valueHasTypeByTag[A2](args(1))) {
      getInvoker(runtime, args(0).asInstanceOf[A1], args(1).asInstanceOf[A2])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1, argument2: A2): Invoker
}

abstract class HasGetInvoker3[A1 : ClassTag, A2 : ClassTag, A3 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 3 && valueHasTypeByTag[A1](args(0)) && valueHasTypeByTag[A2](args(1)) && valueHasTypeByTag[A3](args(2))) {
      getInvoker(runtime, args(0).asInstanceOf[A1], args(1).asInstanceOf[A2], args(2).asInstanceOf[A3])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1, argument2: A2, argument3: A3): Invoker
}




abstract class HasGetDirectInvoker0 {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length == 0) {
      getInvoker(runtime)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime): DirectInvoker
}

abstract class HasGetDirectInvoker1[A1 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length == 1 && valueHasTypeByTag[A1](args(0))) {
      getInvoker(runtime, args(0).asInstanceOf[A1])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1): DirectInvoker
}

abstract class HasGetDirectInvoker2[A1 : ClassTag, A2 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length == 2 && valueHasTypeByTag[A1](args(0)) && valueHasTypeByTag[A2](args(1))) {
      getInvoker(runtime, args(0).asInstanceOf[A1], args(1).asInstanceOf[A2])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1, argument2: A2): DirectInvoker
}

abstract class HasGetDirectInvoker3[A1 : ClassTag, A2 : ClassTag, A3 : ClassTag] {
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length == 3 && valueHasTypeByTag[A1](args(0)) && valueHasTypeByTag[A2](args(1)) && valueHasTypeByTag[A3](args(2))) {
      getInvoker(runtime, args(0).asInstanceOf[A1], args(1).asInstanceOf[A2], args(2).asInstanceOf[A3])
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvoker(runtime: OrcRuntime, argument1: A1, argument2: A2, argument3: A3): DirectInvoker
}
*/
