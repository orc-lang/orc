//
// JavaProxies.scala -- Scala class JavaProxy
// Project OrcScala
//
// Created by jthywiss on Jul 9, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import java.lang.invoke.{ MethodHandles, MethodHandle, MethodType }
import java.lang.reflect.{ Array => JavaArray, Field => JavaField, InvocationTargetException }

import orc.{ Accessor, ErrorAccessor, Invoker, DirectInvoker, OrcRuntime }
import orc.error.runtime.{ HaltException, JavaException, MalformedArrayAccessException, MethodTypeMismatchException, NoSuchMemberException, RuntimeTypeException }
import orc.util.ArrayExtensions.Array1
import orc.values.{ Field => OrcField, Signal, NoSuchMemberAccessor, HasMembers }
import orc.values.sites.OrcJavaCompatibility.{ Invocable, chooseMethodForInvocation, java2orc, orc2java }

/* Due to the way dispatch is handled we cannot pass true wrappers back into Orc. They
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
  val mhLookup = MethodHandles.publicLookup()

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

    @throws[NoSuchMethodException]
    def getMemberInvokerTypeDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val cls = this.cls
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvokeTarget(target: AnyRef): Boolean = {
          cls == target.getClass()
        }
        override def toString() = s"<Member Invoker>($cls.$methodName)"
      }
    }

    @throws[NoSuchMethodException]
    def getMemberInvokerValueDirected(methodName: String, argClss: Array[Class[_]]): Invoker = {
      val cls = this.cls
      val invocable = selectMethod(cls, methodName, argClss)
      new InvocableInvoker(invocable, cls, argClss) {
        def canInvokeTarget(target: AnyRef): Boolean = {
          cls == target
        }
        override def toString() = s"<Member Invoker>($cls.$methodName)"
      }
    }

    def getMemberAccessor(_memberName: String): Accessor = {
      val memberName = _memberName.intern()
      val cls = this.cls
      val javaField = cls.getFieldOption(memberName)
      new SimpleAccessor {
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
      val cls = this.cls
      val javaField = cls.getFieldOption(memberName)
      new SimpleAccessor {
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
          Some(new DirectInvoker {
            def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
              target.getClass().isArray() && arguments.length == 1 && arguments(0).isInstanceOf[java.lang.Long]
            }
            @throws[HaltException]
            def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
              orc.run.StopWatches.implementation {
                new JavaArrayElementProxy(target, arguments(0).asInstanceOf[java.lang.Long].intValue())
              }
            }
            override def toString(): String = s"<Array Index Invoker Long>@${super.toString}"
          })
        }
        case (_, Array1(_: BigInt | _: java.lang.Integer)) if targetCls.isArray() => {
          Some(new DirectInvoker {
            def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
              target.getClass().isArray() && arguments.length == 1 &&
              (arguments(0).isInstanceOf[BigInt] || arguments(0).isInstanceOf[java.lang.Long] || arguments(0).isInstanceOf[java.lang.Integer])
            }
            @throws[HaltException]
            def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
              orc.run.StopWatches.implementation {
                new JavaArrayElementProxy(target, arguments(0).asInstanceOf[Number].intValue())
              }
            }
            override def toString(): String = s"<Array Index Invoker non-Long>@${super.toString}"
          })
        }
        // We should have boxed any java.lang.Integer, java.lang.Short, or java.lang.Byte value into BigInt
        case _ if targetCls.isArray() =>
          Some(new ThrowsInvoker(target, args, new MalformedArrayAccessException(args)))

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
        Some(new ThrowsInvoker(target, args, e))
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

trait SimpleAccessor extends Accessor

/**
  * @author jthywiss, amp
  */
sealed abstract class InvocableInvoker(
    @inline final val invocable: Invocable,
    @inline final val targetCls: Class[_],
    @inline final val argumentClss: Array[Class[_]]) extends DirectInvoker {

  final def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    import orc.values.sites.InvocationBehaviorUtilities._
    canInvokeTarget(target) && valuesHaveType(arguments, argumentClss)
  }

  /** As in Invoker, except that it must only contain constant length loops (with
   *  respect to invocable, etc) and cannot use recursion. This requirement applies
   *  to all transitively called functions as well. This rules out most of the
   *  Scala collections library and any calls to unknown code.
   */
  def canInvokeTarget(target: AnyRef): Boolean

  final val mh = {
    val m = invocable.toMethodHandle
    m.asFixedArity().
      asSpreader(classOf[Array[Object]], m.`type`().parameterCount() - 1).
      asType(MethodType.methodType(classOf[Object], classOf[Object], classOf[Array[Object]]))
  }

  final val boxedReturnType = OrcJavaCompatibility.box(invocable.returnType)

  def getRealTarget(theObject: AnyRef) = theObject

  final def invokeDirect(inputObject: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    val theObject = getRealTarget(inputObject)

    orc.run.StopWatches.javaCall {
      try {
        if (theObject == null && !invocable.isStatic) {
          throw new NullPointerException("Instance method called without a target object (i.e. non-static method called on a class)")
        }
        val finalArgs = if (invocable.isVarArgs) {
          // FIXME: Var args appear to be broken in PorcE. Not sure why. Not needed for papers. Need to get back and fix it.

          // TODO: PERFORMANCE: Optimize this like the else block.
          // Group var args into nested array argument.
          val nNormalArgs = invocable.parameterTypes.length - 1
          val (normalArgs, varArgs) = (arguments.take(nNormalArgs), arguments.drop(nNormalArgs))
          val convertedNormalArgs = (normalArgs, invocable.parameterTypes).zipped.map(orc2java(_, _))

          val varargType = invocable.parameterTypes(nNormalArgs).getComponentType()
          val convertedVarArgs = varArgs.map(orc2java(_, varargType))
          // The vararg array needs to have the correct dynamic type so we create it using reflection.
          val varArgArray = JavaArray.newInstance(varargType, varArgs.size).asInstanceOf[Array[Object]]
          convertedVarArgs.copyToArray(varArgArray)

          (convertedNormalArgs :+ varArgArray).toArray
        } else {
          // IT might be good to optimize the vararg case above as well, but it's much less of a hot path and it would be harder to optimizer.
          val end = arguments.length
          var i = 0
          while(i < end) {
            arguments(i) = orc2java(arguments(i), invocable.parameterTypes(i))
            i += 1
          }
          arguments
        }
        // Some Java Proxy code is partially evaluated and Logging breaks that: Logger.finer(s"Invoking Java method ${JavaCall.classNameAndSignature(targetCls, invocable.getName, invocable.parameterTypes.toList)} with (${finalArgs.map(JavaCall.valueAndType).mkString(", ")})")
        val r = orc.run.StopWatches.javaImplementation {
          // The returnType cast does not add any static information, but it enables runtime optimizations in Graal.
          boxedReturnType.cast(mh.invokeExact(theObject, finalArgs)).asInstanceOf[AnyRef]
        }
        java2orc(r)
      } catch {
        case e: InvocationTargetException => throw new JavaException(e.getCause())
        case e: ExceptionInInitializerError => throw new JavaException(e.getCause())
        case e: InterruptedException => throw e
        case e: Exception => throw new JavaException(e)
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
class JavaMemberProxy(@inline val theObject: Object, @inline val memberName: String, @inline val javaField: Option[JavaField]) extends Site with HasMembers {
  def javaClass = theObject.getClass()

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    import JavaCall._
    import orc.values.sites.InvocationBehaviorUtilities._
    try {
      val memberName = this.memberName
      val javaClass = this.javaClass
      val argClss = args.map(valueType)
      val invocable = selectMethod(javaClass, memberName, argClss)
      new InvocableInvoker(invocable, javaClass, argClss) {
        def canInvokeTarget(target: AnyRef): Boolean = {
          target match {
            case p: JavaMemberProxy =>
              p.javaClass == javaClass &&
                (p.memberName eq memberName)
            case _ => false
          }
        }

        override def getRealTarget(target: AnyRef) = target.asInstanceOf[JavaMemberProxy].theObject

        override def toString() = s"<Member Invoker>($javaClass.$memberName)"
      }
    } catch {
      case nsme: NoSuchMethodException => new ThrowsInvoker(this, args, nsme)
      case mtme: MethodTypeMismatchException => new ThrowsInvoker(this, args, mtme)
    }
  }

  def getAccessor(runtime: OrcRuntime, field: OrcField): Accessor = {
    val submemberName = field.name

    // In violation of JLS §10.7, arrays don't really have a length field!  Java bug 5047859
    if (memberName == "length" && submemberName == "read" && javaClass.isArray()) {
      new SimpleAccessor {
        def canGet(target: AnyRef): Boolean = {
          target match {
            case p: JavaMemberProxy if (p.memberName eq "length") && p.javaClass.isArray() => true
            case _ => false
          }
        }
        def get(target: AnyRef): AnyRef = {
          // Some Java Proxy code is partially evaluated and Logging breaks that: Logger.finer(s"Getting field (${target.asInstanceOf[JavaMemberProxy].theObject}: $javaClass).$memberName.read")
          new JavaArrayLengthPseudofield(target.asInstanceOf[JavaMemberProxy].theObject)
        }
      }
    } else if (javaField.isEmpty) {
      val memberName = this.memberName
      val javaClass = this.javaClass
      new ErrorAccessor with SimpleAccessor {
        @throws[NoSuchMemberException]
        def get(target: AnyRef): AnyRef = {
          throw new NoSuchMemberException(javaClass, memberName)
        }

        def canGet(target: AnyRef): Boolean = {
          if (target.isInstanceOf[JavaMemberProxy]) {
            val p = target.asInstanceOf[JavaMemberProxy]
            (p.memberName eq memberName) && p.javaClass == javaClass
          } else {
            false
          }
        }
      }
    } else {
      val jf = javaField.get
      val memberName = this.memberName
      val javaClass = this.javaClass
      new Accessor {
        def canGet(target: AnyRef): Boolean = {
          target match {
            case p: JavaMemberProxy if (p.memberName eq memberName) && p.javaClass == javaClass => true
            case _ => false
          }
        }
        def get(target: AnyRef): AnyRef = {
          val value = jf.get(target.asInstanceOf[JavaMemberProxy].theObject)
          def valueCls = value.getClass()
          // Some Java Proxy code is partially evaluated and Logging breaks that: Logger.finer(s"Getting field (${target.asInstanceOf[JavaMemberProxy].theObject}: $javaClass).$memberName = $value ($jf)")
          import JavaCall._
          // TODO:PERFORMANCE: The has*Member checks on value will actually be quite expensive. However for these semantics they are required. Maybe we could change the semantics. Or maybe I've missed a way to implement it so that all reflection is JIT time constant.
          if ((submemberName eq "read") && (value == null || !valueCls.hasInstanceMember("read"))) {
            new JavaFieldDerefSite(target.asInstanceOf[JavaMemberProxy].theObject, jf)
          } else if ((submemberName eq "write") && (value == null || !valueCls.hasInstanceMember("write"))) {
            new JavaFieldAssignSite(target.asInstanceOf[JavaMemberProxy].theObject, jf)
          } else if (value == null) {
            throw new NoSuchMemberException(value, submemberName)
          } else {
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
class JavaStaticMemberProxy(declaringClass: Class[_ <: java.lang.Object], _memberName: String, javaField: Option[JavaField]) extends JavaMemberProxy(null, _memberName, javaField) with Serializable {
  override def javaClass = declaringClass
  override def toString() = s"JavaStaticMemberProxy($javaClass.$memberName)"

  @throws(classOf[java.io.ObjectStreamException])
  protected def writeReplace(): AnyRef = {
      new JavaStaticMemberProxyMarshalingReplacement(declaringClass, memberName, javaField)
  }

}

protected case class JavaStaticMemberProxyMarshalingReplacement(declaringClass: Class[_ <: java.lang.Object], memberName: String, javaField: Option[JavaField]) {
  @throws(classOf[java.io.ObjectStreamException])
  protected def readResolve(): AnyRef = new JavaStaticMemberProxy(declaringClass, memberName, javaField)
}

object JavaFieldDerefSite {
  final class Invoker(val cls: Class[_], val mh: MethodHandle) extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target.isInstanceOf[JavaFieldDerefSite] &&
      arguments.length == 0 &&
      (cls == null ||
      cls.isInstance(target.asInstanceOf[JavaFieldDerefSite].theObject))
    }
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.javaCall {
        val r = orc.run.StopWatches.javaImplementation {
          val self = target.asInstanceOf[JavaFieldDerefSite]
          mh.invokeExact(self.theObject)
        }
        java2orc(r)
      }
    }
  }
}

/** A site that will dereference a Java object's field when called
  *
  * @author jthywiss, amp
  */
case class JavaFieldDerefSite(@inline val theObject: Object, @inline val javaField: JavaField) extends Site with FunctionalSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    import MethodHandles._
    if (args.length == 0) {
      val cls = if (theObject == null) null else theObject.getClass
      val mhRaw = JavaCall.mhLookup.unreflectGetter(javaField)
      val mh = if (theObject == null)
        dropArguments(mhRaw.asType(MethodType.methodType(classOf[Object])), 0, classOf[Object])
      else
        mhRaw.asType(MethodType.methodType(classOf[Object], classOf[Object]))
      new JavaFieldDerefSite.Invoker(cls, mh)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

object JavaFieldAssignSite {
  final class Invoker(val cls: Class[_], val mh: MethodHandle, val componentType: Class[_]) extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target.isInstanceOf[JavaFieldAssignSite] &&
      arguments.length == 1 &&
      (cls == null ||
      cls.isInstance(target.asInstanceOf[JavaFieldAssignSite].theObject))
    }
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.javaCall {
        val v = orc2java(arguments(0), componentType)
        orc.run.StopWatches.javaImplementation {
          val self = target.asInstanceOf[JavaFieldAssignSite]
          mh.invokeExact(self.theObject, v) : Unit
          Signal
        }
      }
    }
  }
}

/** A site that will assign a value to a Java object's field when called
  *
  * @author jthywiss, amp
  */
case class JavaFieldAssignSite(@inline val theObject: Object, @inline val javaField: JavaField) extends Site with NonBlockingSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    import MethodHandles._
    if (args.length == 1) {
      val cls = if (theObject == null) null else theObject.getClass
      val mhRaw = JavaCall.mhLookup.unreflectSetter(javaField)
      val mh = if (theObject == null)
        dropArguments(mhRaw.asType(MethodType.methodType(Void.TYPE, classOf[Object])), 0, classOf[Object])
      else
        mhRaw.asType(MethodType.methodType(Void.TYPE, classOf[Object], classOf[Object]))
      new JavaFieldAssignSite.Invoker(cls, mh, javaField.getType)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

/** A Java array access from Orc.  Retain the index, and respond to a read or write
  *
  * @author jthywiss, amp
  */
case class JavaArrayElementProxy(@inline val theArray: AnyRef, @inline val index: Int) extends HasMembers {
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

abstract class ArrayAccessor extends SimpleAccessor {
  def methodInstance(theArray: AnyRef, index: Int): AnyRef

  def canGet(target: AnyRef): Boolean = {
    target.isInstanceOf[JavaArrayElementProxy]
  }

  def get(target: AnyRef): AnyRef = {
    val JavaArrayElementProxy(theArray, index) = target
    methodInstance(theArray, index)
  }
}


object JavaArrayDerefSite {
  final class Invoker(val cls: Class[_], val mh: MethodHandle) extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target.isInstanceOf[JavaArrayDerefSite] && arguments.length == 0 && cls.isInstance(target.asInstanceOf[JavaArrayDerefSite].theArray)
    }
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.javaCall {
        val r = orc.run.StopWatches.javaImplementation {
          val self = target.asInstanceOf[JavaArrayDerefSite]
          mh.invokeExact(self.theArray, self.index)
        }
        java2orc(r)
      }
    }
  }
}

/** A site that will dereference a Java array's component when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayDerefSite(@inline val theArray: AnyRef, @inline val index: Int) extends Site with FunctionalSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      val cls = theArray.getClass
      val mh = MethodHandles.arrayElementGetter(cls).asType(MethodType.methodType(classOf[Object], classOf[Object], Integer.TYPE))
      new JavaArrayDerefSite.Invoker(cls, mh)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

object JavaArrayAssignSite {
  final class Invoker(val cls: Class[_], val mh: MethodHandle, val componentType: Class[_]) extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target.isInstanceOf[JavaArrayAssignSite] && arguments.length == 1 && cls.isInstance(target.asInstanceOf[JavaArrayAssignSite].theArray)
    }
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.javaCall {
        val v = orc2java(arguments(0), componentType)
        orc.run.StopWatches.javaImplementation {
          val self = target.asInstanceOf[JavaArrayAssignSite]
          mh.invokeExact(self.theArray, self.index, v) : Unit
          Signal
        }
      }
    }
  }
}

/** A site that will assign a value to a Java array's component when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayAssignSite(@inline val theArray: AnyRef, @inline val index: Int) extends Site with NonBlockingSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 1) {
      val cls = theArray.getClass
      val mh = MethodHandles.arrayElementSetter(cls).asType(MethodType.methodType(Void.TYPE, classOf[Object], Integer.TYPE, classOf[Object]))
      val componentType = Option(cls.getComponentType).getOrElse[Class[_]](classOf[AnyRef])
      new JavaArrayAssignSite.Invoker(cls, mh, componentType)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

/** A site that will dereference a Java array's length when called
  *
  * @author jthywiss, amp
  */
case class JavaArrayLengthPseudofield(val theArray: AnyRef) extends Site with FunctionalSite {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      val cls = theArray.getClass
      new JavaArrayLengthPseudofield.Invoker(cls)
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

object JavaArrayLengthPseudofield {
  final class Invoker(val cls: Class[_]) extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target.isInstanceOf[JavaArrayLengthPseudofield] &&
      arguments.length == 0 &&
      cls.isInstance(target.asInstanceOf[JavaArrayLengthPseudofield].theArray)
    }
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.javaCall {
        val r = orc.run.StopWatches.javaImplementation {
          val self = target.asInstanceOf[JavaArrayLengthPseudofield]
          JavaArray.getLength(self.theArray).asInstanceOf[AnyRef]
        }
        java2orc(r)
      }
    }
  }
}
