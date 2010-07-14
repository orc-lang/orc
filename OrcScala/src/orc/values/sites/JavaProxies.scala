//
// JavaProxies.scala -- Scala class JavaProxy
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 9, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

import scala.collection.immutable.List
import orc.TokenAPI
import orc.oil.nameless.Type // FIXME: Typechecker should operate on named types instead
import orc.values.Signal
import orc.values.{Field => OrcField}
import orc.run.Logger
import orc.error.NotYetImplementedException
import orc.error.runtime.JavaException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.MessageNotUnderstoodException
import orc.error.runtime.MalformedArrayAccessException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.{Member => JavaMember}
import java.lang.reflect.{Constructor => JavaConstructor}
import java.lang.reflect.{Method => JavaMethod}
import java.lang.reflect.{Field => JavaField}
import java.lang.reflect.Modifier


/**
 * Transforms an Orc site call to an appropriate Java invocation
 *
 * @author jthywiss
 */
object JavaCall extends Function3[Object, List[AnyRef], TokenAPI, Unit] {
  def apply(target: Object, args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(OrcField(memberName)) => callingToken.publish(new JavaMemberProxy(target, memberName))
      case _ if (!target.getClass.isArray) => callingToken.publish(JavaObjectProxy(target).invoke(target, "apply", args))
      case List(i: BigInt) if (target.getClass.isArray) => callingToken.publish(new JavaArrayAccess(target.asInstanceOf[Array[Any]], i.toInt))
      // We should have boxed any java.lang.Integer, java.lang.Short, or java.lang.Byte value into BigInt
      case _ if (target.getClass.isArray) => throw new MalformedArrayAccessException(args)
    }
  }
}


/**
 * Parent of all JavaProxy classes; provides consistent conversion and invocation functions.
 *
 * @author jthywiss
 */
abstract class JavaProxy extends Site {

  def javaClass: Class[_]

  lazy val javaClassName: String = javaClass.getCanonicalName()

  def java2orc(javaValue: Object): AnyRef = javaValue match {
    case _: java.lang.Void => orc.values.Signal
    case i: java.lang.Byte => BigInt(i.byteValue)
    case i: java.lang.Short => BigInt(i.shortValue)
    case i: java.lang.Integer => BigInt(i.intValue)
    case i: java.lang.Long => BigInt(i.longValue)
    case f: java.lang.Float => BigDecimal(f.floatValue)
    case f: java.lang.Double => BigDecimal(f.doubleValue)
    case s: java.lang.String => s
    case b: java.lang.Boolean => b
    case null => null
    case v => v 
  }

  def orc2java(orcValue: AnyRef): Object = orc2java(orcValue, classOf[Object])

  def orc2java(orcValue: AnyRef, expectedType: Class[_]): Object =
    orcValue match {
      case i: BigInt => {
        expectedType match {
          case `byteRefClass` | java.lang.Byte.TYPE => i.toByte.asInstanceOf[java.lang.Byte]
          case `shortRefClass` | java.lang.Short.TYPE => i.toShort.asInstanceOf[java.lang.Short]
          case `intRefClass` | java.lang.Integer.TYPE => i.toInt.asInstanceOf[java.lang.Integer]
          case `longRefClass` | java.lang.Long.TYPE => i.toLong.asInstanceOf[java.lang.Long]
          case `floatRefClass` | java.lang.Float.TYPE => i.toFloat.asInstanceOf[java.lang.Float]
          case `doubleRefClass` | java.lang.Double.TYPE => i.toDouble.asInstanceOf[java.lang.Double]
          case _ => i
        }
      }
      case f: BigDecimal => {
        expectedType match {
          case `floatRefClass` | java.lang.Float.TYPE => f.toFloat.asInstanceOf[java.lang.Float]
          case `doubleRefClass` | java.lang.Double.TYPE => f.toDouble.asInstanceOf[java.lang.Double]
          case _ => f
        }
      }
      case JavaObjectProxy(j) => j
      case _ => orcValue.asInstanceOf[Object]
    }

  def hasMember(memberName: String): Boolean =
    //TODO: Memoize!  This is expensive!
    javaClass.getMethods().exists({_.getName().equals(memberName)}) ||
        javaClass.getFields().exists({_.getName().equals(memberName)})

  def invoke(theObject: Object, methodName: String, args: List[AnyRef]): AnyRef = {
    val unOrcWrappedArgs = args.map(orc2java(_)) // Un-wrapped from Orc's Literal, JavaObjectProxy, etc., but not Orc number conversions
    val method = try { 
      try {
        chooseMethodForInvocation(methodName, unOrcWrappedArgs)
      } catch { // Fill in "blank" exceptions with more details
        case e: java.lang.NoSuchMethodException if (e.getMessage() == null) => throw new java.lang.NoSuchMethodException(classNameAndSignatureA(methodName, unOrcWrappedArgs))
      }
    } catch {
      case e: InvocationTargetException => throw new JavaException(e.getCause())
      case e => throw new JavaException(e)
    }
    val convertedArgs = (args, method.getParameterTypes()).zipped.map(orc2java(_, _)).toArray
    if (theObject == null && !method.isStatic) {
      throw new NullPointerException("Instance method called without a target object (i.e. non-static method called on a class)")
    }
    Logger.finer("Invoking Java method "+classNameAndSignature(methodName, method.getParameterTypes.toList))
    java2orc(method.invoke(theObject, convertedArgs))
  }

  private def classNameAndSignature(methodName: String, argTypes: List[Class[_]]): String = {
    javaClass.getCanonicalName()+"."+methodName+"("+argTypes.map(_.getCanonicalName()).mkString(", ")+")"
  }
  private def classNameAndSignatureA(methodName: String, args: List[Object]): String = {
    classNameAndSignature(methodName, args.map(_.getClass()))
  }

  // Java Method and Constructor do NOT have a decent supertype, so we wrap them here
  // to at least share an common invocation method.  Ugh.
  abstract class Invocable {def getParameterTypes(): Array[java.lang.Class[_]]; def isStatic: Boolean; def invoke(obj: Object, args: Array[Object]): Object}
  object Invocable {
    def apply(wrapped: java.lang.reflect.Member): Invocable = {
      wrapped match {
        case meth: JavaMethod => new InvocableMethod(meth)
        case ctor: JavaConstructor[_] => new InvocableCtor(ctor)
        case _ => throw new IllegalArgumentException("Invocable can only wrap a Method or a Constructor")
      }
    }
  }
  class InvocableMethod(method: JavaMethod) extends Invocable {
    def getParameterTypes(): Array[java.lang.Class[_]] = method.getParameterTypes
    def isStatic = Modifier.isStatic(method.getModifiers())
    def invoke(obj: Object, args: Array[Object]): Object = method.invoke(obj, args: _*)
  }
  class InvocableCtor(ctor: JavaConstructor[_]) extends Invocable {
    def getParameterTypes(): Array[java.lang.Class[_]] = ctor.getParameterTypes
    def isStatic = true
    def invoke(obj: Object, args: Array[Object]): Object = ctor.newInstance(args: _*).asInstanceOf[Object]
  }

  def chooseMethodForInvocation(memberName: String, args: List[Object]): Invocable = {
    //Phase 0: Identify Potentially Applicable Methods
    //A member method is potentially applicable to a method invocation if and only if all of the following are true:
    //* The name of the member is identical to the name of the method in the method invocation.
    //* The member is accessible (§6.6) to the class or interface in which the method invocation appears.
    //* The arity of the member is lesser or equal to the arity of the method invocation.
    //* If the member is a variable arity method with arity n, the arity of the method invocation is greater or equal to n-1.
    //* If the member is a fixed arity method with arity n, the arity of the method invocation is equal to n.
    //* If the method invocation includes explicit type parameters, and the member is a generic method, then the number of actual type parameters is equal to the number of formal type parameters.
    type JavaMethodOrCtor = java.lang.reflect.Member {def getParameterTypes(): Array[java.lang.Class[_]]; def isVarArgs(): Boolean}
    val methodName = if ("<init>".equals(memberName)) javaClass.getName() else memberName
    val ms: Array[JavaMethodOrCtor] = if ("<init>".equals(memberName)) javaClass.getConstructors().asInstanceOf[Array[JavaMethodOrCtor]] else javaClass.getMethods().asInstanceOf[Array[JavaMethodOrCtor]]
    val potentiallyApplicableMethods = ms.filter({m => 
        m.getName().equals(methodName) &&
        Modifier.isPublic(m.getModifiers()) &&
        // Modfier ABSTRACT is handled later.
        (m.getParameterTypes().size == args.size ||
         m.isVarArgs() && m.getParameterTypes().size-1 <= args.size)})
    Logger.finest(memberName+" potentiallyApplicableMethods="+potentiallyApplicableMethods.mkString("{", ", ", "}"))

    //Phase 1: Identify Matching Arity Methods Applicable by Subtyping
    val phase1Results = potentiallyApplicableMethods.filter( { m =>
      !m.isVarArgs() &&
      m.getParameterTypes().corresponds(args)({(fp, arg) => isApplicable(fp, arg, false)})
    } )
    Logger.finest(memberName+" phase1Results="+phase1Results.mkString("{", ", ", "}"))
    if (phase1Results.nonEmpty) {
      return Invocable(mostSpecificMethod(phase1Results.toList))
    }
    
    //Phase 2: Identify Matching Arity Methods Applicable by Method Invocation Conversion
    val phase2Results = potentiallyApplicableMethods.filter( { m =>
      !m.isVarArgs() &&
      m.getParameterTypes().corresponds(args)({(fp, arg) => isApplicable(fp, arg, true)})
    } )
    Logger.finest(memberName+" phase2Results="+phase2Results.mkString("{", ", ", "}"))
    if (phase2Results.nonEmpty) {
      return Invocable(mostSpecificMethod(phase2Results.toList))
    }

    //Phase 3: Identify Applicable Variable Arity Methods
    //FIXME:TODO: Implement var arg calls
    
    // No match
    throw new orc.error.runtime.MethodTypeMismatchException(memberName);
  }

  private def isApplicable(formalParamType: Class[_], actualArg: Object, allowConversion: Boolean): Boolean = {
    // allowConversion refers to method invocation conversion (JLS §5.3), which is one of:
    // 0. identity conversion
    // 1. widening primitive conversion (JLS §5.1.2) -- one of 19 conversions
    // 2. widening reference conversion (JLS §5.1.5) -- i.e., normal subtyping
    // 3. boxing conversion (one of 8) (JLS §5.1.7) optionally followed by widening reference conversion
    // 4. unboxing conversion (one of 8) (§5.1.8) optionally followed by a widening primitive conversion
    // "Method invocation conversions specifically do not include the implicit narrowing of integer constants which is part of assignment conversion"

    //TODO: Consider converting OrcList, etc. to java List, etc.
    val actualArgType = actualArg.getClass()
    if (!allowConversion || (!formalParamType.isPrimitive() && !actualArgType.isPrimitive())) {
      formalParamType.isInstance(actualArg)
    } else if (formalParamType.isPrimitive() && actualArgType.isPrimitive()) {
      isPrimWidenable(actualArgType, formalParamType)
    } else {
      if (formalParamType.isPrimitive()) { /* && actualArg is NOT primitive */
        val unboxedType = unbox(actualArgType)
        isPrimWidenable(unboxedType, formalParamType) || isOrcJavaNumConvertable(actualArgType, formalParamType)
      } else { /* formalParamType is NOT primitive && actualArg IS primitive */
        val boxedType = box(actualArgType)
        formalParamType.isAssignableFrom(boxedType)
      }
    }
  }

  private val booleanRefClass = classOf[java.lang.Boolean]
  private val byteRefClass = classOf[java.lang.Byte]
  private val charRefClass = classOf[java.lang.Character]
  private val shortRefClass = classOf[java.lang.Short]
  private val intRefClass = classOf[java.lang.Integer]
  private val longRefClass = classOf[java.lang.Long]
  private val floatRefClass = classOf[java.lang.Float]
  private val doubleRefClass = classOf[java.lang.Double]
  
  private val orcIntegralClass = classOf[BigInt]
  private val orcFloatingPointClass = classOf[BigDecimal]

  private def box(primType: Class[_]): Class[_] = {
    primType match {
      case java.lang.Boolean.TYPE => booleanRefClass
      case java.lang.Byte.TYPE => byteRefClass
      case java.lang.Character.TYPE => charRefClass
      case java.lang.Short.TYPE => shortRefClass
      case java.lang.Integer.TYPE => intRefClass
      case java.lang.Long.TYPE => longRefClass
      case java.lang.Float.TYPE => floatRefClass
      case java.lang.Double.TYPE => doubleRefClass
      case _ => primType
    }
  }

  private def unbox(refType: Class[_]): Class[_] = {
    refType match {
      case `booleanRefClass` => java.lang.Boolean.TYPE
      case `byteRefClass` => java.lang.Byte.TYPE
      case `charRefClass` => java.lang.Character.TYPE
      case `shortRefClass` => java.lang.Short.TYPE
      case `intRefClass` => java.lang.Integer.TYPE
      case `longRefClass` => java.lang.Long.TYPE
      case `floatRefClass` => java.lang.Float.TYPE
      case `doubleRefClass` => java.lang.Double.TYPE
      case _ => refType
    }
  }

  /** "true" for an identity conversion (JLS §5.1.1) or a widening primitive conversion (JLS §5.1.2) */
  private def isPrimWidenable(fromPrimType: Class[_], toPrimType: Class[_]): Boolean = {
    (fromPrimType == toPrimType) || (fromPrimType match {
      case java.lang.Byte.TYPE => toPrimType == java.lang.Short.TYPE || toPrimType == java.lang.Integer.TYPE || toPrimType == java.lang.Long.TYPE || toPrimType == java.lang.Float.TYPE || toPrimType == java.lang.Double.TYPE
      case java.lang.Short.TYPE => toPrimType == java.lang.Integer.TYPE || toPrimType == java.lang.Long.TYPE || toPrimType == java.lang.Float.TYPE || toPrimType == java.lang.Double.TYPE
      case java.lang.Character.TYPE => toPrimType == java.lang.Integer.TYPE || toPrimType == java.lang.Long.TYPE || toPrimType == java.lang.Float.TYPE || toPrimType == java.lang.Double.TYPE
      case java.lang.Integer.TYPE => toPrimType == java.lang.Long.TYPE || toPrimType == java.lang.Float.TYPE || toPrimType == java.lang.Double.TYPE
      case java.lang.Long.TYPE => toPrimType == java.lang.Float.TYPE || toPrimType == java.lang.Double.TYPE 
      case java.lang.Float.TYPE => toPrimType == java.lang.Double.TYPE
      case _ => false
    })
  }

  private def isOrcJavaNumConvertable(fromType: Class[_], toType: Class[_]): Boolean = {
    toType match {
      case `byteRefClass` => orcIntegralClass.isAssignableFrom(fromType)
      case `shortRefClass` => orcIntegralClass.isAssignableFrom(fromType)
      case `intRefClass` => orcIntegralClass.isAssignableFrom(fromType)
      case `longRefClass` => orcIntegralClass.isAssignableFrom(fromType)
      case `floatRefClass` => orcIntegralClass.isAssignableFrom(fromType) || orcFloatingPointClass.isAssignableFrom(fromType)
      case `doubleRefClass` => orcIntegralClass.isAssignableFrom(fromType) || orcFloatingPointClass.isAssignableFrom(fromType)
      case java.lang.Byte.TYPE => orcIntegralClass.isAssignableFrom(fromType)
      case java.lang.Short.TYPE => orcIntegralClass.isAssignableFrom(fromType)
      case java.lang.Integer.TYPE => orcIntegralClass.isAssignableFrom(fromType)
      case java.lang.Long.TYPE => orcIntegralClass.isAssignableFrom(fromType) 
      case java.lang.Float.TYPE => orcIntegralClass.isAssignableFrom(fromType) || orcFloatingPointClass.isAssignableFrom(fromType)
      case java.lang.Double.TYPE => orcIntegralClass.isAssignableFrom(fromType) || orcFloatingPointClass.isAssignableFrom(fromType)
      case _ => false
    }
  }

  private def mostSpecificMethod[M <: {def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]]; def getModifiers(): Int}](methods: List[M]): M = {
    //FIXME:TODO: Implement var arg calls
    val maximallySpecificMethods = 
      methods.foldLeft(List[M]())({(prevMostSpecific: List[M], nextMethod: M) =>
        if (prevMostSpecific.isEmpty) {
          List(nextMethod)
        } else { 
          if (isEqOrMoreSpecific(nextMethod, prevMostSpecific.head)) {
            if (isEqOrMoreSpecific(prevMostSpecific.head, nextMethod)) { // equally specific, add to list
              prevMostSpecific :+ nextMethod
            } else { // nextMethod strictly more specific
              List(nextMethod)
            }
          } else { // nextMethod > prevMostSpecific
            prevMostSpecific
          }
        }
      })
    Logger.finest("maximallySpecificMethods="+maximallySpecificMethods.mkString("{", ", ", "}"))
    if (maximallySpecificMethods.length == 1) {
      return maximallySpecificMethods.head
    } else if (maximallySpecificMethods.length == 0) {
      throw new java.lang.NoSuchMethodException()  //TODO: throw a MethodTypeMismatchException instead
    } else {
      val concreteMethods = maximallySpecificMethods.filter({m => !Modifier.isAbstract(m.getModifiers())})
      concreteMethods.length match {
        case 1 => return concreteMethods.head
        case 0 => return maximallySpecificMethods.head //pick arbitrarily per JLS §15.12.2.5
        case _ => throw new orc.error.runtime.AmbiguousInvocationException(concreteMethods.map(_.toString).toArray)
      }
    }
  }
  
  /** left is more or equally specific than right */
  private def isEqOrMoreSpecific(left: {def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]]}, right: {def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]]}): Boolean = {
    if (isSameArgumentTypes(left, right)) {
      left.getDeclaringClass().getClasses().contains(right.getDeclaringClass())  // An override is more specific than super
    } else {
      left.getParameterTypes().corresponds(right.getParameterTypes())({(l,r) => isJavaSubtypeOf(l, r)})
    }
  }

  private def isJavaSubtypeOf(left: java.lang.Class[_], right: java.lang.Class[_]): Boolean = {
    (left == right) || (right.isAssignableFrom(left)) || isPrimWidenable(left, right) ||
    (left.isArray && right.isArray && isJavaSubtypeOf(left.getComponentType, right.getComponentType))
  }

  private def isSameArgumentTypes(left: {def getParameterTypes(): Array[java.lang.Class[_]]}, right: {def getParameterTypes(): Array[java.lang.Class[_]]}): Boolean = {
    left.getParameterTypes().length == right.getParameterTypes().length &&
    left.getParameterTypes().corresponds(right.getParameterTypes())({(l,r) => r.isAssignableFrom(l) && l.isAssignableFrom(r)})
  }
}


/**
 * Wrapper for a plain old Java class as an Orc site
 *
 * @author jthywiss
 */
case class JavaClassProxy(val javaClass: Class[Object]) extends JavaProxy {
  // Reminder: A java.lang.Class could be a regular class, an interface, an array, or a primitive type.  

  override lazy val name = javaClass.getName()

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(OrcField("?")) => throw new NotYetImplementedException("MatchProxy not implemented yet") //TODO:FIXME: Implement this -- publish(new MatchProxy(javaClass))
      case List(OrcField(memberName)) => callingToken.publish(new JavaStaticMemberProxy(javaClass, memberName))
      case _ => callingToken.publish(invoke(null, "<init>", args))
    }
  }
}


/**
 * Wrapper for a plain old Java object.
 *
 * @author jthywiss
 */
case class JavaObjectProxy(val theObject: Object) extends JavaProxy {

  override lazy val name = theObject.getClass().getName()

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theObject.getClass()

  override def call(args: List[AnyRef], callingToken: TokenAPI) = JavaCall(theObject, args, callingToken)
}


/**
 * An Orc field lookup result from a Java object
 *
 * @author jthywiss
 */
case class JavaMemberProxy(val theObject: Object, val memberName: String) extends JavaProxy {
  // Could be a method or field.  We defer this decision until we are called.

  override lazy val name = this.getClass().getCanonicalName()+"("+javaClassName+"."+memberName+", "+theObject.toString()+")"

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theObject.getClass()

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(OrcField(submemberName)) => {
        // In violation of JLS 10.7, arrays don't really have a length field!  Java bug 5047859
        if (memberName.equals("length") && submemberName.equals("read") && javaClass.isArray())
          return callingToken.publish(new JavaArrayLengthPseudofield(theObject.asInstanceOf[Array[Any]]))

        val javaField = javaClass.getField(memberName)
        callingToken.publish(submemberName match {
          case "read" if !hasMember("read") => new JavaFieldDerefSite(theObject, javaField)
          case "write" if !hasMember("write") => new JavaFieldAssignSite(theObject, javaField)
          case _ => new JavaMemberProxy(javaField.get(theObject), submemberName)
        })
      }
      case _ => callingToken.publish(invoke(theObject, memberName, args))
    }
  }
}


/**
 * An Orc field lookup result from a Java class
 *
 * @author jthywiss
 */
class JavaStaticMemberProxy(declaringClass: Class[_], memberName: String) extends JavaMemberProxy(null, memberName) {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = declaringClass

  override lazy val name = this.getClass().getCanonicalName()+"("+javaClassName+"."+memberName+")"

}


/**
 * A site that will dereference a Java object's field when called
 *
 * @author jthywiss
 */
case class JavaFieldDerefSite(val theObject: Object, val javaField: JavaField) extends JavaProxy {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = javaField.getDeclaringClass()

  override lazy val name = this.getClass().getCanonicalName()+"("+javaClassName+"."+javaField.getName()+", "+theObject+")"

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List() => callingToken.publish(java2orc(javaField.get(theObject)))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}


/**
 * A site that will assign a value to a Java object's field when called
 *
 * @author jthywiss
 */
case class JavaFieldAssignSite(val theObject: Object, val javaField: JavaField) extends JavaProxy {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = javaField.getDeclaringClass()

  override lazy val name = this.getClass().getCanonicalName()+"("+javaClassName+"."+javaField.getName()+", "+theObject+")"

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(a) => {
        javaField.set(theObject, orc2java(a))
        callingToken.publish(Signal)
      }
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}


/**
 * A Java array access from Orc.  Retain the index, and respond to a read or write 
 *
 * @author jthywiss
 */
case class JavaArrayAccess(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override lazy val name = this.getClass().getCanonicalName()+"(element "+index+" of "+theArray+")"

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theArray.getClass()

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(OrcField("read")) => callingToken.publish(new JavaArrayDerefSite(theArray, index))
      case List(OrcField("write")) => callingToken.publish(new JavaArrayAssignSite(theArray, index))
      case List(OrcField(fieldname)) => throw new MessageNotUnderstoodException(fieldname)
      case List(v) => throw new ArgumentTypeMismatchException(0, "message", v.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.length)
    }
  }
}


/**
 * A site that will dereference a Java array's component when called
 *
 * @author jthywiss
 */
case class JavaArrayDerefSite(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName()+"(element "+index+" of "+theArray+")"

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List() => callingToken.publish(java2orc(theArray(index).asInstanceOf[AnyRef]))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}


/**
 * A site that will assign a value to a Java array's component when called
 *
 * @author jthywiss
 */
case class JavaArrayAssignSite(val theArray: Array[Any], val index: Int) extends JavaProxy {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName()+"(element "+index+" of "+theArray+")"

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List(a) => {
        theArray(index) = orc2java(a)
        callingToken.publish(Signal)
      }
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

}


/**
 * A site that will dereference a Java array's component when called
 *
 * @author jthywiss
 */
case class JavaArrayLengthPseudofield(val theArray: Array[Any]) extends JavaProxy {

  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this

  override def javaClass = theArray.getClass().getComponentType()

  override lazy val name = this.getClass().getCanonicalName()+"("+theArray+")"

  def call(args: List[AnyRef], callingToken: TokenAPI) {
    args match {
      case List() => callingToken.publish(java2orc(theArray.length.asInstanceOf[AnyRef]))
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

}
