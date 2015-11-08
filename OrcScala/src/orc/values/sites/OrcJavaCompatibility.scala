//
// OrcJavaCompatibility.scala -- Scala object OrcJavaCompatibility
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 8, 2010.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.WrappedArray
import scala.language.{ existentials, reflectiveCalls }
import java.lang.reflect.{ Constructor => JavaConstructor }
import java.lang.reflect.{ Method => JavaMethod }
import java.lang.reflect.Modifier
import orc.run.Logger
import scala.collection.generic.Shrinkable

/**
  * @author jthywiss, dkitchin
  */
object OrcJavaCompatibility {

  /** Java to Orc value conversion */
  def java2orc(javaValue: Object): AnyRef = javaValue match {
    case _: java.lang.Void => orc.values.Signal
    case i: java.lang.Byte => BigInt(i.byteValue)
    case i: java.lang.Short => BigInt(i.shortValue)
    case i: java.lang.Integer => BigInt(i.intValue)
    case i: java.lang.Long => BigInt(i.longValue)
    case f: java.lang.Float => BigDecimal(f.floatValue.toDouble)
    case f: java.lang.Double => BigDecimal(f.doubleValue)
    case s: java.lang.String => s
    case b: java.lang.Boolean => b
    case null => null
    case v => v
  }

  /** Convenience method for <code>orc2java(orcValue, classOf[Object])</code> */
  def orc2java(orcValue: AnyRef): Object = orc2java(orcValue, classOf[Object])

  /** Orc to Java value conversion, given an expected Java type */
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

  // Java Method and Constructor do NOT have a decent supertype, so we wrap them here
  // to at least share an common invocation method.  Ugh.
  abstract class Invocable { def getParameterTypes(): Array[java.lang.Class[_]]; def isStatic: Boolean; def invoke(obj: Object, args: Array[Object]): Object }

  object Invocable {
    def apply(wrapped: java.lang.reflect.Member): Invocable = {
      wrapped match {
        case meth: JavaMethod => new InvocableMethod(meth)
        case ctor: JavaConstructor[_] => new InvocableCtor(ctor)
        case _ => throw new IllegalArgumentException("Invocable can only wrap a Method or a Constructor")
      }
    }
  }

  case class InvocableMethod(method: JavaMethod) extends Invocable {
    def getParameterTypes(): Array[java.lang.Class[_]] = method.getParameterTypes
    def isStatic = Modifier.isStatic(method.getModifiers())
    def invoke(obj: Object, args: Array[Object]): Object = method.invoke(obj, args: _*)
  }

  case class InvocableCtor(ctor: JavaConstructor[_]) extends Invocable {
    def getParameterTypes(): Array[java.lang.Class[_]] = ctor.getParameterTypes
    def isStatic = true
    def invoke(obj: Object, args: Array[Object]): Object = ctor.newInstance(args: _*).asInstanceOf[Object]
  }

  /** Given a method name and arg list, find the correct Method to call, per JLS §15.12.2's rules */
  def chooseMethodForInvocation(targetClass: Class[_], memberName: String, argTypes: List[Class[_]]): Invocable = {
    //Phase 0: Identify Potentially Applicable Methods
    //A member method is potentially applicable to a method invocation if and only if all of the following are true:
    //* The name of the member is identical to the name of the method in the method invocation.
    //* The member is accessible (§6.6) to the class or interface in which the method invocation appears.
    //* The arity of the member is lesser or equal to the arity of the method invocation.
    //* If the member is a variable arity method with arity n, the arity of the method invocation is greater or equal to n-1.
    //* If the member is a fixed arity method with arity n, the arity of the method invocation is equal to n.
    //* If the method invocation includes explicit type parameters, and the member is a generic method, then the number of actual type parameters is equal to the number of formal type parameters.
    type JavaMethodOrCtor = java.lang.reflect.Member { def getParameterTypes(): Array[java.lang.Class[_]]; def isVarArgs(): Boolean }
    val methodName = if ("<init>".equals(memberName)) targetClass.getName() else memberName
    val ms: Traversable[JavaMethodOrCtor] = if ("<init>".equals(memberName)) targetClass.getConstructors() else getAccessibleMethods(targetClass)
    val potentiallyApplicableMethods = ms.filter({ m =>
      m.getName().equals(methodName) &&
        // Modifier PUBLIC (for method & class) already handled
        // Modifier ABSTRACT is handled later.
        (m.getParameterTypes().size == argTypes.size ||
          m.isVarArgs() && m.getParameterTypes().size - 1 <= argTypes.size)
    })
    Logger.finest(memberName + " potentiallyApplicableMethods=" + potentiallyApplicableMethods.mkString("{", ", ", "}"))
    if (potentiallyApplicableMethods.isEmpty) {
      throw new NoSuchMethodException("No public " + methodName + " with " + argTypes.size + " arguments in " + targetClass.getName() + " [[OrcWiki:NoSuchMethodException]]")
    }

    //Phase 1: Identify Matching Arity Methods Applicable by Subtyping
    val phase1Results = potentiallyApplicableMethods.filter({ m =>
      !m.isVarArgs() &&
        m.getParameterTypes().corresponds(argTypes)({ (fp, arg) => isApplicable(fp, arg, false) })
    })
    Logger.finest(memberName + " phase1Results=" + phase1Results.mkString("{", ", ", "}"))
    if (phase1Results.nonEmpty) {
      return Invocable(mostSpecificMethod(phase1Results))
    }

    //Phase 2: Identify Matching Arity Methods Applicable by Method Invocation Conversion
    val phase2Results = potentiallyApplicableMethods.filter({ m =>
      !m.isVarArgs() &&
        m.getParameterTypes().corresponds(argTypes)({ (fp, arg) => isApplicable(fp, arg, true) })
    })
    Logger.finest(memberName + " phase2Results=" + phase2Results.mkString("{", ", ", "}"))
    if (phase2Results.nonEmpty) {
      return Invocable(mostSpecificMethod(phase2Results))
    }

    //Phase 3: Identify Applicable Variable Arity Methods
    //FIXME:TODO: Implement var arg calls

    // No match
    throw new orc.error.runtime.MethodTypeMismatchException(memberName, targetClass);
  }

  /** Given a type (class or interface), returns a sequence of declared or
    * inherited public methods in public types.  Overridden methods are
    * excluded, leaving only the overriding method.  This methods is like
    * java.lang.Class.getMethods, except this handles public methods in
    * inaccessible classes correctly, and doesn't erroneously "inherit"
    * overridden methods (per JLS § 6.4.3).
    */
  def getAccessibleMethods(typeToSearch: Class[_]): Seq[JavaMethod] = {
    val accessibleMethods = new ArrayBuffer[JavaMethod]()
    if (Modifier.isPublic(typeToSearch.getModifiers()))
      accessibleMethods ++= (typeToSearch.getDeclaredMethods() filter (m => Modifier.isPublic(m.getModifiers())))

    val accessibleInheritedMethods = new ArrayBuffer[JavaMethod]()
    /* All interfaces and their methods are always accessible */
    typeToSearch.getInterfaces() foreach (ifc => accessibleInheritedMethods ++= WrappedArray.make(ifc.getMethods()))
    /* (Note: OK to use java.lang.Class.getMethods for interfaces) */

    val superclass = typeToSearch.getSuperclass()
    if (superclass != null) {
      val superMethods = getAccessibleMethods(superclass).toBuffer
      /* If typeToSearch inherits an interface that has a method
       * that has been already implemented in a superclass, ignore
       * the interface's re-declaration of that method. */
      superMethods foreach (removeEqSignature(accessibleInheritedMethods, _))
      accessibleInheritedMethods.prependAll(superMethods)
    }

    /* remove typeToSearch's overrides from inherited methods */
    accessibleMethods foreach (removeEqSignature(accessibleInheritedMethods, _))

    accessibleMethods.sizeHint(accessibleMethods.size + accessibleInheritedMethods.size)
    accessibleInheritedMethods foreach (m => if (!accessibleMethods.contains(m)) accessibleMethods += m)

    accessibleMethods
  }

  private def removeEqSignature(methSeq: Shrinkable[JavaMethod] with Traversable[JavaMethod], signatureToRemove: JavaMethod) {
    methSeq --= (methSeq filter (m =>
      m.getName == signatureToRemove.getName &&
        m.getParameterTypes.sameElements(signatureToRemove.getParameterTypes)))
  }

  /** Given a formal param type and an actual arg type, determine if the method applies per JLS §15.12.2.2/3 rules */
  private def isApplicable(formalParamType: Class[_], actualArgType: Class[_], allowConversion: Boolean): Boolean = {
    // allowConversion refers to method invocation conversion (JLS §5.3), which is one of:
    // 0. identity conversion
    // 1. widening primitive conversion (JLS §5.1.2) -- one of 19 conversions
    // 2. widening reference conversion (JLS §5.1.5) -- i.e., normal subtyping
    // 3. boxing conversion (one of 8) (JLS §5.1.7) optionally followed by widening reference conversion
    // 4. unboxing conversion (one of 8) (JLS §5.1.8) optionally followed by a widening primitive conversion
    // "Method invocation conversions specifically do not include the implicit narrowing of integer constants which is part of assignment conversion"

    if (!formalParamType.isPrimitive() && actualArgType == null) {
      true // Null type is a subtype of all reference types (JLS §4.10)
    } else if (!allowConversion || (!formalParamType.isPrimitive() && !actualArgType.isPrimitive())) {
      formalParamType.isAssignableFrom(actualArgType)
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

  // Java's reference types for primitive types
  private val booleanRefClass = classOf[java.lang.Boolean]
  private val byteRefClass = classOf[java.lang.Byte]
  private val charRefClass = classOf[java.lang.Character]
  private val shortRefClass = classOf[java.lang.Short]
  private val intRefClass = classOf[java.lang.Integer]
  private val longRefClass = classOf[java.lang.Long]
  private val floatRefClass = classOf[java.lang.Float]
  private val doubleRefClass = classOf[java.lang.Double]

  // Orc's numeric types
  val orcIntegralClass = classOf[BigInt]
  val orcFloatingPointClass = classOf[BigDecimal]

  /** Java boxing conversion per JLS §5.1.7 */
  def box(primType: Class[_]): Class[_] = {
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

  /** Java unboxing conversion per JLS §5.1.8 */
  def unbox(refType: Class[_]): Class[_] = {
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
  def isPrimWidenable(fromPrimType: Class[_], toPrimType: Class[_]): Boolean = {
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

  /** "true" if an Orc value conversion applies */
  def isOrcJavaNumConvertable(fromType: Class[_], toType: Class[_]): Boolean = {
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

  /** Most specific method per JLS §15.12.2.5 */
  private def mostSpecificMethod[M <: { def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]]; def getModifiers(): Int }](methods: Traversable[M]): M = {
    //FIXME:TODO: Implement var arg calls
    val maximallySpecificMethods =
      methods.foldLeft(List[M]())({ (prevMostSpecific: List[M], nextMethod: M) =>
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
    Logger.finest("maximallySpecificMethods=" + maximallySpecificMethods.mkString("{", ", ", "}"))
    if (maximallySpecificMethods.length == 1) {
      return maximallySpecificMethods.head
    } else if (maximallySpecificMethods.isEmpty) {
      throw new java.lang.NoSuchMethodException() //TODO: throw a MethodTypeMismatchException instead
    } else {
      val concreteMethods = maximallySpecificMethods.filter({ m => !Modifier.isAbstract(m.getModifiers()) })
      concreteMethods.length match {
        case 1 => return concreteMethods.head
        case 0 => return maximallySpecificMethods.head //pick arbitrarily per JLS §15.12.2.5
        case _ => throw new orc.error.runtime.AmbiguousInvocationException(concreteMethods.map(_.toString).toArray)
      }
    }
  }

  /** Left is more or equally specific than right (per JLS §15.12.2.5) */
  def isEqOrMoreSpecific(left: { def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]] }, right: { def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]] }): Boolean = {
    if (isSameArgumentTypes(left, right)) {
      left.getDeclaringClass().getClasses().contains(right.getDeclaringClass()) // An override is more specific than super
    } else {
      left.getParameterTypes().corresponds(right.getParameterTypes())({ (l, r) => isJavaSubtypeOf(l, r) })
    }
  }

  /** Java subtyping per JLS §4.10 */
  def isJavaSubtypeOf(left: java.lang.Class[_], right: java.lang.Class[_]): Boolean = {
    (left == right) || (right.isAssignableFrom(left)) || isPrimWidenable(left, right) ||
      (left.isArray && right.isArray && isJavaSubtypeOf(left.getComponentType, right.getComponentType))
  }

  /** Same argument types per JLS §8.4.2 */
  private def isSameArgumentTypes(left: { def getParameterTypes(): Array[java.lang.Class[_]] }, right: { def getParameterTypes(): Array[java.lang.Class[_]] }): Boolean = {
    left.getParameterTypes().length == right.getParameterTypes().length &&
      left.getParameterTypes().corresponds(right.getParameterTypes())({ (l, r) => r.isAssignableFrom(l) && l.isAssignableFrom(r) })
  }

}
