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

import scala.collection.immutable.List
import scala.language.existentials
import orc.{ Handle, Invoker, DirectInvoker, Accessor, OnlyDirectInvoker }
import orc.values.Signal
import orc.values.{ Field => OrcField }
import orc.run.Logger
import orc.error.NotYetImplementedException
import orc.error.runtime.JavaException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.MalformedArrayAccessException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.{ Member => JavaMember }
import java.lang.reflect.{ Constructor => JavaConstructor }
import java.lang.reflect.{ Method => JavaMethod }
import java.lang.reflect.{ Field => JavaField }
import java.lang.reflect.{ Array => JavaArray }
import java.lang.reflect.Modifier
import orc.values.sites.OrcJavaCompatibility._
import orc.compile.typecheck.Typeloader._
import orc.error.runtime.NoSuchMemberException
import orc.util.ArrayExtensions.{ Array1, Array0 }
import orc.error.runtime.{ UncallableValueException, HaltException, MethodTypeMismatchException }
import java.lang.NoSuchMethodException
import orc.run.Logger
import orc.InvocationBehaviorUtilities
import orc.values.Field
import orc.error.runtime.NoSuchMemberException
import orc.NoSuchMemberAccessor
import orc.UncallableValueInvoker

/** Due to the way dispatch is handled we cannot pass true wrappers back into Orc. They
  * would interfere with any call to which they were passed as an argument.
  *
  * Instead any object needs to be passed back bare and then handled again in getInvoker
  * and getAccessor.
  *
  * This does not apply to objects which "wrap" members and similar since those are not
  * values with types in Java and hence cannot appear as argument types in other calls.
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

    import InvocationBehaviorUtilities._

    @throws[NoSuchMethodException]
    def getMemberInvokerTypeDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          cls == target.getClass() && valuesHaveType(arguments, argClss)
        }
      }
    }

    @throws[NoSuchMethodException]
    def getMemberInvokerValueDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          cls == target && valuesHaveType(arguments, argClss)
        }
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
      }
    }
  }

  def getInvoker(target: AnyRef, args: Array[AnyRef]): Option[Invoker] = {
    val targetCls = target.getClass()
    val argClss = args.map(InvocationBehaviorUtilities.valueType)
    (target, args) match {
      // ARRAYS
      case (_, Array1(_: BigInt)) if targetCls.isArray() => {
        Some(new OnlyDirectInvoker {
          def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
            target.getClass().isArray() && arguments.length == 1 && arguments(0).isInstanceOf[BigInt]
          }
          @throws[HaltException]
          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            new JavaArrayElementProxy(target.asInstanceOf[Array[Any]], arguments(0).asInstanceOf[BigInt].toInt)
          }
        })
      }
      // We should have boxed any java.lang.Integer, java.lang.Short, or java.lang.Byte value into BigInt
      case _ if targetCls.isArray() => 
        //throw new MalformedArrayAccessException(args)
        Some(UncallableValueInvoker(target))

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
  }

  def getAccessor(target: AnyRef, f: OrcField): Option[Accessor] = {
    target match {
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
abstract class InvocableInvoker(val invocable: Invocable, val targetCls: Class[_], val argumentClss: Array[Class[_]]) extends Invoker {
  import JavaCall._
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean

  @throws[UncallableValueException]
  def invoke(h: Handle, theObject: AnyRef, arguments: Array[AnyRef]): Unit = {
    orc.run.core.Tracer.traceJavaCall(h)
    try {
      if (theObject == null && !invocable.isStatic) {
        throw new NullPointerException("Instance method called without a target object (i.e. non-static method called on a class)")
      }
      val finalArgs = if (invocable.isVarArgs) {        
        // Group var args into nested array argument.
        val nNormalArgs = invocable.getParameterTypes.size - 1
        val (normalArgs, varArgs) = (arguments.take(nNormalArgs), arguments.drop(nNormalArgs))
        val convertedNormalArgs = (normalArgs, invocable.getParameterTypes).zipped.map(orc2java(_, _))

        val varargType = invocable.getParameterTypes.last.getComponentType()
        val convertedVarArgs = varArgs.map(orc2java(_, varargType))
        // The vararg array needs to have the correct dynamic type so we create it using reflection.
        val varArgArray = JavaArray.newInstance(varargType, varArgs.size).asInstanceOf[Array[Object]]
        convertedVarArgs.copyToArray(varArgArray)

        convertedNormalArgs :+ varArgArray
      } else {
        // TODO: PERFORMANCE: It may be worth it to replace all these java collections calls with some optimized loops. I know it's terrible, but this is on the path for EVERY java call
        // IT might be good to optimize the vararg case above as well, but it's much less of a hot path and it would be harder to optimizer.
        val convertedArgs = (arguments, invocable.getParameterTypes).zipped.map(orc2java(_, _))
        convertedArgs
      }
      Logger.finer(s"Invoking Java method ${classNameAndSignature(targetCls, invocable.getName, invocable.getParameterTypes.toList)} with (${finalArgs.map(valueAndType).mkString(", ")})")
      h.publish(java2orc(invocable.invoke(theObject, finalArgs.toArray)))
    } catch {
      case e: InvocationTargetException => throw new JavaException(e.getCause())
      case e: InterruptedException => throw e
      case e: Exception => throw new JavaException(e)
    } finally {
      orc.run.core.Tracer.traceJavaReturn(h)
    }
  }
}

/** An Orc field lookup result from a Java object.
  *
  * The member could be either a method or a field.
  *
  * @author jthywiss, amp
  */
class JavaMemberProxy(val theObject: Object, val memberName: String, val javaField: Option[JavaField]) extends InvokerMethod with AccessorValue {
  def javaClass = theObject.getClass()

  def getInvoker(args: Array[AnyRef]): Invoker = {
    import JavaCall._
    import InvocationBehaviorUtilities._
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
        override def invoke(h: Handle, theObject: AnyRef, arguments: Array[AnyRef]): Unit = {
          super.invoke(h, theObject.asInstanceOf[JavaMemberProxy].theObject, arguments)
        }
  
        override def toString() = s"<Member Invoker>($javaClass.$memberName)"
      }
    } catch {
      case _: NoSuchMethodException | _: MethodTypeMismatchException =>
        UncallableValueInvoker(this, memberName)
    }
  }

  def getAccessor(field: Field): Accessor = {
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
          Logger.finer(s"Getting field ($theObject: $javaClass).$memberName.read")
          new JavaArrayLengthPseudofield(theObject.asInstanceOf[Array[Any]])
        }
      }
    } else if (javaField.isEmpty) {
        NoSuchMemberAccessor(this, memberName)
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
          val value = jf.get(theObject)
          lazy val valueCls = value.getClass()
          Logger.finer(s"Getting field ($theObject: $javaClass).$memberName = $value ($jf)")
          import JavaCall._
          // TODO:PERFORMANCE: The hasMember checks on value will actually be quite expensive. However for these semantics they are required. Maybe we could change the semantics. Or maybe I've missed a way to implement it so that all reflection is JIT time constant.
          submemberName match {
            case "read" if value == null || !valueCls.hasInstanceMember("read") =>
              new JavaFieldDerefSite(theObject, jf)
            case "write" if value == null || !valueCls.hasInstanceMember("write") =>
              new JavaFieldAssignSite(theObject, jf)
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
case class JavaArrayElementProxy(val theArray: Array[Any], val index: Int) extends AccessorValue {
  def getAccessor(field: Field): Accessor = {
    field match {
      case OrcField("read") => 
        new ArrayAccessor {
          def methodInstance(theArray: Array[Any], index: Int): AnyRef = new JavaArrayDerefSite(theArray, index)
        }
      case OrcField("readnb") => 
        new ArrayAccessor {
          def methodInstance(theArray: Array[Any], index: Int): AnyRef = new JavaArrayDerefSite(theArray, index)
        }
      case OrcField("write") => 
        new ArrayAccessor {
          def methodInstance(theArray: Array[Any], index: Int): AnyRef = new JavaArrayAssignSite(theArray, index)
        }
      case OrcField(fieldname) => 
        NoSuchMemberAccessor(this, fieldname)
    }
  }
}

abstract class ArrayAccessor extends Accessor {
  def methodInstance(theArray: Array[Any], index: Int): AnyRef
  
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
case class JavaArrayDerefSite(val theArray: Array[Any], val index: Int) extends TotalSite0 {
  def eval(): AnyRef = {
    java2orc(theArray(index).asInstanceOf[AnyRef])
  }
}

/** A site that will assign a value to a Java array's component when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayAssignSite(val theArray: Array[Any], val index: Int) extends TotalSite1 {
  def eval(a: AnyRef): AnyRef = {
    theArray(index) = orc2java(a)
    Signal
  }
}

/** A site that will dereference a Java array's length when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayLengthPseudofield(val theArray: Array[Any]) extends TotalSite0 {
  def eval(): AnyRef = {
    java2orc(theArray.length.asInstanceOf[AnyRef])
  }
}
