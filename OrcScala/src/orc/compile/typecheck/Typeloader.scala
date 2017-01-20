//
// Typeloader.scala -- Scala object Typeloader
// Project OrcScala
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml.
//
package orc.compile.typecheck

import orc.ast.oil.{ named => syntactic }
import orc.types._
import orc.values.sites.SiteClassLoading
import orc.compile.typecheck.Typechecker._
import orc.types.Variance._
import orc.error.compiletime.typing._
import orc.error.compiletime.UnboundTypeVariableException
import orc.util.OptionMapExtension._
import java.lang.{ reflect => jvm }
import orc.util.TypeListEnrichment._

import scala.collection.immutable.HashMap

import orc.lib.state.types.ArrayType
import orc.lib.state.types.RefType

/** Lift syntactic types to semantic types, or reify
  * semantic types to syntactic types.
  *
  * @author dkitchin
  */
object Typeloader extends SiteClassLoading {
  import Typechecker.Context

  /** Lift a syntactic type to a first-order semantic type.
    */
  def lift(t: syntactic.Type)(implicit ctx: Context): Type = {
    import ctx._

    t match {
      case u: syntactic.BoundTypevar => {
        (typeContext get u) match {
          case Some(t) => t
          case None => {
            (typeOperatorContext get u) match {
              case Some(t) => throw new FirstOrderTypeExpectedException(t.toString)
              case None => throw new UnboundTypeException(u.toString) // should never occur
            }
          }
        }
      }
      case syntactic.Top() => Top
      case syntactic.Bot() => Bot
      case syntactic.TupleType(elements) => TupleType(elements map lift)
      case syntactic.RecordType(entries) => RecordType(entries mapValues lift)
      case syntactic.AssertedType(assertedType) => lift(assertedType)
      case syntactic.FunctionType(typeFormals, argTypes, returnType) => {
        val newTypeFormals = typeFormals map { u => new TypeVariable(u) }
        val typeBindings = typeFormals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newArgTypes = argTypes map { lift(_)(ctx.copy(typeContext=newTypeContext)) }
        val newReturnType = lift(returnType)(ctx.copy(typeContext=newTypeContext))
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
      case syntactic.TypeApplication(t, typeactuals) => {
        liftToOperator(t).operate(typeactuals map lift)
      }
      case syntactic.ImportedType(classname) => loadType(classname)
      case syntactic.ClassType(classname) => {
        val cl = loadClass(classname)
        liftJavaType(cl)
      }
      case syntactic.VariantType(self, Nil, variants) => {
        val dt = new MonomorphicDatatype()
        val newTypeContext = typeContext + { (self, dt) }
        val constructorTypes =
          for ((name, variantArgs) <- variants) yield {
            val argTypes = variantArgs map { lift(_)(ctx.copy(typeContext=newTypeContext)) }
            new RecordType(
              "apply" -> SimpleFunctionType(argTypes, dt),
              "unapply" -> SimpleFunctionType(List(dt), argTypes.condense))
          }
        dt.optionalDatatypeName = self.optionalVariableName
        dt.constructorTypes = Some(constructorTypes)
        dt
      }
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case t => throw new FirstOrderTypeExpectedException(t.toString)
    }
  }

  /** Lift a syntactic type to a second-order semantic type.
    */
  def liftToOperator(t: syntactic.Type)(implicit ctx: Context): TypeOperator = {
    import ctx._

    t match {
      case u: syntactic.BoundTypevar => {
        (typeOperatorContext get u) match {
          case Some(op) => op
          case None => {
            (typeContext get u) match {
              case Some(tt) => throw new SecondOrderTypeExpectedException(tt.toString)
              case None => throw new UnboundTypeException(u.toString) // should never occur
            }
          }
        }
      }
      case syntactic.VariantType(self, typeformals, variants) if (!typeformals.isEmpty) => {

        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings

        /*
         * Given a datatype with a proposed set of variances,
         * lift the variants to generate the constructor types.
         */
        def usingDatatype(dt: PolymorphicDatatype): List[List[Type]] = {
          val newTypeOperatorContext = typeOperatorContext + { (self, dt) }
          val variantTypes =
            for ((name, variantArgs) <- variants) yield {
              val constructorArgTypes = variantArgs map { lift(_)(ctx.copy(typeContext=newTypeContext, typeOperatorContext=newTypeOperatorContext)) }
              constructorArgTypes
            }
          variantTypes.toList
        }

        /* Find the variance of X in this datatype by a search.
         *
         * We assume that variances are linearly independent, i.e.
         * that the variances of formals besides X cannot affect
         * the variance of X.
         */
        def findVariance(X: TypeVariable): Variance = {
          for (V <- List(Constant, Covariant, Contravariant)) {
            val currentGuess = newTypeFormals map { Y => if (Y eq X) V else Invariant }
            val dt = new PolymorphicDatatype(currentGuess)
            val variantTypes = usingDatatype(dt)
            val argVariances = variantTypes map { _ map { _ varianceOf X } }
            val combinedVariances = (argVariances map { _.combined }).combined
            if (combinedVariances eq V) {
              return V
            }
            /* otherwise, try the next possibility */
          }
          /* If no other variance is consistent, return Invariant
           * by default, since it will always be consistent.
           */
          return Invariant
        }
        val stableVariances = newTypeFormals map findVariance

        val dt = new PolymorphicDatatype(stableVariances)
        val variantTypes = usingDatatype(dt)
        val constructorTypes =
          variantTypes map
            { argTypes =>
              val applyType = {
                val funTypeFormals = newTypeFormals map { x => new TypeVariable(x) }
                val funArgTypes = argTypes map { _ subst (funTypeFormals, newTypeFormals) }
                val funReturnType = TypeInstance(dt, funTypeFormals)
                FunctionType(funTypeFormals, funArgTypes, funReturnType)
              }
              val unapplyType = new UnaryCallableType() with StrictCallableType {
                def call(t: Type): Type = {
                  t match {
                    case TypeInstance(`dt`, typeActuals) => {
                      val actualArgTypes = argTypes map { _ subst (typeActuals, newTypeFormals) }
                      actualArgTypes.condense
                    }
                    case u => throw new ArgumentTypecheckingException(0, TypeInstance(dt, newTypeFormals), u)
                  }
                }
              }
              new RecordType(
                "apply" -> applyType,
                "unapply" -> unapplyType)
            }

        dt.constructorTypes = Some(constructorTypes)
        dt.optionalDatatypeName = self.optionalVariableName
        dt
      }
      case syntactic.TypeAbstraction(typeformals, t) => {
        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newT = lift(t)(ctx.copy(typeContext=newTypeContext))
        new TypeOperator { def operate(ts: List[Type]) = newT.subst(ts, newTypeFormals) }
      }
      case syntactic.ImportedType(classname) => {
        loadTypeOperator(classname)
      }
      case syntactic.ClassType(classname) => {
        val cl = loadClass(classname)
        liftJavaTypeOperator(cl)
      }
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case tt => {
        throw new SecondOrderTypeExpectedException(tt.toString)
      }
    }
  }

  /** Lift a syntactic type to either a first or second order type;
    * the kind is not known in advance.
    */
  def liftEither(t: syntactic.Type)(implicit ctx: Context): Either[Type, TypeOperator] = {
    import ctx._

    try {
      Left(lift(t))
    } catch {
      case _: FirstOrderTypeExpectedException | _: TypeResolutionException => {
        Right(liftToOperator(t))
      }
    }
  }

  /** Lift a Java object type (represented by a Java class with no type parameters)
    * to an Orc type.
    */
  def liftJavaType(jt: jvm.Type, jctx: Map[jvm.TypeVariable[_], Type] = Nil.toMap): Type = {
    jt match {
      // C
      case cl: Class[_] => {
        // Make sure the parameters have been set in the context;
        // otherwise we are probably trying to lift a second-order type
        // by mistake.
        for (x <- cl.getTypeParameters()) {
          if (!(jctx contains x)) {
            throw new FirstOrderTypeExpectedException(Option(cl.getClass.getCanonicalName).getOrElse(cl.getClass.getName))
          }
        }
        // Check if this is actually a primitive array class
        if (cl.isArray()) {
          val T = liftJavaType(cl.getComponentType(), jctx)
          ArrayType(T)
        } else if (java.lang.Void.TYPE isAssignableFrom cl) {
          SignalType
        } else if (java.lang.Boolean.TYPE isAssignableFrom cl) {
          BooleanType
        } else if (java.lang.Character.TYPE isAssignableFrom cl) {
          JavaObjectType(classOf[java.lang.Character])
        } // The Orc type system does not track distinctions between Java primitive types.
        else if ((java.lang.Byte.TYPE isAssignableFrom cl)
          || (java.lang.Short.TYPE isAssignableFrom cl)
          || (java.lang.Integer.TYPE isAssignableFrom cl)
          || (java.lang.Long.TYPE isAssignableFrom cl)) {
          IntegerType
        } else if ((java.lang.Float.TYPE isAssignableFrom cl)
          || (java.lang.Double.TYPE isAssignableFrom cl)) {
          NumberType
        } else {
          JavaObjectType(cl, jctx)
        }
      }
      // X
      case x: jvm.TypeVariable[_] => {
        x.getBounds().toList match {
          case List(b: Class[_]) if b.isAssignableFrom(classOf[Object]) => {
            jctx(x)
          }
          case _ => {
            throw new NoJavaTypeBoundsException()
          }
        }
      }
      // ?
      case w: jvm.WildcardType => {
        w.getUpperBounds().toList match {
          case List(upperBound: Class[_]) if upperBound.isAssignableFrom(classOf[Object]) => {
            Top
          }
          case _ => {
            throw new NoJavaTypeBoundsException()
          }
        }
      }
      // C<D>
      case pt: jvm.ParameterizedType => {
        val typeActuals = pt.getActualTypeArguments.toList map { liftJavaType(_, jctx) }
        val typeOp = liftJavaTypeOperator(pt.getRawType())
        typeOp.operate(typeActuals)
      }
      // C[]
      case gat: jvm.GenericArrayType => {
        val T = liftJavaType(gat.getGenericComponentType(), jctx)
        ArrayType(T)
      }
    }
  }

  def liftJavaClassType(cl: Class[_]) = JavaClassType(cl)

  /** Lift a Java generic type (represented by a Java class with type parameters)
    * to an Orc type operator.
    */
  def liftJavaTypeOperator(jt: jvm.Type): TypeOperator = {

    jt match {
      case cl: Class[_] => JavaTypeConstructor(cl)
      // At present, due to the type kinding assumptions of the Orc typechecker,
      // (namely, that type arguments are always first order), we can only allow
      // raw generic classes as type operators.
      case _ => throw new SecondOrderTypeExpectedException(jt.toString())
    }

  }

  def liftJavaField(jf: jvm.Field, jctx: Map[jvm.TypeVariable[_], Type]): Type = {
    val javaT = jf.getGenericType()
    val T = liftJavaType(javaT, jctx)
    RefType(T)
  }

  def liftJavaMethod(jm: jvm.Method, jctx: Map[jvm.TypeVariable[_], Type]): CallableType = {
    val javaTypeFormals = jm.getTypeParameters().toList
    val javaArgTypes = jm.getGenericParameterTypes().toList
    val javaReturnType = jm.getGenericReturnType()

    val newTypeFormals = javaTypeFormals map { jx => new TypeVariable(Some(jx.getName())) }
    val newJavaContext = jctx ++ (javaTypeFormals zip newTypeFormals)
    val newArgTypes = javaArgTypes map { liftJavaType(_, newJavaContext) }
    val newReturnType = liftJavaType(javaReturnType, newJavaContext)

    FunctionType(newTypeFormals, newArgTypes, newReturnType)
  }

  /** This function converts a semantic type back to a syntactic type.
    *
    * The result is optional, since some semantic types will have
    * no syntactic representation.
    *
    * This conversion performs reverse lookups on typing contexts
    * to find the syntactic type variable which originally bound
    * the semantic type. If the result is not unique, reification fails.
    *
    * @author dkitchin
    */
  def reify(that: Type)(implicit ctx: Context): Option[syntactic.Type] = {
    import ctx._

    that match {
      case Top => Some(syntactic.Top())
      case Bot => Some(syntactic.Bot())
      case IntegerConstantType(_) => reify(IntegerType)
      case TupleType(elements) => {
        val newElements = elements optionMap reify
        newElements match {
          case Some(syntacticElements) => {
            Some(syntactic.TupleType(syntacticElements))
          }
          case _ => None
        }
      }
      case RecordType(entries) => {
        var newEntries: Option[Map[String, syntactic.Type]] = Some(HashMap.empty)
        for ((k, t) <- entries) {
          if (!newEntries.isEmpty) {
            reify(t) match {
              case Some(u) => newEntries = newEntries map { _ + ((k, u)) }
              case None => newEntries = None
            }
          }
        }
        newEntries map { syntactic.RecordType(_) }
      }
      case FunctionType(typeFormals, argTypes, returnType) => {
        val syntacticTypeFormals = typeFormals map { u => new syntactic.BoundTypevar(u.optionalVariableName) }
        val typeBindings = syntacticTypeFormals zip typeFormals
        val newTypeContext = typeContext ++ typeBindings // exploit reverse lookup to restore syntactic parameters
        val newArgTypes = argTypes optionMap { reify(_)(ctx.copy(typeContext=newTypeContext)) }
        val newReturnType = reify(returnType)(ctx.copy(typeContext=newTypeContext))
        (newArgTypes, newReturnType) match {
          case (Some(syntacticArgTypes), Some(syntacticReturnType)) => {
            Some(syntactic.FunctionType(syntacticTypeFormals, syntacticArgTypes, syntacticReturnType))
          }
          case _ => None
        }
      }
      case TypeInstance(tycon, args) => {
        val newTycon = reverseLookup(typeOperatorContext, tycon)
        val newArgs = args optionMap reify
        (newTycon, newArgs) match {
          case (Some(syntacticTycon), Some(syntacticArgs)) => {
            Some(syntactic.TypeApplication(syntacticTycon, syntacticArgs))
          }
          case _ => None
        }
      }
      case t => {
        reverseLookup(typeContext, t)
      }
    }
  }

  def reverseLookup[X, Y](m: Map[X, Y], target: Y): Option[X] = {
    val candidates = m filter { case (x, y) => y equals target }
    candidates.keys.toList match {
      case Nil => None
      case List(x) => Some(x)
      case alts => None // TODO Issue warning: multiple candidates
    }
  }

  /** Given the name of a subtype of Type, instantiate that class
    * as a type.
    */
  def loadType(name: String): Type = {
    val loadedClass = loadClass(name)
    if (classOf[Type] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[Type]].newInstance()
      } catch {
        case e: Throwable => throw new TypeResolutionException(loadedClass.getName(), e)
      }
    } else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name + "$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[Type]
      } catch {
        case _: Throwable => {} //Ignore -- It's not a Scala object, then.
      }
      throw new TypeResolutionException(loadedClass.getName, new ClassCastException(loadedClass.getName + " cannot be cast to " + classOf[Type].getName))
    }
  }

  /** Given the name of a subtype of TypeOperator, instantiate that class
    * as a type operator.
    */
  def loadTypeOperator(name: String): TypeOperator = {
    val loadedClass = loadClass(name)
    if (classOf[TypeOperator] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[TypeOperator]].newInstance()
      } catch {
        case e: Throwable => throw new TypeOperatorResolutionException(loadedClass.getName(), e)
      }
    } else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name + "$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[TypeOperator]
      } catch {
        case _: Throwable => {} //Ignore -- It's not a Scala object, then.
      }
      throw new TypeOperatorResolutionException(loadedClass.getName, new ClassCastException(loadedClass.getName + " cannot be cast to " + classOf[TypeOperator].getName))
    }
  }

}
