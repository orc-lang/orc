//
// TypeChecker.scala -- Scala object TypeChecker
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.typecheck

import scala.language.reflectiveCalls
import orc.ast.oil.{ named => syntactic }
import orc.ast.oil.named.{ Expression, Stop, Hole, Call, ||, ow, <, <|, >, Limit, VtimeZone, DeclareDefs, HasType, DeclareType, Constant, UnboundVar, Def, FoldedCall, FoldedLambda }
import orc.types._
import orc.error.compiletime.typing._
import orc.error.compiletime.{ UnboundVariableException, UnboundTypeVariableException, CompilationException, ContinuableSeverity }
import orc.error.OrcExceptionExtension._
import orc.util.OptionMapExtension._
import orc.values.{ Signal, Field }
import scala.math.BigInt
import scala.math.BigDecimal
import orc.values.sites.TypedSite
import orc.compile.typecheck.ConstraintSet._
import orc.compile.typecheck.Typeloader._

/** Typechecker for Orc expressions.
  *
  * This typechecker implements a variation of the local type inference algorithm
  * described by Pierce and Turner in "Local Type Inference" (POPL '98). It extends
  * the algorithm with second-order types (thus the metatheory is based on F2sub),
  * but it does not yet implement bounded polymorphism.
  *
  * Calls to the typechecker will return a new version of the given expression.
  * At present this transformation capability is used only to add inferred types
  * and type arguments to the AST. However, it could be used in the future to
  * perform type-directed transformations of the AST.
  *
  * It is suggested that the AST undergo def fractioning before typechecking; otherwise
  * the detection of recursive functions will be oversensitive, and the typechecker
  * may fail to infer return types for some non-recursive functions.
  *
  * @author dkitchin
  */
object Typechecker {

  type Context = Map[syntactic.BoundVar, Type]
  type TypeContext = Map[syntactic.BoundTypevar, Type]
  type TypeOperatorContext = Map[syntactic.BoundTypevar, TypeOperator]

}

class Typechecker(val reportProblem: CompilationException with ContinuableSeverity => Unit) {

  import orc.compile.typecheck.Typechecker._

  def typecheck(expr: Expression): (Expression, Type) = {
    typeSynthExpr(expr)(Map.empty, Map.empty, Map.empty)
  }

  def typeSynthExpr(expr: Expression)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Expression, Type) = {
    try {
      val (newExpr, exprType) =
        expr match {
          case Stop() => (expr, Bot)
          case Hole(_, _) => (expr, Bot)
          case Constant(value) => (expr, typeValue(value))
          case x: syntactic.BoundVar => (x, context(x))
          case UnboundVar(name) => throw new UnboundVariableException(name)
          case FoldedCall(target, args, typeArgs) => {
            typeFoldedCall(target, args, typeArgs, None, expr)
          }
          case left || right => {
            val (newLeft, typeLeft) = typeSynthExpr(left)
            val (newRight, typeRight) = typeSynthExpr(right)
            (newLeft || newRight, typeLeft join typeRight)
          }
          case left ow right => {
            val (newLeft, typeLeft) = typeSynthExpr(left)
            val (newRight, typeRight) = typeSynthExpr(right)
            (newLeft ow newRight, typeLeft join typeRight)
          }
          case left > x > right => {
            val (newLeft, typeLeft) = typeSynthExpr(left)
            val (newRight, typeRight) = typeSynthExpr(right)(context + ((x, typeLeft)), typeContext, typeOperatorContext)
            (newLeft > x > newRight, typeRight)
          }
          case left < x <| right => {
            val (newRight, typeRight) = typeSynthExpr(right)
            val (newLeft, typeLeft) = typeSynthExpr(left)(context + ((x, typeRight)), typeContext, typeOperatorContext)
            (newLeft < x <| newRight, typeLeft)
          }
          case Limit(body) => {
            val (newBody, typeBody) = typeSynthExpr(body)
            (Limit(newBody), typeBody)
          }
          case DeclareDefs(defs, body) => {
            val (newDefs, defBindings) = typeDefs(defs)
            val (newBody, typeBody) = typeSynthExpr(body)(context ++ defBindings, typeContext, typeOperatorContext)
            (DeclareDefs(newDefs, newBody), typeBody)
          }
          case VtimeZone(order, body) => {
            val (newBody, typeBody) = typeSynthExpr(body)
            (VtimeZone(order, newBody), typeBody)
          }
          case HasType(body, syntactic.AssertedType(t)) => {
            (expr, lift(t))
          }
          case HasType(body, t) => {
            val expectedType = lift(t)
            val newBody = typeCheckExpr(body, expectedType)
            (HasType(newBody, t), expectedType)
          }
          case DeclareType(u, t, body) => {
            val declaredType = liftEither(t)
            val (newTypeContext, newTypeOperatorContext) =
              declaredType match {
                case Left(t) => (typeContext + ((u, t)), typeOperatorContext)
                case Right(op) => (typeContext, typeOperatorContext + ((u, op)))
              }
            val (newBody, typeBody) = typeSynthExpr(body)(context, newTypeContext, newTypeOperatorContext)
            (DeclareType(u, t, newBody), typeBody)
          }
        }
      (expr ->> newExpr, exprType)
    } catch {
      case e: TypeException => {
        throw (e.setPosition(expr.pos))
      }
    }
  }

  def typeCheckExpr(expr: Expression, T: Type)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Expression = {
    try {
      expr -> {
        /* FoldedCall must be checked before prune, since it
         * may contain some number of enclosing prunings.
         */
        case FoldedCall(target, args, typeArgs) => {
          val (e, _) = typeFoldedCall(target, args, typeArgs, Some(T), expr)
          e
        }
        case left || right => {
          val newLeft = typeCheckExpr(left, T)
          val newRight = typeCheckExpr(right, T)
          newLeft || newRight
        }
        case left ow right => {
          val newLeft = typeCheckExpr(left, T)
          val newRight = typeCheckExpr(right, T)
          newLeft ow newRight
        }
        case left > x > right => {
          val (newLeft, typeLeft) = typeSynthExpr(left)
          val newRight = typeCheckExpr(right, T)(context + ((x, typeLeft)), typeContext, typeOperatorContext)
          newLeft > x > newRight
        }
        case left < x <| right => {
          val (newRight, typeRight) = typeSynthExpr(right)
          val newLeft = typeCheckExpr(left, T)(context + ((x, typeRight)), typeContext, typeOperatorContext)
          newLeft < x <| newRight
        }
        case FoldedLambda(formals, body, typeFormals, None, None) => {
          T match {
            case FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType) => {
              val argBindings = formals zip liftedArgTypes
              val typeBindings = typeFormals zip liftedTypeFormals
              val newBody = typeCheckExpr(body, liftedReturnType)(context ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)
              val argTypes = liftedArgTypes optionMap { reify(_)(typeContext ++ typeBindings, typeOperatorContext) }
              val returnType = reify(liftedReturnType)
              FoldedLambda(formals, newBody, typeFormals, argTypes, returnType)
            }
            case _ => throw new FunctionTypeExpectedException(T)
          }
        }
        case DeclareDefs(defs, body) => {
          val (newDefs, defBindings) = typeDefs(defs)
          val newBody = typeCheckExpr(body, T)(context ++ defBindings, typeContext, typeOperatorContext)
          DeclareDefs(newDefs, newBody)
        }
        case DeclareType(u, t, body) => {
          val declaredType = liftEither(t)
          val (newTypeContext, newTypeOperatorContext) =
            declaredType match {
              case Left(t) => (typeContext + ((u, t)), typeOperatorContext)
              case Right(op) => (typeContext, typeOperatorContext + ((u, op)))
            }
          val newBody = typeCheckExpr(body, T)(context, typeContext + ((u, lift(t))), typeOperatorContext)
          DeclareType(u, t, newBody)
        }
        case _ => {
          val (newExpr, exprType) = typeSynthExpr(expr)
          exprType assertSubtype T
          newExpr
        }
      }
    } catch {
      case e: TypeException => {
        throw (e.setPosition(expr.pos))
      }
    }
  }

  def typeDefs(defgroup: List[Def])(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (List[Def], List[(syntactic.BoundVar, Type)]) = {
    val defs =
      defgroup map {
        // If argument types are missing, and there are no formals, infer an empty arg type list
        case d @ Def(_, Nil, _, _, None, _) => d.copy(argtypes = Some(Nil))
        case d => d
      }
    defs match {
      // The return type is absent and we may be able to infer it.
      case List(d @ Def(name, formals, body, typeFormals, Some(argTypes), None)) => {
        if (d.body.freevars contains d.name) {
          // We do not infer return types for recursive functions.
          val e = new UnspecifiedReturnTypeException()
          e.setPosition(d.pos)
          throw e
        } else {
          val liftedTypeFormals = typeFormals map { u => new TypeVariable(u) }
          val typeBindings = typeFormals zip liftedTypeFormals

          val liftedArgTypes = argTypes map { lift(_)(typeContext ++ typeBindings, typeOperatorContext) }

          val argBindings = formals zip liftedArgTypes

          // Note that the function itself is not bound in the context, since we know it is not recursive.
          val (newBody, liftedReturnType) = typeSynthExpr(body)(context ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)

          val newDef = d.copy(body = newBody, returntype = reify(liftedReturnType)(typeContext ++ typeBindings, typeOperatorContext))
          val liftedDefType = FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType)

          (List(newDef), List((name, liftedDefType)))
        }
      }
      case ds => {
        val defBindings: List[(syntactic.BoundVar, Type)] =
          for (d <- ds) yield {
            d match {
              case Def(name, _, _, typeFormals, Some(argTypes), Some(returnType)) => {
                val syntacticType = syntactic.FunctionType(typeFormals, argTypes, returnType)
                (name, lift(syntacticType))
              }
              case Def(_, _, _, _, None, _) => {
                val e = new UnspecifiedArgTypesException()
                e.setPosition(d.pos)
                throw e
              }
              case Def(_, _, _, _, _, None) => {
                val e = new UnspecifiedReturnTypeException()
                e.setPosition(d.pos)
                throw e
              }
            }
          }
        val defTypeMap = defBindings.toMap
        val newDefs = {
          for (d <- ds) yield {
            val FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType) = defTypeMap(d.name)

            val argBindings = d.formals zip liftedArgTypes
            val typeBindings = d.typeformals zip liftedTypeFormals

            val newBody = typeCheckExpr(d.body, liftedReturnType)(context ++ defBindings ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)

            d.copy(body = newBody)
          }
        }
        (newDefs, defBindings)
      }

    }
  }

  def typeFoldedCall(target: Expression, args: List[Expression], syntacticTypeArgs: Option[List[syntactic.Type]], checkReturnType: Option[Type], callPoint: Expression)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Expression, Type) = {
    val (newTarget, targetType) = typeSynthExpr(target)
    val (newArgs, argTypes) = {
      targetType match {
        /* Note that this argument type annotation inference is very limited;
         * the typechecker only switches to checking mode if the function has
         * no type parameters. Thus, type argument inference will fail on many
         * common cases, such as list mapping, where one might hope that type
         * argument annotations would not be required.
         *
         * Mea culpa; I could not find a way to make this work for polymorphic
         * calls and still be sure that it was correct.
         *
         * - dkitchin
         */
        case FunctionType(Nil, funArgTypes, _) => {
          def check(pair: (Expression, Type)): (Expression, Type) = {
            val (arg, t) = pair
            val newArg = typeCheckExpr(arg, t)
            (newArg, t)
          }
          if (funArgTypes.size != args.size) {
            throw new ArgumentArityException(funArgTypes.size, args.size)
          }
          ((args zip funArgTypes) map check).unzip
        }
        case _ => (args map typeSynthExpr).unzip
      }
    }
    val (newTypeArgs, returnType) = typeCall(syntacticTypeArgs, targetType, argTypes, checkReturnType, callPoint)
    (FoldedCall(newTarget, newArgs, newTypeArgs), returnType)
  }

  def typeCall(syntacticTypeArgs: Option[List[syntactic.Type]], targetType: Type, argTypes: List[Type], checkReturnType: Option[Type], callPoint: Expression)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Option[List[syntactic.Type]], Type) = {

    // Special call cases
    targetType match {
      case Bot => {
        (syntacticTypeArgs, Bot)
      }
      case _: StrictType if argTypes contains Bot => {
        (syntacticTypeArgs, Bot)
      }
      case RecordType(entries) => {
        argTypes match {
          /* If this is a field access, check the call as normal. */
          case List(_: FieldType) => {
            typeCoreCall(syntacticTypeArgs, targetType, argTypes, checkReturnType, callPoint)
          }
          /* If this is not a field access, try to use an 'apply' member. */
          case _ => {
            if (entries contains "apply") {
              typeCall(syntacticTypeArgs, entries("apply"), argTypes, checkReturnType, callPoint)
            } else {
              typeCoreCall(syntacticTypeArgs, targetType, argTypes, checkReturnType, callPoint)
            }
          }
        }
      }
      case OverloadedType(alternatives) => {
        var failure = new OverloadedTypeException()
        def tryAlternatives(alts: List[Type]): (Option[List[syntactic.Type]], Type) =
          alts match {
            case t :: rest => {
              try {
                typeCall(syntacticTypeArgs, t, argTypes, checkReturnType, callPoint)
              } catch {
                case te: TypeException => {
                  failure = failure.addAlternative(t, te)
                  tryAlternatives(rest)
                }
              }
            }
            case Nil => {
              throw failure
            }
          }
        tryAlternatives(alternatives)
      }
      case `IntegerType` | IntegerConstantType(_) => {
        typeCall(syntacticTypeArgs, JavaObjectType(classOf[java.math.BigInteger]), argTypes, checkReturnType, callPoint)
      }
      case `NumberType` => {
        typeCall(syntacticTypeArgs, JavaObjectType(classOf[java.math.BigDecimal]), argTypes, checkReturnType, callPoint)
      }
      case _ => {
        typeCoreCall(syntacticTypeArgs, targetType, argTypes, checkReturnType, callPoint)
      }
    }

  }

  def typeCoreCall(syntacticTypeArgs: Option[List[syntactic.Type]], targetType: Type, argTypes: List[Type], checkReturnType: Option[Type], callPoint: Expression)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Option[List[syntactic.Type]], Type) = {

    val (finalSyntacticTypeArgs, finalReturnType) =
      syntacticTypeArgs match {
        case Some(args) => {
          val typeArgs = args map lift
          val returnType =
            targetType match {
              case FunctionType(funTypeFormals, funArgTypes, funReturnType) => {
                if (funTypeFormals.size != typeArgs.size) {
                  throw new TypeArgumentArityException(funTypeFormals.size, typeArgs.size)
                }
                if (funArgTypes.size != argTypes.size) {
                  throw new ArgumentArityException(funArgTypes.size, argTypes.size)
                }
                for ((s, u) <- argTypes zip funArgTypes) {
                  val t = u.subst(typeArgs, funTypeFormals)
                  s assertSubtype t
                }
                funReturnType subst (typeArgs, funTypeFormals)
              }
              case ct: CallableType => {
                ct.call(typeArgs, argTypes)
              }
              case _ => throw new UncallableTypeException(targetType)
            }
          (syntacticTypeArgs, returnType)
        }
        case None => {
          targetType match {
            case FunctionType(Nil, funArgTypes, funReturnType) => {
              if (funArgTypes.size != argTypes.size) {
                throw new ArgumentArityException(funArgTypes.size, argTypes.size)
              }
              for ((s, t) <- argTypes zip funArgTypes) {
                s assertSubtype t
              }
              (Some(Nil), funReturnType)
            }
            case FunctionType(funTypeFormals, funArgTypes, funReturnType) => {
              if (funArgTypes.size != argTypes.size) {
                throw new ArgumentArityException(funArgTypes.size, argTypes.size)
              }
              val X = funTypeFormals.toSet
              val baseConstraints = new ConstraintSet(X)
              val argConstraints =
                for ((s, t) <- argTypes zip funArgTypes) yield {
                  typeConstraints({ _ => false }, s, t)(X)
                }
              val sigma: Map[TypeVariable, Type] =
                checkReturnType match {
                  case Some(r) => {
                    val returnConstraints = typeConstraints({ _ => false }, funReturnType, r)(X)
                    val allConstraints = meetAll(argConstraints) meet returnConstraints meet baseConstraints
                    allConstraints.anySubstitution
                  }
                  case None => {
                    /* Emit a warning if an invariant type constructor cannot find a minimal type, and must make a guess. */
                    def warnNoMinimal(guess: Type) {
                      reportProblem((new NoMinimalTypeWarning(guess)) at callPoint)
                    }

                    val allConstraints = meetAll(argConstraints) meet baseConstraints
                    allConstraints.minimalSubstitution(funReturnType, warnNoMinimal)
                  }
                }

              val newReturnType = funReturnType subst sigma
              val newTypeArgs = funTypeFormals map { sigma(_) }

              val newSyntacticTypeArgs = newTypeArgs optionMap reify
              (newSyntacticTypeArgs, newReturnType)
            }
            case ct: CallableType => {
              val returnType = ct.call(Nil, argTypes)
              (Some(Nil), returnType)
            }
            case _ => throw new UncallableTypeException(targetType)
          }
        }
      }

    checkReturnType match {
      case Some(t) => finalReturnType assertSubtype t
      case None => {}
    }

    (finalSyntacticTypeArgs, finalReturnType)

  }

  def typeValue(value: AnyRef): Type = {

    if (value eq null) {
      NullType
    } else {
      value match {
        case Signal => SignalType
        case _: java.lang.Boolean => BooleanType
        case i: BigInt => IntegerConstantType(i)
        case _: BigDecimal => NumberType
        case _: String => StringType
        case Field(f) => FieldType(f)
        case s: TypedSite => s.orcType
        case v => liftJavaType(v.getClass())
      }
    }
  }

  /** Find constraints on the variables xs such that S <: T
    *
    * We assume that the variables xs appear in at most one of S or T
    */
  def typeConstraints(V: TypeVariable => Boolean, below: Type, above: Type)(implicit xs: Set[TypeVariable]): ConstraintSet = {
    (below, above) match {
      case (_, Top) => NoConstraints
      case (Bot, _) => NoConstraints
      case (x: TypeVariable, y: TypeVariable) if (x eq y) => {
        NoConstraints
      }
      case (y: TypeVariable, s) if (xs contains y) => {
        val t = s demote V
        new ConstraintSet(Bot, y, t)
      }
      case (s, y: TypeVariable) if (xs contains y) => {
        val t = s promote V
        new ConstraintSet(t, y, Top)
      }
      case (f: FunctionType, g: FunctionType) if (f sameShape g) => {
        val (FunctionType(typeFormals, lowerArgTypes, lowerReturnType),
          FunctionType(_, upperArgTypes, upperReturnType)) = f shareFormals g
        def Vscope(x: TypeVariable) = {
          (!(typeFormals contains x)) && (V(x))
        }
        val C =
          for ((s, t) <- upperArgTypes zip lowerArgTypes) yield {
            typeConstraints(Vscope, s, t)
          }
        val D = {
          typeConstraints(Vscope, lowerReturnType, upperReturnType)
        }
        meetAll(C) meet D
      }
      case (TupleType(elementsBelow), TupleType(elementsAbove)) if (elementsBelow.size == elementsAbove.size) => {
        val C =
          for ((s, t) <- elementsBelow zip elementsAbove) yield {
            typeConstraints(V, s, t)
          }
        meetAll(C)
      }
      case (RecordType(entriesBelow), RecordType(entriesAbove)) if ((entriesAbove.keySet) subsetOf (entriesBelow.keySet)) => {
        val C =
          for (l <- entriesAbove.keys) yield {
            typeConstraints(V, entriesBelow(l), entriesAbove(l))
          }
        meetAll(C)
      }
      case (TypeInstance(tycon, argsBelow), TypeInstance(tyconPrime, argsAbove)) if (tycon eq tyconPrime) => {
        val C =
          for ((v, (s, t)) <- tycon.variances zip (argsBelow zip argsAbove)) yield {
            v match {
              case orc.types.Constant => NoConstraints
              case Covariant => typeConstraints(V, s, t)
              case Contravariant => typeConstraints(V, t, s)
              /* Note:
               *
               * This generation of constraints for invariant type constructor positions
               * relies on the assumption that S <: T and T <: S together imply S = T
               *
               * In the presence of bounded polymorphism, a more creative solution is
               * needed, probably involving an addition = form (similar to the <: form)
               * of constraint generation.
               */
              case Invariant => (typeConstraints(V, s, t)) meet (typeConstraints(V, t, s))
            }
          }
        meetAll(C)
      }
      case (s, t) => {
        s assertSubtype t
        NoConstraints
      }

    }
  }

}
