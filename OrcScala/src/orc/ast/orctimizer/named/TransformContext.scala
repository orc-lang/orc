//
// TransformContext.scala -- Scala class/trait/object TransformContext
// Project OrcScala
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.orctimizer.named

import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import orc.ast.PrecomputeHashcode
import orc.util.SingletonCache
import orc.error.compiletime.UnboundTypeVariableException
import orc.compile.Logger

/** The context in which an expression appears.
  */
abstract class TransformContext extends PrecomputeHashcode with Product {
  // Chaining implementation for higher efficiency in equality and hashing
  import Bindings._, TransformContext._
  def apply(e: BoundVar): Binding
  def apply(e: BoundTypevar): TypeBinding
  def contains(e: BoundVar): Boolean
  def contains(e: BoundTypevar): Boolean

  def extendBindings(bs: Seq[Binding]): TransformContext = {
    normalize(ExtendBindings(new ArrayBuffer() ++ bs, this))
  }
  def extendTypeBindings(bs: Seq[TypeBinding]): TransformContext = {
    normalize(ExtendTypeBindings(new ArrayBuffer() ++ bs, this))
  }

  def +(b: Binding): TransformContext = {
    normalize(ExtendBindings(ArrayBuffer(b), this))
  }
  def +(b: TypeBinding): TransformContext = {
    normalize(ExtendTypeBindings(ArrayBuffer(b), this))
  }

  def size: Int
  def bindings: Set[Binding]
}

object TransformContext {
  import Bindings._

  def apply(): TransformContext = Empty

  val cache = new SingletonCache[TransformContext]()

  // This is a very important optimization as contexts are constantly compared 
  // to each other and if that's a pointer compare than we win.
  private[TransformContext] def normalize(c: TransformContext) = {
    cache.normalize(c)
  }

  def clear() {
    cache.clear()
  }

  object Empty extends TransformContext {
    def apply(v: BoundVar) = throw new UnboundVariableException(v.toString)
    def apply(v: BoundTypevar) = throw new UnboundTypeVariableException(v.toString)

    def contains(e: BoundVar): Boolean = false
    def contains(e: BoundTypevar): Boolean = false

    def canEqual(that: Any): Boolean = that.isInstanceOf[this.type]
    def productArity: Int = 0
    def productElement(n: Int): Any = ???

    def size = 0
    def bindings: Set[Binding] = Set()

    override def toString = "()"
  }

  case class ExtendBindings(nbindings: ArrayBuffer[Binding], prev: TransformContext) extends TransformContext {
    def apply(v: BoundVar) = nbindings.find(_.variable == v).getOrElse(prev(v))
    def apply(v: BoundTypevar) = prev(v)

    def contains(v: BoundVar): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)
    def contains(v: BoundTypevar): Boolean = prev.contains(v)

    def size = nbindings.length + prev.size
    def bindings = prev.bindings ++ nbindings

    override def toString = s"(${nbindings.mkString(",")})+$prev"

    override def equals(o: Any): Boolean = o match {
      case ExtendBindings(onb, op) if hashCode == o.hashCode => {
        nbindings == onb && prev == op
      }
      case _ => false
    }
  }
  case class ExtendTypeBindings(nbindings: ArrayBuffer[TypeBinding], prev: TransformContext) extends TransformContext {
    def apply(v: BoundVar) = prev(v)
    def apply(v: BoundTypevar) = nbindings.find(_.variable == v).getOrElse(prev(v))

    def contains(v: BoundVar): Boolean = prev.contains(v)
    def contains(v: BoundTypevar): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)

    def size = nbindings.length + prev.size
    def bindings = prev.bindings

    override def toString = s"[${nbindings.mkString(",")}]+$prev"
  }
}

case class TypeBinding(ctx: TransformContext, variable: BoundTypevar) {
  override def toString = s"$productPrefix($variable)"
}

object Bindings {
  /** The class representing binding in the context. The analysis results stored in it refer
    * to a simple direct reference to the variable.
    */
  sealed trait Binding extends Product with PrecomputeHashcode {
    /** The variable that is bound.
      */
    def variable: BoundVar

    val ctx: TransformContext
    val ast: NamedAST

    override def toString = s"$productPrefix($variable)"

    def nonRecursive: Binding = this
  }

  case class SeqBound(ctx: TransformContext, ast: Branch) extends Binding {
    val variable = ast.x
  }

  case class ForceBound(ctx: TransformContext, ast: Force, variable: BoundVar) extends Binding {
    assert(ast.xs.contains(variable))

    def publishForce = ast.publishForce
  }

  case class FutureBound(ctx: TransformContext, ast: Future) extends Binding {
    val variable = ast.x
  }

  case class CallableBound(ctx: TransformContext, ast: DeclareCallables, d: Callable) extends Binding {
    assert(ast.defs.contains(d))
    def variable = d.name
  }

  case class ArgumentBound(ctx: TransformContext, ast: Callable, variable: BoundVar) extends Binding {
    assert(ast.formals.contains(variable))
  }

  case class RecursiveCallableBound(ctx: TransformContext, ast: DeclareCallables, d: Callable) extends Binding {
    assert(ast.defs.contains(d))
    def variable = d.name

    override def nonRecursive: Binding = CallableBound(ctx, ast, d)
  }
}

/** WithContext represents a node paired with the context in which it should be
  * viewed. Some infix notation is provided for working with them.
  */
case class WithContext[+E <: NamedAST](e: E, ctx: TransformContext) {
  override def toString = s"$e<in $ctx>"
}
object WithContext {
  import scala.language.implicitConversions

  implicit def withoutContext[E <: NamedAST](e: WithContext[E]): E = e.e
}

trait WithContextInfixCombinator {
  self: NamedAST =>
  def in(ctx: TransformContext): WithContext[this.type] = WithContext(this, ctx)
}

object in {
  def unapply[E <: NamedAST](c: WithContext[E]): Option[(E, TransformContext)] = Some((c.e, c.ctx))
}

