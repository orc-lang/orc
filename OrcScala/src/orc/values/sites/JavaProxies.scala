//
// JavaProxies.scala -- Scala class JavaProxy
// Project OrcScala
//
// Created by jthywiss on Jul 9, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

import scala.collection.immutable.List
import scala.language.existentials
import orc.Handle
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

/** Transforms an Orc site call to an appropriate Java invocation
  *
  * @author jthywiss
  */
object JavaCall extends Function3[Object, List[AnyRef], Handle, Boolean] {

  /* Return true if the call was successfully dispatched.
   * Return false if the call could not be dispatched.
   */
  def apply(target: Object, args: List[AnyRef], h: Handle): Boolean = {
    args match {
      case List(OrcField(memberName)) => {
        h.publish(new JavaMemberProxy(target, memberName))
        true
      }
      case List(i: BigInt) if (target.getClass.isArray) => {
        h.publish(new JavaArrayAccess(target.asInstanceOf[Array[Any]], i.toInt))
        true
      }
      // We should have boxed any java.lang.Integer, java.lang.Short, or java.lang.Byte value into BigInt
      case _ if (target.getClass.isArray) => throw new MalformedArrayAccessException(args)
      case _ if (!target.getClass.isArray) => {
        val proxy = JavaObjectProxy(target)
        if (proxy.hasMember("apply")) {
          h.publish(proxy.invoke(target, "apply", args))
          true
        } else {
          false
        }
      }
      case _ => false
    }
  }
}

/** Parent of all JavaProxy classes; provides a consistent invocation function.
  *
  * @author jthywiss
  */
abstract class JavaProxy extends Site {

  def javaClass: Class[_]

  lazy val javaClassName: String = javaClass.getCanonicalName()

  /** Does this class have a method or field of the given name? */
  def hasMember(memberName: String): Boolean =
    //TODO: Memoize!  This is expensive!
    javaClass.getMethods().exists({ _.getName().equals(memberName) }) ||
      javaClass.getFields().exists({ _.getName().equals(memberName) })

  /** Invoke a method on the given Java object of the given name with the given arguments */
  def invoke(theObject: Object, methodName: String, args: List[AnyRef]): AnyRef = {
    val unOrcWrappedArgs = args.map(orc2java(_)) // Un-wrapped from Orc's Literal, JavaObjectProxy, etc., but not Orc number conversions
    try {
      val method = try {
        chooseMethodForInvocation(javaClass, methodName, unOrcWrappedArgs map { a => { if (a != null) a.getClass() else null } })
      } catch { // Fill in "blank" exceptions with more details
        case e: java.lang.NoSuchMethodException if (e.getMessage() == null) => throw new java.lang.NoSuchMethodException(classNameAndSignatureA(methodName, unOrcWrappedArgs))
      }
      if (theObject == null && !method.isStatic) {
        throw new NullPointerException("Instance method called without a target object (i.e. non-static method called on a class)")
      }
      val finalArgs = if (method.isVarArgs) {
        // Group var args into nested array argument.
        val nNormalArgs = method.getParameterTypes().size - 1
        val (normalArgs, varArgs) = (args.take(nNormalArgs), args.drop(nNormalArgs))
        val convertedNormalArgs = (normalArgs, method.getParameterTypes()).zipped.map(orc2java(_, _))

        val varargType = method.getParameterTypes().last.getComponentType()
        val convertedVarArgs = varArgs.map(orc2java(_, varargType))
        // The vararg array needs to have the correct dynamic type so we create it using reflection. 
        val varArgArray = JavaArray.newInstance(varargType, varArgs.size).asInstanceOf[Array[Object]]
        convertedVarArgs.copyToArray(varArgArray)

        convertedNormalArgs :+ varArgArray
      } else {
        val convertedArgs = (args, method.getParameterTypes()).zipped.map(orc2java(_, _))
        convertedArgs
      }
      Logger.finer(s"Invoking Java method ${classNameAndSignature(methodName, method.getParameterTypes.toList)} with (${finalArgs.map(valueAndType).mkString(", ")})")
      java2orc(method.invoke(theObject, finalArgs.toArray))
    } catch {
      case e: InvocationTargetException => throw new JavaException(e.getCause())
      case e: InterruptedException => throw e
      case e: Exception => throw new JavaException(e)
    }
  }

  private def valueAndType(v: AnyRef): String = {
    val str = v match {
      case a: Array[_] => a.map(x => valueAndType(x.asInstanceOf[AnyRef])).mkString("Array(", ", ", ")")
      case v => v.toString
    }
    s"$str : ${v.getClass.getSimpleName}"
  }

  private def classNameAndSignature(methodName: String, argTypes: List[Class[_]]): String = {
    javaClass.getCanonicalName() + "." + methodName + "(" + argTypes.map(_.getCanonicalName()).mkString(", ") + ")"
  }

  private def classNameAndSignatureA(methodName: String, args: List[Object]): String = {
    classNameAndSignature(methodName, args.map(_.getClass()))
  }

}

/** Wrapper for a plain old Java class as an Orc site
  *
  * @author jthywiss
  */
case class JavaClassProxy(val javaClass: Class[_ <: java.lang.Object]) extends JavaProxy with TypedSite {
  // Reminder: A java.lang.Class could be a regular class, an interface, an array, or a primitive type.

  override lazy val name = javaClass.getName()

  override def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(OrcField(memberName)) => h.publish(new JavaStaticMemberProxy(javaClass, memberName))
      case _ => h.publish(invoke(null, "<init>", args))
    }
  }

  def orcType = liftJavaClassType(javaClass)

}

/** Wrapper for a plain old Java object.
  *
  * @author jthywiss
  */
case class JavaObjectProxy(val theObject: Object) extends JavaProxy with TypedSite {

  override def javaClass = theObject.getClass()

  override lazy val name = javaClass.getName()

  override def call(args: List[AnyRef], h: Handle) {
    JavaCall(theObject, args, h)
  }

  def orcType = liftJavaType(javaClass)

}

/** An Orc field lookup result from a Java object
  *
  * @author jthywiss
  */
case class JavaMemberProxy(val theObject: Object, val memberName: String) extends JavaProxy {
  // Could be a method or field.  We defer this decision until we are called.

  override lazy val name = this.getClass().getCanonicalName() + "(" + javaClassName + "." + memberName + ", " + theObject.toString() + ")"

  override def javaClass = theObject.getClass()

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(OrcField(submemberName)) => {
        // In violation of JLS §10.7, arrays don't really have a length field!  Java bug 5047859
        if (memberName.equals("length") && submemberName.equals("read") && javaClass.isArray())
          return h.publish(new JavaArrayLengthPseudofield(theObject.asInstanceOf[Array[Any]]))

        val javaField = javaClass.getField(memberName)
        h.publish(submemberName match {
          case "read" if !hasMember("read") => new JavaFieldDerefSite(theObject, javaField)
          case "write" if !hasMember("write") => new JavaFieldAssignSite(theObject, javaField)
          case _ => new JavaMemberProxy(javaField.get(theObject), submemberName)
        })
      }
      case _ => h.publish(invoke(theObject, memberName, args))
    }
  }
}

/** An Orc field lookup result from a Java class
  *
  * @author jthywiss
  */
class JavaStaticMemberProxy(declaringClass: Class[_ <: java.lang.Object], memberName: String) extends JavaMemberProxy(null, memberName) {

  override def javaClass = declaringClass

  override lazy val name = this.getClass().getCanonicalName() + "(" + javaClassName + "." + memberName + ")"

}

/** A site that will dereference a Java object's field when called
  *
  * @author jthywiss
  */
case class JavaFieldDerefSite(val theObject: Object, val javaField: JavaField) extends JavaProxy {

  override def javaClass = javaField.getDeclaringClass()

  override lazy val name = this.getClass().getCanonicalName() + "(" + javaClassName + "." + javaField.getName() + ", " + theObject + ")"

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List() => h.publish(java2orc(javaField.get(theObject)))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}

/** A site that will assign a value to a Java object's field when called
  *
  * @author jthywiss
  */
case class JavaFieldAssignSite(val theObject: Object, val javaField: JavaField) extends JavaProxy {

  override def javaClass = javaField.getDeclaringClass()

  override lazy val name = this.getClass().getCanonicalName() + "(" + javaClassName + "." + javaField.getName() + ", " + theObject + ")"

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a) => {
        javaField.set(theObject, orc2java(a))
        h.publish(Signal)
      }
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}

/** A Java array access from Orc.  Retain the index, and respond to a read or write
  *
  * @author jthywiss
  */
case class JavaArrayAccess(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override lazy val name = this.getClass().getCanonicalName() + "(element " + index + " of " + theArray + ")"

  override def javaClass = theArray.getClass()

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(OrcField("read")) => h.publish(new JavaArrayDerefSite(theArray, index))
      case List(OrcField("readnb")) => h.publish(new JavaArrayDerefSite(theArray, index))
      case List(OrcField("write")) => h.publish(new JavaArrayAssignSite(theArray, index))
      case List(OrcField(fieldname)) => throw new NoSuchMethodException(fieldname + " in Ref") //A "white lie"
      case List(v) => throw new ArgumentTypeMismatchException(0, "message", v.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.length) //Is there a better exception to throw?
    }
  }
}

/** A site that will dereference a Java array's component when called
  *
  * @author jthywiss
  */
case class JavaArrayDerefSite(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName() + "(element " + index + " of " + theArray + ")"

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List() => h.publish(java2orc(theArray(index).asInstanceOf[AnyRef]))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}

/** A site that will assign a value to a Java array's component when called
  *
  * @author jthywiss
  */
case class JavaArrayAssignSite(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName() + "(element " + index + " of " + theArray + ")"

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a) => {
        theArray(index) = orc2java(a)
        h.publish(Signal)
      }
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}

/** A site that will dereference a Java array's component when called
  *
  * @author jthywiss
  */
case class JavaArrayLengthPseudofield(val theArray: Array[Any]) extends JavaProxy {

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName() + "(" + theArray + ")"

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List() => h.publish(java2orc(theArray.length.asInstanceOf[AnyRef]))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}
