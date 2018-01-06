//
// JavaProxies.scala -- Scala class JavaProxy
// Project OrcScala
//
// Created by jthywiss on Jul 9, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

import java.lang.reflect.{ Array => JavaArray, Field => JavaField, InvocationTargetException }

import orc.{ Accessor, InvocationBehaviorUtilities, Invoker, NoSuchMemberAccessor, ErrorAccessor, OnlyDirectInvoker, TargetThrowsInvoker, TargetArgsThrowsInvoker }
import orc.error.runtime.{ HaltException, JavaException, MethodTypeMismatchException, NoSuchMemberException, MalformedArrayAccessException, RuntimeTypeException }
import orc.run.Logger
import orc.OrcRuntime
import orc.util.ArrayExtensions.Array1
import orc.values.{ Field => OrcField, Signal }
import orc.values.sites.OrcJavaCompatibility.{ Invocable, chooseMethodForInvocation, java2orc, orc2java }
import java.lang.invoke.MethodHandles
import orc.Invoker
import orc.IllegalArgumentInvoker
import orc.OnlyDirectInvoker
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/** Due to the way dispatch is handled we cannot pass true wrappers back into Orc. They
  * would interfere with any call to which they were passed as an argument.
  *
  * Instead any object needs to be passed back bare and then handled again in getInvoker
  * and getAccessor.
  *
  * This does not apply to objects which "wrap" members and similar since those are not
  * values with types in Java and hence cannot appear as argument in other calls.
  */

/** Transforms an Orc site call to an appropriate Java invocation
  *
  * @author jthywiss, amp
  */
object JavaCall {
  def valueAndType(v: AnyRef): String = {
    val str = v match {
      case a: Array[_] => a.map(x => valueAndType(x.asInstanceOf[AnyRef])).mkString("Array(", ", ", ")")
      case v => v.toString
    }
    s"$str : ${v.getClass.getSimpleName}"
  }

  def classNameAndSignature(cls: Class[_], methodName: String, argTypes: Seq[Class[_]]): String = {
    cls.getCanonicalName() + "." + methodName + "(" + argTypes.map(_.getCanonicalName()).mkString(", ") + ")"
  }

  /** Invoke a method on the given Java object of the given name with the given arguments */
  def selectMethod(cls: Class[_], methodName: String, argClss: Array[Class[_]]): Invocable = {
    // No unwrapping of types is nessecary at this point. Very little would be possible anyway.
    try {
      chooseMethodForInvocation(cls, methodName, argClss)
    } catch { // Fill in "blank" exceptions with more details
      case e: java.lang.NoSuchMethodException if (e.getMessage() == null) =>
        throw new java.lang.NoSuchMethodException(classNameAndSignature(cls, methodName, argClss))
    }
  }
  
  implicit class MethodAdds(m: java.lang.reflect.Member) {
    def isStatic() = java.lang.reflect.Modifier.isStatic(m.getModifiers())    
  }

  implicit class ClassAdds(cls: Class[_]) {
    def hasInstanceMember(f: String) = {
      cls.getMethods().exists(m => m.getName() == f && !m.isStatic()) || cls.getFields().exists(m => m.getName() == f && !m.isStatic())
    }
    def hasStaticMember(f: String) = {
      cls.getMethods().exists(m => m.getName() == f && m.isStatic()) || cls.getFields().exists(m => m.getName() == f && m.isStatic())
    }
    def getFieldOption(memberName: String): Option[JavaField] = {
      try {
        Some(cls.getField(memberName))
      } catch {
        case _: NoSuchFieldException => None
      }
    }

    import orc.InvocationBehaviorUtilities._

    @throws[NoSuchMethodException]
    def getMemberInvokerTypeDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          cls == target.getClass() && valuesHaveType(arguments, argClss)
        }
        override def toString() = s"<Member Invoker>($cls.$methodName)"
      }
    }

    @throws[NoSuchMethodException]
    def getMemberInvokerValueDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          cls == target && valuesHaveType(arguments, argClss)
        }
        override def toString() = s"<Member Invoker>($cls.$methodName)"
      }
    }

    def getMemberAccessor(memberName: String): Accessor = {
      val javaField = cls.getFieldOption(memberName)
      new Accessor {
        def canGet(target: AnyRef): Boolean = {
          cls.isInstance(target)
        }
        def get(target: AnyRef): AnyRef = {
          new JavaMemberProxy(target, memberName, javaField)
        }
        override def toString() = s"<Member Accessor>($cls.$memberName)"
      }
    }
    
    def getStaticMemberAccessor(memberName: String): Accessor = {
      val javaField = cls.getFieldOption(memberName)
      new Accessor {
        def canGet(target: AnyRef): Boolean = {
          cls == target
        }
        def get(target: AnyRef): AnyRef = {
          new JavaStaticMemberProxy(target.asInstanceOf[Class[_ <: AnyRef]], memberName, javaField)
        }
        override def toString() = s"<Static Member Accessor>($cls.$memberName)"
      }
    }
  }

  def getInvoker(target: AnyRef, args: Array[AnyRef]): Option[Invoker] = {
    def targetCls = target.getClass()
    val argClss = args.map(InvocationBehaviorUtilities.valueType)
    try {
      (target, args) match {
        case (null, _) =>
          None
          
        // ARRAYS
        case (_, Array1(l: java.lang.Long)) if targetCls.isArray() => {
          Some(new OnlyDirectInvoker {
            def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
              target.getClass().isArray() && arguments.length == 1 && arguments(0).isInstanceOf[java.lang.Long]
            }
            @throws[HaltException]
            def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
              orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
              try {
                new JavaArrayElementProxy(target, arguments(0).asInstanceOf[java.lang.Long].intValue())
              } finally {
                if (orc.run.RuntimeProfiler.profileRuntime) {
                  orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
                }
              }
            }
          })
        }
        case (_, Array1(_: BigInt | _: java.lang.Long | _: java.lang.Integer)) if targetCls.isArray() => {
          Some(new OnlyDirectInvoker {
            def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
              target.getClass().isArray() && arguments.length == 1 && 
              (arguments(0).isInstanceOf[BigInt] || arguments(0).isInstanceOf[java.lang.Long] || arguments(0).isInstanceOf[java.lang.Integer]) 
            }
            @throws[HaltException]
            def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
              orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
              try {
                new JavaArrayElementProxy(target, arguments(0).asInstanceOf[Number].intValue())
              } finally {
                if (orc.run.RuntimeProfiler.profileRuntime) {
                  orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
                }
              }
            }
          })
        }
        // We should have boxed any java.lang.Integer, java.lang.Short, or java.lang.Byte value into BigInt
        case _ if targetCls.isArray() => 
          Some(TargetArgsThrowsInvoker(target, args, new MalformedArrayAccessException(args)))
  
        // CLASSES (Constructors)
        case (target: Class[_], _) => {
          if (target.getConstructors().nonEmpty) {
            Some(target.getMemberInvokerValueDirected("<init>", argClss))
          } else if (target.hasStaticMember("apply")) {
            Some(target.getMemberInvokerValueDirected("apply", argClss))          
          } else {
            None
          }
        }
  
        // NORMAL CALLS
        case _ => {
          if (targetCls.hasInstanceMember("apply")) {
            Some(targetCls.getMemberInvokerTypeDirected("apply", argClss))
          } else {
            None
          }
        }
      }
    } catch {
      case e: RuntimeTypeException =>
        Some(TargetThrowsInvoker(target, e))
    }
  }

  def getAccessor(target: AnyRef, f: OrcField): Option[Accessor] = {
    target match {
      case null =>
        None
        
      // CLASSES (static fields)
      case target: Class[_] if target.hasStaticMember(f.name) => {
        Some(target.getStaticMemberAccessor(f.name))
      }

      // INSTANCES (instance fields)
      case _ if target.getClass().hasInstanceMember(f.name) => {
        Some(target.getClass().getMemberAccessor(f.name))
      }

      // ARRAYS (pretend arrays have a length field. the other cases are handled above.)
      case _ if target.getClass().isArray() && f.name == "length" => {
        Some(target.getClass().getMemberAccessor(f.name))
      }

      case _ =>
        None
    }
  }
}

/** 
  * @author jthywiss, amp
  */
abstract class InvocableInvoker(@inline val invocable: Invocable, @inline val targetCls: Class[_], @inline val argumentClss: Array[Class[_]]) extends OnlyDirectInvoker {
  import JavaCall._
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean
  
  val mh = {
    val m = invocable.toMethodHandle
    m.asSpreader(classOf[Array[Object]], m.`type`().parameterCount() - 1)
  }

  def invokeDirect(theObject: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.JavaDispatch)
    try {
      if (theObject == null && !invocable.isStatic) {
        throw new NullPointerException("Instance method called without a target object (i.e. non-static method called on a class)")
      }
      val finalArgs = if (invocable.isVarArgs) {        
        // TODO: PERFORMANCE: It may be worth it to replace all these scala collections calls with some optimized loops. Hot path, mumble, mumble.
        
        // Group var args into nested array argument.
        val nNormalArgs = invocable.getParameterTypes.size - 1
        val (normalArgs, varArgs) = (arguments.take(nNormalArgs), arguments.drop(nNormalArgs))
        val convertedNormalArgs = (normalArgs, invocable.getParameterTypes).zipped.map(orc2java(_, _))

        val varargType = invocable.getParameterTypes.last.getComponentType()
        val convertedVarArgs = varArgs.map(orc2java(_, varargType))
        // The vararg array needs to have the correct dynamic type so we create it using reflection.
        val varArgArray = JavaArray.newInstance(varargType, varArgs.size).asInstanceOf[Array[Object]]
        convertedVarArgs.copyToArray(varArgArray)

        (convertedNormalArgs :+ varArgArray).toArray
      } else {
        // IT might be good to optimize the vararg case above as well, but it's much less of a hot path and it would be harder to optimizer.
        val paramTypes = invocable.getParameterTypes
        val end = arguments.length min paramTypes.size
        var i = 0
        while(i < end) {
          arguments(i) = orc2java(arguments(i), paramTypes(i))
          i += 1
        }
        arguments
      }
      //Logger.finer(s"Invoking Java method ${classNameAndSignature(targetCls, invocable.getName, invocable.getParameterTypes.toList)} with (${finalArgs.map(valueAndType).mkString(", ")})")
      orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
      val r = try {
        mh.invoke(theObject, finalArgs)
      } finally {
        if (orc.run.RuntimeProfiler.profileRuntime) {
          orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
        }
      }
      java2orc(r)
    } catch {
      case e: InvocationTargetException => throw new JavaException(e.getCause())
      case e: ExceptionInInitializerError => throw new JavaException(e.getCause())
      case e: InterruptedException => throw e
      case e: Exception => throw new JavaException(e)
    } finally {
      if (orc.run.RuntimeProfiler.profileRuntime) {
        orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.JavaDispatch)
      }
    }
  }
}

/** An Orc field lookup result from a Java object.
  *
  * The member could be either a method or a field.
  *
  * @author jthywiss, amp
  */
class JavaMemberProxy(@inline val theObject: Object, @inline val memberName: String, @inline val javaField: Option[JavaField]) extends InvokerMethod with AccessorValue {
  def javaClass = theObject.getClass()

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    import JavaCall._
    import orc.InvocationBehaviorUtilities._
    try {
      val argClss = args.map(valueType)
      val invocable = selectMethod(javaClass, memberName, argClss)
      new InvocableInvoker(invocable, javaClass, argClss) {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target match {
            case p: JavaMemberProxy =>
              p.javaClass == javaClass && 
                p.memberName == memberName &&
                valuesHaveType(arguments, argumentClss)
            case _ => false
          }
        }
        
        override def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          super.invokeDirect(target.asInstanceOf[JavaMemberProxy].theObject, arguments)
        }
  
        override def toString() = s"<Member Invoker>($javaClass.$memberName)"
      }
    } catch {
      case nsme: NoSuchMethodException => TargetThrowsInvoker(this, nsme)
      case mtme: MethodTypeMismatchException => TargetThrowsInvoker(this, mtme)
    }
  }

  def getAccessor(runtime: OrcRuntime, field: OrcField): Accessor = {
    val submemberName = field.name

    // In violation of JLS §10.7, arrays don't really have a length field!  Java bug 5047859
    if (memberName == "length" && submemberName == "read" && javaClass.isArray()) {
      new Accessor {
        def canGet(target: AnyRef): Boolean = {
          target match {
            case p: JavaMemberProxy if p.memberName == "length" && p.javaClass.isArray() => true
            case _ => false
          }
        }
        def get(target: AnyRef): AnyRef = {
          //Logger.finer(s"Getting field (${target.asInstanceOf[JavaMemberProxy].theObject}: $javaClass).$memberName.read")
          new JavaArrayLengthPseudofield(target.asInstanceOf[JavaMemberProxy].theObject)
        }
      }
    } else if (javaField.isEmpty) {
      new ErrorAccessor {
        @throws[NoSuchMemberException]
        def get(target: AnyRef): AnyRef = {
          throw new NoSuchMemberException(theObject, memberName)
        }
      
        def canGet(target: AnyRef): Boolean = {
          if (target.isInstanceOf[JavaMemberProxy]) {
            val p = target.asInstanceOf[JavaMemberProxy]
            p.memberName == memberName && p.javaClass == javaClass
          } else {
            false
          }
        }
      }
    } else {
      val jf = javaField.get
      new Accessor {
        def canGet(target: AnyRef): Boolean = {
          target match {
            case p: JavaMemberProxy if p.memberName == memberName && p.javaClass == javaClass => true
            case _ => false
          }
        }
        def get(target: AnyRef): AnyRef = {
          val value = jf.get(target.asInstanceOf[JavaMemberProxy].theObject)
          lazy val valueCls = value.getClass()
          //Logger.finer(s"Getting field (${target.asInstanceOf[JavaMemberProxy].theObject}: $javaClass).$memberName = $value ($jf)")
          import JavaCall._
          // TODO:PERFORMANCE: The has*Member checks on value will actually be quite expensive. However for these semantics they are required. Maybe we could change the semantics. Or maybe I've missed a way to implement it so that all reflection is JIT time constant.
          submemberName match {
            case "read" if value == null || !valueCls.hasInstanceMember("read") =>
              new JavaFieldDerefSite(target.asInstanceOf[JavaMemberProxy].theObject, jf)
            case "write" if value == null || !valueCls.hasInstanceMember("write") =>
              new JavaFieldAssignSite(target.asInstanceOf[JavaMemberProxy].theObject, jf)
            case _ if value == null =>
              throw new NoSuchMemberException(value, submemberName)
            case _ =>
              new JavaMemberProxy(value, submemberName, valueCls.getFieldOption(submemberName))
          }
        }
      }
    }
  }
  
  override def toString() = s"JavaMemberProxy(($theObject: $javaClass).$memberName)"
}

/** An Orc field lookup result from a Java class
  *
  * @author jthywiss, amp
  */
class JavaStaticMemberProxy(declaringClass: Class[_ <: java.lang.Object], memberName: String, javaField: Option[JavaField]) extends JavaMemberProxy(null, memberName, javaField) {
  override def javaClass = declaringClass
}

/** A site that will dereference a Java object's field when called
  *
  * @author jthywiss, amp
  */
case class JavaFieldDerefSite(val theObject: Object, val javaField: JavaField) extends TotalSite0 {
  def eval(): AnyRef = {
    java2orc(javaField.get(theObject))
  }
}

/** A site that will assign a value to a Java object's field when called
  *
  * @author jthywiss, amp
  */
case class JavaFieldAssignSite(val theObject: Object, val javaField: JavaField) extends TotalSite1 {
  def eval(a: AnyRef): AnyRef = {
    javaField.set(theObject, orc2java(a))
    Signal
  }
}

/** A Java array access from Orc.  Retain the index, and respond to a read or write
  *
  * @author jthywiss, amp
  */
case class JavaArrayElementProxy(@inline val theArray: AnyRef, @inline val index: Int) extends AccessorValue {
  def getAccessor(runtime: OrcRuntime, field: OrcField): Accessor = {
    field match {
      case OrcField("read") => 
        new ArrayAccessor {
          def methodInstance(theArray: AnyRef, index: Int): AnyRef = new JavaArrayDerefSite(theArray, index)
        }
      case OrcField("readnb") => 
        new ArrayAccessor {
          def methodInstance(theArray: AnyRef, index: Int): AnyRef = new JavaArrayDerefSite(theArray, index)
        }
      case OrcField("write") => 
        new ArrayAccessor {
          def methodInstance(theArray: AnyRef, index: Int): AnyRef = new JavaArrayAssignSite(theArray, index)
        }
      case OrcField(fieldname) => 
        NoSuchMemberAccessor(this, fieldname)
    }
  }
}

abstract class ArrayAccessor extends Accessor {
  def methodInstance(theArray: AnyRef, index: Int): AnyRef
  
  def canGet(target: AnyRef): Boolean = {
    target.isInstanceOf[JavaArrayElementProxy]
  }

  def get(target: AnyRef): AnyRef = {
    val JavaArrayElementProxy(theArray, index) = target
    methodInstance(theArray, index)
  }
}


/** A site that will dereference a Java array's component when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayDerefSite(@inline val theArray: AnyRef, @inline val index: Int) extends InvokerMethod with FunctionalSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      val cls = theArray.getClass
      val mh = MethodHandles.arrayElementGetter(cls).asType(MethodType.methodType(classOf[Object], classOf[Object], Integer.TYPE))
      new OnlyDirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target.isInstanceOf[JavaArrayDerefSite] && arguments.length == 0 && cls.isInstance(target.asInstanceOf[JavaArrayDerefSite].theArray)
        }
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
          val r = try {
            val self = target.asInstanceOf[JavaArrayDerefSite]
            mh.invokeExact(self.theArray, self.index)
          } finally {
            if (orc.run.RuntimeProfiler.profileRuntime) {
              orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
            }
          }
          java2orc(r)
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

/** A site that will assign a value to a Java array's component when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayAssignSite(@inline val theArray: AnyRef, @inline val index: Int) extends InvokerMethod with NonBlockingSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 1) {
      val cls = theArray.getClass
      val mh = MethodHandles.arrayElementSetter(cls)
      val componentType = Option(cls.getComponentType).getOrElse[Class[_]](classOf[AnyRef])
      new OnlyDirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target.isInstanceOf[JavaArrayAssignSite] && arguments.length == 1 && cls.isInstance(target.asInstanceOf[JavaArrayAssignSite].theArray)
        }
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          orc.run.RuntimeProfiler.traceEnter(orc.run.RuntimeProfiler.SiteImplementation)
          try {
            val self = target.asInstanceOf[JavaArrayAssignSite]
            mh.invoke(self.theArray, self.index, orc2java(arguments(0), componentType))
          } finally {
            if (orc.run.RuntimeProfiler.profileRuntime) {
              orc.run.RuntimeProfiler.traceExit(orc.run.RuntimeProfiler.SiteImplementation)
            }
          }
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

/** A site that will dereference a Java array's length when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayLengthPseudofield(val theArray: AnyRef) extends TotalSite0 {
  def eval(): AnyRef = {
    java2orc(JavaArray.getLength(theArray).asInstanceOf[AnyRef])
  }
}
