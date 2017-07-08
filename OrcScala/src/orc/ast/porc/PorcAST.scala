//
// PorcAST.scala -- Scala class/trait/object PorcAST
// Project OrcScala
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.values.sites
import orc.ast.PrecomputeHashcode
import java.util.logging.Level
import orc.values.Field
import orc.ast.ASTForSwivel
import swivel.root
import swivel.replacement
import swivel.leaf
import swivel.branch
import swivel.subtree
import orc.ast.hasAutomaticVariableName
import scala.PartialFunction
import swivel.TransformFunction
import orc.util.Ternary

/* TODO: Also change to simplify the truffle implementation but without increasing the complexity of say a C or JS implementation.
 * 
 * Continuation is not needed. Each time we use it we could instead have an expression to call as the continuation.
 * This would effectively defer CPS conversion to the backend. However it also sets aside the continuations so that conversion would be trivial.
 * Since we can now have specialized argument bindings for each continuation. This should avoid the need for builtin TupleElim.
 * 
 * The problem with this is that it can result in code duplication when there is more than one call to the continuation
 * (which there is in the presence of parallel). This means there will still need to be something like Continuation.
 * It might be useful to give it "join" (like the Jones talk) semantics were it is limited and guaranteed to be
 * compilable to a jump.
 * 
 * Let is still needed.
 * 
 * The continuations which appear inline would still need information to assist in capture. This would be
 * argument names and free variables which will be captured from the scope. This would make continuation lifting
 * trivial in the backend.
 * 
 * Defs and sites should also include captured variables to allow for compaction and easier lifting.
 * 
 * Can defs and sites be declared the same way?
 *   It should be possible. A site simply captures T from it's declaring scope and ignores it's parameter T.
 *   Sites will always receive non-futures as arguments. Sites will also artificially increment the halt count to prevent the declaration from ever halting.
 *   Defs will not capture T and will have future arguments.
 *   
 *   Calls to these values are all the same. The calls receive a continuation arg and other useful args.
 *   
 *   Eliminate direct calls and replace with a CPS version with a flag marking calls which are statically known to be direct.
 *   
 * 
 * Can that system be reused to dedup continuations?
 *   Probably not. Since continuations should have guaranteed jump semantics.
 *
 * Even continuations will not always be implementable as a jump.
 * The problem is that in the presence of parallel a continuation is called in a non-tail position.
 * 
 * 
 * 
 * It appears that very little can actually easily change.
 * Continuations are multiple called in parallel so they both cannot be jumps and cannot be inlined in 
 * all cases (because of potential code explosion).
 * Def and site decl and call can be combined, but splitting it out to allow static optimization or
 * simply better start-up of the JIT (because more is known the first compile) may be useful.
 * 
 * Continuations could be expanded to multiple parameters to eliminate the need for any tuples.
 * SpawnFutureBind could be changed to take a continuation which takes either a tuple of P and C or
 * multiple parameters.
 * Spawn could take zero args or a unit arg.
 * 
 * These changes would simplify the language by preventing any code from appearing inside operators
 * like spawn. However, the opposite would also be possible. Remove explicit continuations and
 * have explicit code blocks in all positions. The problem with this is that the same continuation 
 * is used more than once in some cases, so explicit continuations will remove that duplication.
 * 
 * Forced inlining of the continuations (on speculative direct paths) would still be possible by
 * making the continuation and it's body into compile time constants. However, truffle and graal have
 * their own inlining which may well solve the problem without the need for forced inlining. Spectulative
 * directifying may still be useful, but the P call would be a normal call.
 * 
 * Multiple parameter continuations would be slightly harder to implement on some backends. However, 
 * they would probably be faster in almost all cases since they would allow the underlying compiler
 * to store both values on the stack without needing to detect the potential for on-stack scalar replacement.
 * 
 */

abstract class Transform extends TransformFunction {
  def onExpression: PartialFunction[Expression.Z, Expression] = {
    case a: Argument.Z if onArgument.isDefinedAt(a) => onArgument(a)
  }
  def apply(e: Expression.Z) = transformWith[Expression.Z, Expression](e)(this, onExpression)

  def onArgument: PartialFunction[Argument.Z, Argument] = PartialFunction.empty
  def apply(e: Argument.Z) = transformWith[Argument.Z, Argument](e)(this, onArgument)

  def onMethod: PartialFunction[Method.Z, Method] = PartialFunction.empty
  def apply(e: Method.Z) = transformWith[Method.Z, Method](e)(this, onMethod)

  def apply(e: PorcAST.Z): PorcAST = e match {
    case e: Argument.Z => apply(e)
    case e: Expression.Z => apply(e)
    case e: Method.Z => apply(e)
  }
}

/** @author amp
  */
@root @transform[Transform]
sealed abstract class PorcAST extends ASTForSwivel with Product {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  override def toString() = prettyprint()

  def boundVars: Set[Variable] = Set()
}

// ==================== CORE ===================
@branch @replacement[Expression]
sealed abstract class Expression extends PorcAST with FreeVariables with Substitution[Expression] with PrecomputeHashcode with PorcInfixExpr

object Expression {
  class Z {
    def contextBoundVars = {
      parents.flatMap(_.value.boundVars)
    }
    def freeVars = {
      value.freeVars
    }
    def binderOf(x: Variable) = {
      parents.find(_.value.boundVars.contains(x))
    }
  }
}

@branch
sealed abstract class Argument extends Expression with PorcInfixValue with PrecomputeHashcode
@leaf @transform
final case class Constant(v: AnyRef) extends Argument
@leaf @transform
final case class PorcUnit() extends Argument
@leaf @transform
final class Variable(val optionalName: Option[String] = None) extends Argument with hasAutomaticVariableName {
  optionalVariableName = optionalName
  autoName("pv")

  def this(s: String) = this(Some(s))

  override def productIterator = Nil.iterator
  // Members declared in scala.Equals
  def canEqual(that: Any): Boolean = that.isInstanceOf[Variable]
  // Members declared in scala.Product
  def productArity: Int = 0
  def productElement(n: Int): Any = ???

  override val hashCode = System.identityHashCode(this)
}

@leaf @transform
final case class CallContinuation(@subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression

@leaf @transform
final case class Let(x: Variable, @subtree v: Expression, @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = Set(x)
}

@leaf @transform
final case class Sequence(@subtree es: Seq[Expression]) extends Expression {
  //assert(!es.isEmpty)

  def simplify = es match {
    case Seq(e) => e
    case _ => this
  }
}
object Sequence {
  def apply(es: Seq[Expression]): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case e => Seq(e)
    }).toList)
  }
}

@leaf @transform
final case class Continuation(arguments: Seq[Variable], @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = arguments.toSet
}

@leaf @transform
final case class MethodDeclaration(@subtree defs: Seq[Method], @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = defs.map(_.name).toSet
}

@branch @replacement[Method]
sealed abstract class Method extends PorcAST {
  def name: Variable
  def arguments: Seq[Variable]
  def body: Expression
  def isDef: Boolean

  def allArguments: Seq[Variable]
  
  override def boundVars: Set[Variable] = allArguments.toSet
}

object Method {
  class Z {
    def name: Variable
    def arguments: Seq[Variable]
    def allArguments: Seq[Variable] = value.allArguments
    def body: Expression.Z    
  }
}

// TODO: Currently Methods are not handled correctly. Specifically defs never correctly force any futures.
//       We will need either a resolve combinator here or a split between routines and services.

@leaf @transform
final case class MethodCPS(name: Variable, pArg: Variable, cArg: Variable, tArg: Variable, isDef: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  override def allArguments: Seq[Variable] = pArg +: cArg +: tArg +: arguments
}
@leaf @transform
final case class MethodDirect(name: Variable, isDef: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  override def allArguments: Seq[Variable] = arguments
}

@leaf @transform
final case class MethodCPSCall(isExternal: Ternary, @subtree target: Argument, @subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree arguments: Seq[Argument]) extends Expression
@leaf @transform
final case class MethodDirectCall(isExternal: Ternary, @subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression

@leaf @transform
final case class IfDef(@subtree argument: Argument, @subtree left: Expression, @subtree right: Expression) extends Expression

@leaf @transform
final case class GetField(@subtree future: Argument, field: Field) extends Expression

@leaf @transform
final case class New(@subtree bindings: Map[Field, Argument]) extends Expression

// ==================== PROCESS ===================

// TODO: The semantics of this have been changed to "spawn or run as the runtime prefers"
@leaf @transform
final case class Spawn(@subtree c: Argument, @subtree t: Argument, @subtree computation: Argument) extends Expression

@leaf @transform
final case class NewTerminator(@subtree parentT: Argument) extends Expression
@leaf @transform
final case class Kill(@subtree t: Argument) extends Expression
@leaf @transform
final case class TryOnKilled(@subtree body: Expression, @subtree handler: Expression) extends Expression

@leaf @transform
final case class NewCounter(@subtree parentT: Argument, @subtree haltHandler: Argument) extends Expression
@leaf @transform
final case class Halt(@subtree c: Argument) extends Expression
@leaf @transform
final case class SetDiscorporate(@subtree c: Argument) extends Expression
@leaf @transform
final case class TryOnHalted(@subtree body: Expression, @subtree handler: Expression) extends Expression

@leaf @transform
final case class TryFinally(@subtree body: Expression, @subtree handler: Expression) extends Expression

// ==================== FUTURE ===================

@leaf @transform
final case class NewFuture() extends Expression
@leaf @transform
final case class SpawnBindFuture(@subtree fut: Argument, @subtree c: Argument, @subtree t: Argument, @subtree computation: Argument) extends Expression
@leaf @transform
final case class Force(@subtree p: Argument, @subtree c: Argument, @subtree t: Argument, forceClosures: Boolean, @subtree futures: Seq[Argument]) extends Expression
