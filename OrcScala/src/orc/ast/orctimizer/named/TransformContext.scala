//
// TransformContext.scala -- Scala class/trait/object TransformContext
// Project OrcScala
//
// $Id$
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

/** The context in which analysis is occuring.
  */
/* This implementation is simplier but seems to be slightly slower and this actually is a hotspot.
case class TransformContext(val _bindings: Map[BoundVar, Bindings.Binding] = Map(), val typeBindings: Map[BoundTypevar, TypeBinding] = Map()) extends PrecomputeHashcode {
  import Bindings._, TransformContext._
  def apply(e: BoundVar): Binding = _bindings(e)
  def apply(e: BoundTypevar): TypeBinding = typeBindings(e)
  def contains(e: BoundVar): Boolean = _bindings.contains(e)
  def contains(e: BoundTypevar): Boolean = typeBindings.contains(e)

  def extendBindings(bs: Iterable[Binding]): TransformContext = {
    normalize(new TransformContext(_bindings ++ bs.map(b => (b.variable, b)), typeBindings))
  }
  def extendTypeBindings(bs: Iterable[TypeBinding]): TransformContext = {
    normalize(new TransformContext(_bindings, typeBindings ++ bs.map(b => (b.variable, b))))
  }

  def +(b: Binding): TransformContext = {
    normalize(new TransformContext(_bindings + ((b.variable, b)), typeBindings))
  }
  def +(b: TypeBinding): TransformContext = {
    normalize(new TransformContext(_bindings, typeBindings + ((b.variable, b))))
  }
  
  def size = _bindings.size + typeBindings.size
  def bindings = _bindings.values.toSet
}*/

// Chaining implementation for higher efficiency in equality and hashing
abstract class TransformContext extends PrecomputeHashcode with Product {
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
  
  def apply():TransformContext = Empty
  
  val cache = new SingletonCache[TransformContext]()

  // This is a very important optimization as contexts are constantly compared to each other and if that's a pointer compare than we win.
  private[TransformContext] def normalize(c: TransformContext) = {
    cache.normalize(c)
  }
  
  def clear() {
    cache.clear()
  }
  
  object Empty extends TransformContext {
    def apply(v: BoundVar) = throw new UnboundVariableException(v.toString)
    def apply(v: BoundTypevar) = throw new UnboundVariableException(v.toString)
    
    def contains(e: BoundVar): Boolean = false
    def contains(e: BoundTypevar): Boolean = false

    def canEqual(that: Any): Boolean = that.isInstanceOf[this.type] 
    def productArity: Int = 0 
    def productElement(n: Int): Any = ??? 
    
    def size = 0
    def bindings: Set[Binding] = Set()
  }
  
  case class ExtendBindings(nbindings: ArrayBuffer[Binding], prev: TransformContext) extends TransformContext {
    def apply(v: BoundVar) = nbindings.find(_.variable == v).getOrElse(prev(v))
    def apply(v: BoundTypevar) = prev(v) 

    def contains(v: BoundVar): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)
    def contains(v: BoundTypevar): Boolean = prev.contains(v)

    def size = nbindings.length + prev.size
    def bindings = prev.bindings ++ nbindings
  }
  case class ExtendTypeBindings(nbindings: ArrayBuffer[TypeBinding], prev: TransformContext) extends TransformContext {
    def apply(v: BoundVar) = prev(v)
    def apply(v: BoundTypevar) = nbindings.find(_.variable == v).getOrElse(prev(v))

    def contains(v: BoundVar): Boolean = prev.contains(v)
    def contains(v: BoundTypevar): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)

    def size = nbindings.length + prev.size
    def bindings = prev.bindings
  }
}

case class TypeBinding(ctx: TransformContext, variable: BoundTypevar) {
  override def toString = s"$productPrefix($variable)"
}

object Bindings {
  /** The class representing binding in the context. The analysis results stored in it refer
    * to a simple direct reference to the variable.
    */
  sealed trait Binding extends PrecomputeHashcode with Product {
    /** The variable that is bound.
      */
    def variable: BoundVar

    val ctx: TransformContext
    val ast: NamedAST
    
    override def toString = s"$productPrefix($variable)" //${ast.toString.take(50).replace('\n', ' ')}
  }

  case class SeqBound(ctx: TransformContext, ast: Sequence) extends Binding {
    val variable = ast.x
  }
  
  case class DefBound(ctx: TransformContext, ast: DeclareDefs, d: Def) extends Binding {
    assert(ast.defs.contains(d))
    def variable = d.name
  }

  case class ArgumentBound(ctx: TransformContext, ast: Def, variable: BoundVar) extends Binding {
    assert(ast.formals.contains(variable))
  }

  case class RecursiveDefBound(ctx: TransformContext, ast: DeclareDefs, d: Def) extends Binding {
    assert(ast.defs.contains(d))
    def variable = d.name
  }
}


/**
 * WithContext represents a node paired with the context in which it should be 
 * viewed. Some infix notation is provided for working with them.
 */
case class WithContext[+E <: NamedAST](e: E, ctx: TransformContext)
object WithContext {
  import scala.language.implicitConversions
  
  @inline
  implicit def withoutContext[E <: NamedAST](e: WithContext[E]): E = e.e
}

trait WithContextInfixCombinator {
  self: NamedAST =>
  def in(ctx: TransformContext): WithContext[this.type] = WithContext(this, ctx)
}

object in {
  def unapply[E <: NamedAST](c: WithContext[E]): Option[(E, TransformContext)] = Some((c.e, c.ctx))
}

