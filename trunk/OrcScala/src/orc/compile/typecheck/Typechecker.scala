//
// TypeChecker.scala -- Scala object TypeChecker
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.typecheck

import orc.ast.oil.{named => syntactic}
import orc.ast.oil.named.{Expression, Stop, Hole, Call, ||, ow, <, >, DeclareDefs, HasType, DeclareType, Constant, UnboundVar, Def, FoldedCall, FoldedLambda}
import orc.types._
import orc.error.compiletime.typing._
import orc.util.OptionMapExtension._
import orc.values.Signal
import orc.values.sites.TypedSite
import orc.compile.typecheck.ConstraintSet._

/**
 * 
 * Typechecker for Orc expressions.
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
  
  def apply(expr: Expression): (Expression, Type) = typeSynth(expr)(Map.empty, Map.empty, Map.empty)
  
  def typeSynth(expr: Expression)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Expression, Type) = {
    try {
      val (newExpr, exprType) = 
        expr match {
          case Stop() => (expr, Bot)
          case Hole(_,_) => (expr, Bot)
          case Constant(value) => (expr, typeValue(value))
          case x: syntactic.BoundVar => (x, context(x))
          case UnboundVar(_) => (expr, Bot)  // TODO: Emit a warning: unbound variable
          case FoldedCall(target, args, typeArgs) => {
            val (newTarget, typeTarget) = typeSynth(target)
            val (newArgs, argTypes) = (args map typeSynth).unzip
            val (newTypeArgs, returnType) = typeCall(typeArgs, typeTarget, argTypes, None)
            (FoldedCall(newTarget, newArgs, newTypeArgs), returnType)
          }
          case left || right  => {
            val (newLeft, typeLeft) = typeSynth(left)
            val (newRight, typeRight) = typeSynth(right)
            (newLeft || newRight, typeLeft join typeRight)
          }
          case left ow right => {
            val (newLeft, typeLeft) = typeSynth(left)
            val (newRight, typeRight) = typeSynth(right)
            (newLeft || newRight, typeLeft join typeRight)
          }
          case left > x > right => {
            val (newLeft, typeLeft) = typeSynth(left)
            val (newRight, typeRight) = typeSynth(right)(context + ((x, typeLeft)), typeContext, typeOperatorContext)
            (newLeft > x > newRight, typeRight)
          }
          case left < x < right => {
            val (newRight, typeRight) = typeSynth(right)
            val (newLeft, typeLeft) = typeSynth(left)(context + ((x, typeRight)), typeContext, typeOperatorContext)
            (newLeft < x < newRight, typeLeft)
          }
          case DeclareDefs(defs, body) => {
            val (newDefs, defBindings) = typeDefs(defs)
            val (newBody, typeBody) = typeSynth(body)(context ++ defBindings, typeContext, typeOperatorContext)
            (DeclareDefs(newDefs, newBody), typeBody) 
          }
          case HasType(body, syntactic.AssertedType(t)) => {
            (expr, lift(t))
          }
          case HasType(body, t) => {
            val expectedType = lift(t)
            val newBody = typeCheck(body, expectedType)
            (HasType(newBody, t), expectedType)
          }
          case DeclareType(u, t, body) => {
            val declaredType = liftEither(t)
            val (newTypeContext, newTypeOperatorContext) = 
              declaredType match {
                case Left(t) => (typeContext + ((u,t)), typeOperatorContext)
                case Right(op) => (typeContext, typeOperatorContext + ((u,op)))
              }
            val (newBody, typeBody) = typeSynth(body)(context, newTypeContext, newTypeOperatorContext) 
            (DeclareType(u, t, newBody), typeBody)
          }
        }
      (expr ->> newExpr, exprType)
    }
    catch {
      case e: TypeException => {
        for ( (x, t) <- context.elements) { System.err.println(x + " :: " + t) }
        throw (e.setPosition(expr.pos))
      }
    }
  }
  
  
  def typeCheck(expr: Expression, T: Type)(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Expression = {
    try {
      expr -> {
        case left || right => {
          val newLeft = typeCheck(left, T)
          val newRight = typeCheck(right, T)
          newLeft || newRight
        }
        case left ow right => {
          val newLeft = typeCheck(left, T)
          val newRight = typeCheck(right, T)
          newLeft ow newRight
        }
        case left > x > right => {
          val (newLeft, typeLeft) = typeSynth(left)
          val newRight = typeCheck(right, T)(context + ((x, typeLeft)), typeContext, typeOperatorContext)
          newLeft > x > newRight
        }
        case left < x < right => {
          val (newRight, typeRight) = typeSynth(right)
          val newLeft = typeCheck(left, T)(context + ((x, typeRight)), typeContext, typeOperatorContext)
          newLeft < x < newRight
        }
        case FoldedCall(target, args, typeArgs) => {
            val (newTarget, typeTarget) = typeSynth(target)
            val (newArgs, argTypes) = (args map typeSynth).unzip
            val (newTypeArgs, _) = typeCall(typeArgs, typeTarget, argTypes, Some(T))
            FoldedCall(newTarget, newArgs, newTypeArgs)
          }
        case FoldedLambda(formals, body, typeFormals, None, None) => {
          T match {
            case FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType) => {
              val argBindings = formals zip liftedArgTypes
              val typeBindings = typeFormals zip liftedTypeFormals
              val newBody = typeCheck(body, liftedReturnType)(context ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)
              val argTypes = liftedArgTypes optionMap reify
              val returnType = reify(liftedReturnType)
              FoldedLambda(formals, newBody, typeFormals, argTypes, returnType)
            }
            case _ => throw new FunctionTypeExpectedException(T)
          }
        }
        case DeclareDefs(defs, body) => {
          val (newDefs, defBindings) = typeDefs(defs)
          val newBody = typeCheck(body, T)(context ++ defBindings, typeContext, typeOperatorContext)
          DeclareDefs(newDefs, newBody)
        }
        case DeclareType(u, t, body) => {
          val declaredType = liftEither(t)
          val (newTypeContext, newTypeOperatorContext) = 
            declaredType match {
              case Left(t) => (typeContext + ((u,t)), typeOperatorContext)
              case Right(op) => (typeContext, typeOperatorContext + ((u,op)))
            }
          val newBody = typeCheck(body, T)(context, typeContext + ((u, lift(t))), typeOperatorContext)
          DeclareType(u, t, newBody)
        }
        case _ => {
          val (newExpr, exprType) = typeSynth(expr)
          exprType assertSubtype T
          newExpr
        }
      }
    }
    catch {
      case e: TypeException => {
        for ( (x, t) <- context.elements) { System.err.println(x + " :: " + t) }
        throw (e.setPosition(expr.pos))
      }
    }
  }
  
  

  def typeDefs(defs: List[Def])(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (List[Def], List[(syntactic.BoundVar, Type)]) = {
    defs match {
      // The return type is absent and we may be able to infer it.
      case List(d@ Def(name, formals, body, typeFormals, Some(argTypes), None)) => {
        if (d.body.freevars contains d.name) {
          // We do not infer return types for recursive functions.
          val e = new UnspecifiedReturnTypeException()
          e.setPosition(d.pos)
          throw e
        }
        else {
          val liftedTypeFormals = typeFormals map { u => new TypeVariable(u) }  
          val liftedArgTypes = argTypes map lift
          
          val argBindings = formals zip liftedArgTypes
          val typeBindings = typeFormals zip liftedTypeFormals
          
          // Note that the function itself is not bound in the context, since we know it is not recursive.
          val (newBody, liftedReturnType) = typeSynth(body)(context ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)
          
          val newDef = d.copy(body = newBody, returntype = reify(liftedReturnType))
          val liftedDefType = FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType)
          
          (List(newDef), List( (name, liftedDefType) ))
        }
      }
      case _ => {
        val defBindings: List[(syntactic.BoundVar, Type)] = 
          for (d <- defs) yield {
            d match {
              case Def(name, _, _, typeFormals, Some(argTypes), Some(returnType)) => {
                val syntacticType = syntactic.FunctionType(typeFormals, argTypes, returnType)
                (name, lift(syntacticType))
              }
              case Def(_,_,_,_,None,_) => {
                val e = new UnspecifiedArgTypesException()
                e.setPosition(d.pos)
                throw e
              }
              case Def(_,_,_,_,_,None) => {
                val e = new UnspecifiedReturnTypeException()
                e.setPosition(d.pos)
                throw e
              }
            }
          }
        val defTypeMap = defBindings.toMap
        val newDefs = {
          for (d <- defs) yield {
            val FunctionType(liftedTypeFormals, liftedArgTypes, liftedReturnType) = defTypeMap(d.name)
            
            val argBindings = d.formals zip liftedArgTypes
            val typeBindings = d.typeformals zip liftedTypeFormals
            
            val newBody = typeCheck(d.body, liftedReturnType)(context ++ defBindings ++ argBindings, typeContext ++ typeBindings, typeOperatorContext)
            
            d.copy(body = newBody)
          }
        }
        (newDefs, defBindings)
        
      }
    }
  }
  
  def typeCall(syntacticTypeArgs: Option[List[syntactic.Type]], targetType: Type, argTypes: List[Type], checkReturnType: Option[Type])(implicit context: Context, typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): (Option[List[syntactic.Type]], Type) = {
    
    if (targetType eq Bot) { return (syntacticTypeArgs, Bot) }
    
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
              for ((s,u) <- argTypes zip funArgTypes) {
                val t = u.subst(typeArgs, funTypeFormals)
                s assertSubtype t
              }
              funReturnType subst (typeArgs, funTypeFormals)
            }
            case ct: CallableType => { 
              /* Type variables cannot be exposed to external types. */
              val safeTypeArgs = typeArgs map { _.clean }
              val safeArgTypes = argTypes map { _.clean }
              ct.call(safeTypeArgs, safeArgTypes) 
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
            for ((s,t) <- argTypes zip funArgTypes) {
              s assertSubtype t
            }
            (Some(Nil), funReturnType)
          }
          case FunctionType(funTypeFormals, funArgTypes, funReturnType) => {
            val argConstraints = 
              for ( (s,t) <- argTypes zip funArgTypes) yield {
                typeConstraints({ _ => false}, s, t)(funTypeFormals.toSet)
              }
            val sigma: Map[TypeVariable, Type] = 
              checkReturnType match {
                case Some(r) => {
                  val returnConstraints = typeConstraints({ _ => false}, funReturnType, r)(funTypeFormals.toSet)
                  val allConstraints = meetAll(argConstraints) meet returnConstraints
                  allConstraints.anySubstitution
                }
                case None => {
                  val allConstraints = meetAll(argConstraints)
                  allConstraints.minimalSubstitution(funReturnType)
                }
              }
            val newReturnType = funReturnType subst sigma
            val newTypeArgs = funTypeFormals map { sigma(_) }
            val newSyntacticTypeArgs = newTypeArgs optionMap reify  
            (newSyntacticTypeArgs, newReturnType) 
          }
          case ct: CallableType => { 
            // FIXME: type variables are allowed to escape to sites here
            // Fix this by special-casing tuple and record creation sites,
            // and using _.clean for argTypes on all other calls.
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
    value match {
      case Signal => SignalType
      case _ : java.lang.Boolean => BooleanType
      case i : scala.math.BigInt => IntegerConstantType(i)
      case _ : scala.math.BigDecimal => NumberType
      case s : TypedSite => s.orcType // SupportForSiteInvocation
      // case _ : String => JavaClassType(java.lang.String.class) 
      // SupportForJavaObjectInvocation
      // SupportForXMLInvocation must be handled separately, since it
      // implicitly extends the Scala type scala.xml.Node
      case _ => Top
    }
  }
  
  
  /**
   * Find constraints on the variables xs such that S <: T
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
      case (f: FunctionType, g: FunctionType) 
      if (f sameShape g) => {
        val (FunctionType(typeFormals, lowerArgTypes, lowerReturnType), 
             FunctionType(          _, upperArgTypes, upperReturnType)) = f shareFormals g
        def Vscope(x: TypeVariable) = {
          ( !(typeFormals contains x) ) && ( V(x) )
        } 
        val C =
          for ( (s, t) <- upperArgTypes zip lowerArgTypes ) yield {
            typeConstraints(Vscope, s, t) 
          }
        val D = {
          typeConstraints(Vscope, lowerReturnType, upperReturnType)
        }
        meetAll(C) meet D
      }
      case (TupleType(elementsBelow), TupleType(elementsAbove)) 
      if (elementsBelow.size == elementsAbove.size) => {
        val C = 
          for ( (s, t) <- elementsBelow zip elementsAbove) yield {
            typeConstraints(V, s, t)
          }
        meetAll(C)
      }
      case (RecordType(entriesBelow), RecordType(entriesAbove)) 
      if ((entriesAbove.keySet) subsetOf (entriesBelow.keySet)) => {
        val C = 
          for (l <- entriesAbove.keys) yield {
            typeConstraints(V, entriesBelow(l), entriesAbove(l))
          }
        meetAll(C)
      }
      case (TypeInstance(tycon, argsBelow), TypeInstance(tyconPrime, argsAbove))
      if (tycon eq tyconPrime) => {
        val C = 
          for ( (v, (s, t)) <- tycon.variances zip (argsBelow zip argsAbove)) yield {
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
      case (s,t) => {
        if (s < t) {
          NoConstraints
        }
        else {
          throw new SubtypeFailureException(s, t)
        }
      }
    
    }
  }
    
  
  
  
  
  
  /** 
   * The following functions lift syntactic types to semantic types.
   * They require contexts to resolve type variables.
   *
   * @author dkitchin
   */
  
  /**
   * Lift a syntactic type to a first-order semantic type.
   */
  def lift(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Type = {
    t match {
      case u: syntactic.BoundTypevar => typeContext(u)
      case syntactic.Top() => Top
      case syntactic.Bot() => Bot
      case syntactic.TupleType(elements) => TupleType(elements map lift)
      case syntactic.RecordType(entries) => RecordType(entries mapValues lift)
      case syntactic.AssertedType(assertedType) => lift(assertedType) 
      case syntactic.FunctionType(typeFormals, argTypes, returnType) => {
        val newTypeFormals = typeFormals map { u => new TypeVariable(u) }
        val typeBindings = typeFormals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newArgTypes = argTypes map { lift(_)(newTypeContext, typeOperatorContext) }
        val newReturnType = lift(returnType)(newTypeContext, typeOperatorContext)
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
      case syntactic.TypeApplication(t, typeactuals) => {
        val operator = liftToOperator(t)
        operator(typeactuals map lift)
      }
      /*
      case syntactic.ImportedType(classname) => {
        // ensure class is a subtype of Type
      }
      
      case syntactic.ClassType(classname) => {
        // ensure class has no generic parameters
      }
      case syntactic.VariantType(variants) => {
        
      }
      */
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case _ => throw new FirstOrderTypeExpectedException()
    }
  }
  
  /**
   * Lift a syntactic type to a second-order semantic type.
   */ 
  def liftToOperator(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): TypeOperator = {
    t match {
      case u: syntactic.BoundTypevar => typeOperatorContext(u)
      /*
      case syntactic.TypeAbstraction(typeformals, syntactic.VariantType(variants)) => {
        
      }
      */
      /*
      case syntactic.ImportedType(classname) => {
        // ensure class is a subtype of TypeOperator
      }
      case syntactic.ClassType(classname) => {
        // ensure class has generic parameters; suspend on those parameters
      }
      */
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case _ => {
        throw new SecondOrderTypeExpectedException()
      }
    }
  }
    
  /**
   * Lift a syntactic type to either a first or second order type;
   * the kind is not known in advance.
   */
  def liftEither(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Either[Type, TypeOperator] = {
    try {
      Left(lift(t))
    }
    catch {
      case _ : FirstOrderTypeExpectedException => {
        Right(liftToOperator(t))
      }
    }
  }
  
  
  
  /** 
   * This function converts a semantic type back to a syntactic type.
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
  def reify(that: Type)(implicit typecontext: TypeContext): Option[syntactic.Type] = {
    None
  }
  
}


/*
 * Type operators.
 * 
 * 
 */
case class TypeOperator(operator: List[Type] => Type) {
    
    def apply(actuals: List[Type]): Type = operator(actuals)
  
    def withArity(arity: Int): TypeOperator = {
      def newOperator(actuals: List[Type]): Type = {
        if (actuals.size != arity) { 
          throw new TypeArityException(arity, actuals.size)
        }
        else {
          operator(actuals)
        }
      }
      TypeOperator(newOperator)
    }
    
}
