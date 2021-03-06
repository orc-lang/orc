//
// Sites1Util.scala -- Scala traits and classes for 1-ary sites
// Project OrcScala
//
// AUTOGENERATED by orc.values.sites.GenerateSiteClasses
// Do not edit!!!
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.reflect.ClassTag

import orc.{ DirectInvoker, Invoker, OrcRuntime, SiteResponseSet, VirtualCallContext }
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException, HaltException }

import InvocationBehaviorUtilities.valueHasTypeByTag

trait Site1 extends Site with SpecificArity {
  val arity = 1
}

abstract class Site1Base[A0: ClassTag] extends HasGetInvoker1[A0] with Site1 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site1Base[A0], AA0 <: A0]
        (exampleTarget: T, example0: AA0)
        (_impl: (VirtualCallContext, T, AA0) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0).asInstanceOf[Array[AnyRef]])
          with Site1Base.ImplInvoker[T, AA0] {
      val impl = _impl
    }
  }
}

object Site1Base {
  trait ImplInvoker[T, AA0] extends Invoker {
    val impl: (VirtualCallContext, T, AA0) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      orc.run.StopWatches.implementation {
        impl(ctx, target.asInstanceOf[T], arguments(0).asInstanceOf[AA0])
      }
    }
  }
}

abstract class Site1Simple[A0: ClassTag] extends Site1Base[A0] {
  def eval(ctx: VirtualCallContext, arg0: A0): SiteResponseSet

  final def getInvoker(runtime: OrcRuntime, arg0: A0) =
    invoker(this, arg0) { (ctx, self, arg0) =>
      self.eval(ctx, arg0)
    }
}

trait PartialSite1 extends PartialSite with SpecificArity {
  val arity = 1
}

abstract class PartialSite1Base[A0: ClassTag] extends HasGetDirectInvoker1[A0] with PartialSite1 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: PartialSite1Base[A0], AA0 <: A0]
        (exampleTarget: T, example0: AA0)
        (_impl: (T, AA0) => Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0).asInstanceOf[Array[AnyRef]])
          with PartialSite1Base.ImplInvoker[T, AA0] {
      val impl = _impl
    }
  }
}

object PartialSite1Base {
  trait ImplInvoker[T, AA0] extends DirectInvoker {
    val impl: (T, AA0) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0])
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

abstract class PartialSite1Simple[A0: ClassTag] extends PartialSite1Base[A0] {
  def eval(arg0: A0): Option[Any]

  final def getInvoker(runtime: OrcRuntime, arg0: A0) =
    invoker(this, arg0) { (self, arg0) =>
      self.eval(arg0)
    }
}

trait TotalSite1 extends TotalSite with SpecificArity {
  val arity = 1
}

abstract class TotalSite1Base[A0: ClassTag] extends HasGetDirectInvoker1[A0] with TotalSite1 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite1Base[A0], AA0 <: A0]
        (exampleTarget: T, example0: AA0)
        (_impl: (T, AA0) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0).asInstanceOf[Array[AnyRef]])
          with TotalSite1Base.ImplInvoker[T, AA0] {
      val impl = _impl
    }
  }
}

object TotalSite1Base {
  trait ImplInvoker[T, AA0] extends DirectInvoker {
    val impl: (T, AA0) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite1Simple[A0: ClassTag] extends TotalSite1Base[A0] {
  def eval(arg0: A0): Any

  final def getInvoker(runtime: OrcRuntime, arg0: A0) =
    invoker(this, arg0) { (self, arg0) =>
      self.eval(arg0)
    }
}

abstract class HasGetInvoker1[A0: ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length != 1) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(1, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0): Invoker
}

abstract class HasGetDirectInvoker1[A0: ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length != 1) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(1, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0): DirectInvoker
}

