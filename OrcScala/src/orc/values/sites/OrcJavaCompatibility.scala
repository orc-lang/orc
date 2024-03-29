//
// OrcJavaCompatibility.scala -- Scala object OrcJavaCompatibility
// Project OrcScala
//
// Created by dkitchin on Dec 8, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
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
import orc.values.NumericsConfig
import scala.collection.generic.Shrinkable
import java.util.concurrent.ConcurrentHashMap
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/** @author jthywiss, dkitchin
  */
object OrcJavaCompatibility {

  /** Java to Orc value conversion */
  def java2orc(javaValue: Object): AnyRef = {
    def fromInteger(i: Number): AnyRef = {
      if (NumericsConfig.preferLong)
        // Prefer limited precision
        i.longValue.asInstanceOf[AnyRef]
      else
        // Normal (prefer arbitrary precision)
        BigInt(i.longValue)
    }

    def fromFloat(f: Number): AnyRef = {
      if (NumericsConfig.preferDouble)
        // Prefer limited precision
        f.doubleValue.asInstanceOf[AnyRef]
      else
        // Normal (prefer arbitrary precision)
        BigDecimal(f.doubleValue)
    }

    def fromNonNumeric(): AnyRef = javaValue match {
      // FIXME: This is wrong: case _: java.lang.Void => orc.values.Signal
      // The goal was to convert void to signal. The problem is that Void is never actually used as a value. This needs to be based on the TYPE of the method return.
      case v => v
    }

    javaValue match {
      case i: java.lang.Byte => fromInteger(i)
      case i: java.lang.Short => fromInteger(i)
      case i: java.lang.Integer => fromInteger(i)
      case i: java.lang.Long => fromInteger(i)
      case f: java.lang.Float => fromFloat(f)
      case f: java.lang.Double => fromFloat(f)
      case _ => fromNonNumeric()
    }
  }

  /** Convenience method for <code>orc2java(orcValue, classOf[Object])</code> */
  def orc2java(orcValue: AnyRef): Object = orc2java(orcValue, classOf[Object])

  /** Utility methods for orc2java handling any primitive numeric expectedType. */
  private def orc2javaAsNumber(i: Number, expectedType: Class[_]): Object = {
      expectedType match {
        case `byteRefClass` | java.lang.Byte.TYPE => i.byteValue
        case `shortRefClass` | java.lang.Short.TYPE => i.shortValue
        case `intRefClass` | java.lang.Integer.TYPE => i.intValue
        case `longRefClass` | java.lang.Long.TYPE => i.longValue
        case `floatRefClass` | java.lang.Float.TYPE => i.floatValue
        case `doubleRefClass` | java.lang.Double.TYPE => i.doubleValue
        case _ => i
      }
    }.asInstanceOf[Object]

  /** Utility methods for orc2java handling primitive floating-point numeric expectedType. */
  private def orc2javaAsFloat(f: Number, expectedType: Class[_]): Object = {
      expectedType match {
        case `floatRefClass` | java.lang.Float.TYPE => f.floatValue
        case `doubleRefClass` | java.lang.Double.TYPE => f.doubleValue
        case _ => f
      }
    }.asInstanceOf[Object]

  /** Orc to Java value conversion, given an expected Java type */
  def orc2java(orcValue: AnyRef, expectedType: Class[_]): Object = {
    orcValue match {
      case f: BigDecimal => orc2javaAsFloat(f, expectedType)
      case f: java.lang.Double => orc2javaAsFloat(f, expectedType)
      case f: java.lang.Float => orc2javaAsFloat(f, expectedType)

      case i: BigInt => orc2javaAsNumber(i, expectedType)
      case i: java.lang.Number => orc2javaAsNumber(i, expectedType)

      case _ => orcValue
    }
  }

  // TODO: Ideally orc2javaAsFixedPrecision would not be needed or would be deduplicated.
  /** Variant of orc2java which does not handle arbitrary precision numbers.
    *
    * This is needed by PorcE since this method does not call into any recursive or
    * iterative functions, so they can be partially evaluated by Graal.
    */
  def orc2javaAsFixedPrecision(orcValue: AnyRef, expectedType: Class[_]): Object = {
    orcValue match {
      case f: java.lang.Double => orc2javaAsFloat(f, expectedType)
      case f: java.lang.Float => orc2javaAsFloat(f, expectedType)

      case i: java.lang.Number => orc2javaAsNumber(i, expectedType)

      case _ => orcValue
    }
  }

  // Java Method and Constructor do NOT have a decent supertype, so we wrap them here
  // to at least share an common invocation method.  Ugh.
  sealed abstract class Invocable {
    val parameterTypes: Array[java.lang.Class[_]]
    val returnType: java.lang.Class[_]
    val isStatic: Boolean
    val isVarArgs: Boolean
    val name: String
    def executableMember: java.lang.reflect.Executable

    final def declaringClass = executableMember.getDeclaringClass

    def invoke(obj: Object, args: Array[Object]): Object

    def toMethodHandle: MethodHandle
  }

  object Invocable {
    def apply(wrapped: java.lang.reflect.Member): Invocable = {
      wrapped match {
        case meth: JavaMethod => new InvocableMethod(meth)
        case ctor: JavaConstructor[_] => new InvocableCtor(ctor)
        case _ => throw new IllegalArgumentException("Invocable can only wrap a Method or a Constructor")
      }
    }
  }

  val methodLookup = MethodHandles.publicLookup()

  final case class InvocableMethod(method: JavaMethod) extends Invocable {
    val parameterTypes: Array[java.lang.Class[_]] = method.getParameterTypes
    val returnType: java.lang.Class[_] = method.getReturnType
    val isStatic = Modifier.isStatic(method.getModifiers())
    val isVarArgs = method.isVarArgs()
    val name: String = method.getName()
    def executableMember = method

    def invoke(obj: Object, args: Array[Object]): Object = method.invoke(obj, args: _*)

    def toMethodHandle = if (isStatic) {
      MethodHandles.dropArguments(methodLookup.unreflect(method), 0, classOf[Object])
    } else {
      methodLookup.unreflect(method)
    }
  }

  final case class InvocableCtor(ctor: JavaConstructor[_]) extends Invocable {
    val parameterTypes: Array[java.lang.Class[_]] = ctor.getParameterTypes
    val returnType: java.lang.Class[_] = ctor.getDeclaringClass
    val isStatic = true
    val isVarArgs = ctor.isVarArgs()
    val name: String = ctor.getName()
    def executableMember = ctor

    def invoke(obj: Object, args: Array[Object]): Object = ctor.newInstance(args: _*).asInstanceOf[Object]

    def toMethodHandle = {
      MethodHandles.dropArguments(methodLookup.unreflectConstructor(ctor), 0, classOf[Object])
    }
  }

  // TODO: This cache will be less and less useful as we move to cached invokers. We should consider removing it to save space and cache maintenance costs.
  val methodSelectionCache = new ConcurrentHashMap[(Class[_], String, Seq[Class[_]]), Invocable]()

  def chooseMethodForInvocation(targetClass: Class[_], memberName: String, argTypes: Array[Class[_]]): Invocable = {
    val key = (targetClass, memberName, argTypes.toSeq)
    methodSelectionCache.get(key) match {
      case null => {
        val inv = chooseMethodForInvocationSlow(targetClass, memberName, argTypes)
        methodSelectionCache.putIfAbsent(key, inv)
        inv
      }
      case inv => inv
    }
  }

  /** Given a method name and arg list, find the correct Method to call, per JLS §15.12.2's rules */
  def chooseMethodForInvocationSlow(targetClass: Class[_], memberName: String, argTypes: Array[Class[_]]): Invocable = {
    Logger.finest(s"$memberName target=$targetClass argTypes=(${argTypes.mkString(", ")})")
    //Phase 0: Identify Potentially Applicable Methods
    //A member method is potentially applicable to a method invocation if and only if all of the following are true:
    //* The name of the member is identical to the name of the method in the method invocation.
    //* The member is accessible (§6.6) to the class or interface in which the method invocation appears.
    //* The arity of the member is lesser or equal to the arity of the method invocation.
    //* If the member is a variable arity method with arity n, the arity of the method invocation is greater or equal to n-1.
    //* If the member is a fixed arity method with arity n, the arity of the method invocation is equal to n.
    //* If the method invocation includes explicit type parameters, and the member is a generic method, then the number of actual type parameters is equal to the number of formal type parameters.

    try {
      // FIXME: REmove this use of structural types and replace with use of Invocable from above.
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
      val phase3Results = potentiallyApplicableMethods.filter({ m =>
        if (m.isVarArgs()) {
          val normalParams :+ varargParam = m.getParameterTypes().toSeq
          assert(varargParam.isArray())
          // Check that all normal params match
          val normalParamsMatch = normalParams.corresponds(argTypes.take(normalParams.size))((fp, arg) => isApplicable(fp, arg, true))
          // Check that each arg that will go in the vararg list matches the vararg array type
          val varargParamsMatch = argTypes.drop(normalParams.size).forall((arg) => isApplicable(varargParam.getComponentType(), arg, true))
          normalParamsMatch && varargParamsMatch
        } else false
      })
      Logger.finest(memberName + " phase3Results=" + phase3Results.mkString("{", ", ", "}"))
      if (phase3Results.nonEmpty) {
        return Invocable(mostSpecificMethod(phase3Results))
      }
    } catch {
      case e: java.lang.NoSuchMethodException =>
        throw new orc.error.runtime.MethodTypeMismatchException(argTypes, memberName, targetClass)
    }

    // No match
    throw new orc.error.runtime.MethodTypeMismatchException(argTypes, memberName, targetClass)
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

  // Orc's numeric types
  def isOrcIntegralClass(cls: Class[_]) = {
    val isBigInt = classOf[BigInt].isAssignableFrom(cls)
    if (NumericsConfig.preferLong)
      classOf[java.lang.Long].isAssignableFrom(cls) || isBigInt
    else
      isBigInt
  }
  def isOrcFloatingPointClass(cls: Class[_]) = {
    val isBigDecimal = classOf[BigDecimal].isAssignableFrom(cls)
    if (NumericsConfig.preferDouble)
      classOf[java.lang.Double].isAssignableFrom(cls) || isBigDecimal
    else
      isBigDecimal
  }


  /** "true" if an Orc value conversion applies */
  def isOrcJavaNumConvertable(fromType: Class[_], toType: Class[_]): Boolean = {
    toType match {
      case `byteRefClass` => isOrcIntegralClass(fromType)
      case `shortRefClass` => isOrcIntegralClass(fromType)
      case `intRefClass` => isOrcIntegralClass(fromType)
      case `longRefClass` => isOrcIntegralClass(fromType)
      case `floatRefClass` => isOrcIntegralClass(fromType) || isOrcFloatingPointClass(fromType)
      case `doubleRefClass` => isOrcIntegralClass(fromType) || isOrcFloatingPointClass(fromType)
      case java.lang.Byte.TYPE => isOrcIntegralClass(fromType)
      case java.lang.Short.TYPE => isOrcIntegralClass(fromType)
      case java.lang.Integer.TYPE => isOrcIntegralClass(fromType)
      case java.lang.Long.TYPE => isOrcIntegralClass(fromType)
      case java.lang.Float.TYPE => isOrcIntegralClass(fromType) || isOrcFloatingPointClass(fromType)
      case java.lang.Double.TYPE => isOrcIntegralClass(fromType) || isOrcFloatingPointClass(fromType)
      case _ => false
    }
  }

  /** Most specific method per JLS §15.12.2.5 */
  private def mostSpecificMethod[M <: { def getDeclaringClass(): java.lang.Class[_]; def getParameterTypes(): Array[java.lang.Class[_]]; def getModifiers(): Int }](methods: Traversable[M]): M = {
    //FIXME: Verify that this is correct wrt var arg calls. It probably is because array subtyping is allowed.
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
      throw new java.lang.NoSuchMethodException()
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
