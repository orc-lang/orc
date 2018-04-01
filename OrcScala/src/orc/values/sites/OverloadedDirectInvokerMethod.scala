//
// OverloadedDirectInvokerMethod.scala -- Scala classes OverloadedDirectInvokerMethodN
// Project OrcScala
//
// Created by amp on Sept 25, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.reflect.ClassTag
import orc.OnlyDirectInvoker
import orc.OrcRuntime
import orc.Invoker
import orc.IllegalArgumentInvoker

// TODO: It may be possible and useful to use some kind of coercion support to allow sites to specify conversions for some cases to eliminate full cross product of types.
// However, it's not clear to me how to allow this while still having full speculation on the input types and correct type checks at call time.
// Also the old version also had this problem, so this is mostly something to fix when we fix the numeric stack which is the only thing that has this problem.

/** Base class for implementing 1-argument direct external methods with type based overloading.
  *
  * The subclass (often a Scala object) should implement getInvokerSpecialized and
  * use invoker (or invokerStaticType) to create invokers.
  * 
  * See orc.lib.math.Log and orc.lib.math.Add for examples.
  */
abstract class OverloadedDirectInvokerMethod1[BaseArgumentType1 : ClassTag] extends InvokerMethod with SiteMetadata {
  thisMethod =>
  
  private[this] val clsBaseArgumentType1 = implicitly[ClassTag[BaseArgumentType1]].runtimeClass.asInstanceOf[Class[BaseArgumentType1]]
  
  abstract class InvokerBase[-T1 <: BaseArgumentType1](private[this] val clsT1: Class[_]) extends OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      val argumentTypeCorrect = clsT1.isInstance(arguments(0)) || (clsT1 eq clsBaseArgumentType1) && arguments(0) == null 
      target == thisMethod && arguments.length == 1 && argumentTypeCorrect
    }
  
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.implementation {
        invokeDirectSpecialized(clsT1.cast(arguments(0)).asInstanceOf[T1])
      }
    }
    
    def invokeDirectSpecialized(arg1: T1): AnyRef
    
    override def toString(): String = s"$thisMethod<invoker>(${clsT1.getName})"
  }
  
  /** Call to create an invoker which is used for all calls with an argument with this **static** type.
    * 
    * This should only be used in cases where using the dynamic type is specifically undesirable (such
    * as for fallback cases or cases that will not use the argument).
    */
  @inline
  def invokerStaticType[T1 <: BaseArgumentType1 : ClassTag](arg1: T1)(f: (T1) => Any) = {
    new InvokerBase[T1](implicitly[ClassTag[T1]].runtimeClass) {
      def invokeDirectSpecialized(arg1: T1): AnyRef = f(arg1).asInstanceOf[AnyRef]
    }
  }
  
  /** Call to create an invoker which is used for all calls with an argument with this **dynamic** type.
    * 
    * Unless you have specific reasons, you should use this method to create invokers.
    */
  @inline
  def invoker[T1 <: BaseArgumentType1 : ClassTag](arg1: T1)(f: (T1) => Any) = {
    new InvokerBase[T1](arg1.getClass()) {
      def invokeDirectSpecialized(arg1: T1): AnyRef = f(arg1).asInstanceOf[AnyRef]
    }
  }
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 1 &&  (clsBaseArgumentType1.isInstance(args(0)) || args(0) == null)) {
      getInvokerSpecialized(clsBaseArgumentType1.cast(args(0)))
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvokerSpecialized(arg1: BaseArgumentType1): Invoker
  
  override def publications: Range = Range(0, 1)
  override def isDirectCallable: Boolean = true
}

/** Base class for implementing 2-argument direct external methods with type based overloading.
  * 
  * @see OverloadedDirectInvokerMethod1
  */
abstract class OverloadedDirectInvokerMethod2[BaseArgumentType1 : ClassTag, BaseArgumentType2 : ClassTag] extends InvokerMethod with SiteMetadata {
  thisMethod =>
  
  private[this] val clsBaseArgumentType1 = implicitly[ClassTag[BaseArgumentType1]].runtimeClass.asInstanceOf[Class[BaseArgumentType1]]
  private[this] val clsBaseArgumentType2 = implicitly[ClassTag[BaseArgumentType2]].runtimeClass.asInstanceOf[Class[BaseArgumentType2]]
  
  abstract class InvokerBase[-T1 <: BaseArgumentType1, -T2 <: BaseArgumentType2](private[this] val clsT1: Class[_], private[this] val clsT2: Class[_]) extends OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      val argumentTypeCorrect1 = clsT1.isInstance(arguments(0)) || (clsT1 eq clsBaseArgumentType1) && arguments(0) == null 
      val argumentTypeCorrect2 = clsT2.isInstance(arguments(1)) || (clsT2 eq clsBaseArgumentType2) && arguments(1) == null 
      target == thisMethod && arguments.length == 2 && argumentTypeCorrect1 && argumentTypeCorrect2
    }
  
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.implementation {
        invokeDirectSpecialized(clsT1.cast(arguments(0)).asInstanceOf[T1], clsT2.cast(arguments(1)).asInstanceOf[T2])
      }
    }
    
    def invokeDirectSpecialized(arg1: T1, arg2: T2): AnyRef
    
    override def toString(): String = s"$thisMethod<invoker>(${clsT1.getName}, ${clsT2.getName})"
  }
  
  /** @see OverloadedDirectInvokerMethod1.invokerStaticType
    */
  @inline
  def invokerStaticType[T1 <: BaseArgumentType1 : ClassTag, T2 <: BaseArgumentType2 : ClassTag](arg1: T1, arg2: T2)(f: (T1, T2) => Any) = {
    new InvokerBase[T1, T2](implicitly[ClassTag[T1]].runtimeClass, implicitly[ClassTag[T2]].runtimeClass) {
      def invokeDirectSpecialized(arg1: T1, arg2: T2): AnyRef = f(arg1, arg2).asInstanceOf[AnyRef]
    }
  }
  
  /** @see OverloadedDirectInvokerMethod1.invoker
    */
  @inline
  def invoker[T1 <: BaseArgumentType1 : ClassTag, T2 <: BaseArgumentType2 : ClassTag](arg1: T1, arg2: T2)(f: (T1, T2) => Any) = {
    new InvokerBase[T1, T2](arg1.getClass(), arg2.getClass()) {
      def invokeDirectSpecialized(arg1: T1, arg2: T2): AnyRef = f(arg1, arg2).asInstanceOf[AnyRef]
    }
  }
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 2 && 
        (clsBaseArgumentType1.isInstance(args(0)) || args(0) == null) && 
        (clsBaseArgumentType2.isInstance(args(1)) || args(1) == null)) {
      getInvokerSpecialized(clsBaseArgumentType1.cast(args(0)), clsBaseArgumentType2.cast(args(1)))
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }

  def getInvokerSpecialized(arg1: BaseArgumentType1, arg2: BaseArgumentType2): Invoker
  
  override def publications: Range = Range(0, 1)
  override def isDirectCallable: Boolean = true
}
