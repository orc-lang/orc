
//
// Sites3Util.scala -- Scala traits and classes for 3-ary sites
// Project OrcScala
//
// AUTOGENERATED by orc.values.sites.GenerateSiteClasses
// Do not edit!!!
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.reflect.ClassTag

import orc.VirtualCallContext
import orc.SiteResponseSet
import orc.Invoker
import orc.OrcRuntime
import orc.DirectInvoker
import orc.error.runtime.HaltException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException

import InvocationBehaviorUtilities._

trait Site3 extends Site with SpecificArity {
  val arity = 3
}

abstract class Site3Base[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends HasGetInvoker3[A0, A1, A2] with Site3 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (VirtualCallContext, T, AA0, AA1, AA2) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with Site3Base.ImplInvoker[T, AA0, AA1, AA2] with MaybeInlinableInvoker {
      val impl = _impl
      final override val inlinable = Site3Base.this.inlinable
    }
  }

  protected def invokerInline[T <: Site3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (VirtualCallContext, T, AA0, AA1, AA2) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with Site3Base.ImplInvoker[T, AA0, AA1, AA2] with InlinableInvoker {
      val impl = _impl
    }
  }
}

object Site3Base {
  trait ImplInvoker[T, AA0, AA1, AA2] extends Invoker {
    val impl: (VirtualCallContext, T, AA0, AA1, AA2) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      /*orc.run.StopWatches.implementation*/ {
        impl(ctx, target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1], arguments(2).asInstanceOf[AA2])
      }
    }
  }
}

abstract class Site3Simple[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends Site3Base[A0, A1, A2] {
  def eval(ctx: VirtualCallContext, arg0: A0, arg1: A1, arg2: A2): SiteResponseSet

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1, arg2: A2) =
    invoker(this, arg0, arg1, arg2) { (ctx, self, arg0, arg1, arg2) =>
      self.eval(ctx, arg0, arg1, arg2)
    }
}

trait PartialSite3 extends PartialSite with SpecificArity {
  val arity = 3
}

abstract class PartialSite3Base[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends HasGetDirectInvoker3[A0, A1, A2] with PartialSite3 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: PartialSite3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (T, AA0, AA1, AA2) => Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with PartialSite3Base.ImplInvoker[T, AA0, AA1, AA2] with MaybeInlinableInvoker {
      val impl = _impl
      final override val inlinable = PartialSite3Base.this.inlinable
    }
  }

  protected def invokerInline[T <: PartialSite3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (T, AA0, AA1, AA2) => Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with PartialSite3Base.ImplInvoker[T, AA0, AA1, AA2] with InlinableInvoker {
      val impl = _impl
    }
  }
}

object PartialSite3Base {
  trait ImplInvoker[T, AA0, AA1, AA2] extends DirectInvoker {
    val impl: (T, AA0, AA1, AA2) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        /*orc.run.StopWatches.implementation*/ {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1], arguments(2).asInstanceOf[AA2])
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

abstract class PartialSite3Simple[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends PartialSite3Base[A0, A1, A2] {
  def eval(arg0: A0, arg1: A1, arg2: A2): Option[Any]

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1, arg2: A2) =
    invoker(this, arg0, arg1, arg2) { (self, arg0, arg1, arg2) =>
      self.eval(arg0, arg1, arg2)
    }
}

trait TotalSite3 extends TotalSite with SpecificArity {
  val arity = 3
}

abstract class TotalSite3Base[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends HasGetDirectInvoker3[A0, A1, A2] with TotalSite3 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (T, AA0, AA1, AA2) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with TotalSite3Base.ImplInvoker[T, AA0, AA1, AA2] with MaybeInlinableInvoker {
      val impl = _impl
      final override val inlinable = TotalSite3Base.this.inlinable
    }
  }

  protected def invokerInline[T <: TotalSite3Base[A0, A1, A2], AA0 <: A0, AA1 <: A1, AA2 <: A2]
        (exampleTarget: T, example0: AA0, example1: AA1, example2: AA2)
        (_impl: (T, AA0, AA1, AA2) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1, example2).asInstanceOf[Array[AnyRef]])
          with TotalSite3Base.ImplInvoker[T, AA0, AA1, AA2] with InlinableInvoker {
      val impl = _impl
    }
  }
}

object TotalSite3Base {
  trait ImplInvoker[T, AA0, AA1, AA2] extends DirectInvoker {
    val impl: (T, AA0, AA1, AA2) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        /*orc.run.StopWatches.implementation*/ {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1], arguments(2).asInstanceOf[AA2]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite3Simple[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] extends TotalSite3Base[A0, A1, A2] {
  def eval(arg0: A0, arg1: A1, arg2: A2): Any

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1, arg2: A2) =
    invoker(this, arg0, arg1, arg2) { (self, arg0, arg1, arg2) =>
      self.eval(arg0, arg1, arg2)
    }
}

abstract class HasGetInvoker3[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName, implicitly[ClassTag[A1]].runtimeClass.getSimpleName, implicitly[ClassTag[A2]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length != 3) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(3, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A1](args(1))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(1, argumentTypeStrings(1), if (args(1) != null) args(1).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A2](args(2))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(2, argumentTypeStrings(2), if (args(2) != null) args(2).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0], args(1).asInstanceOf[A1], args(2).asInstanceOf[A2])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1, arg2: A2): Invoker
}

abstract class HasGetDirectInvoker3[A0 : ClassTag, A1 : ClassTag, A2 : ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName, implicitly[ClassTag[A1]].runtimeClass.getSimpleName, implicitly[ClassTag[A2]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length != 3) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(3, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A1](args(1))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(1, argumentTypeStrings(1), if (args(1) != null) args(1).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A2](args(2))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(2, argumentTypeStrings(2), if (args(2) != null) args(2).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0], args(1).asInstanceOf[A1], args(2).asInstanceOf[A2])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1, arg2: A2): DirectInvoker
}

