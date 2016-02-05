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
package orc.ast.porc

import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import orc.ast.PrecomputeHashcode
import scala.ref.WeakReference
import orc.util.SingletonCache

/** The context in which analysis is occuring.
  */
// Chaining implementation for higher effiency in equality and hashing
abstract class TransformContext extends PrecomputeHashcode with Product {
  import TransformContext._
  def apply(e: Var): Binding
  def contains(e: Var): Boolean

  def extendBindings(bs: Seq[Binding]): TransformContext = {
    normalize(ExtendBindings(new ArrayBuffer() ++ bs, this))
  }
  def +(b: Binding): TransformContext = {
    normalize(ExtendBindings(ArrayBuffer(b), this))
  }

  def enclosingTerminator: Option[PorcAST]
  def setTerminator(n: PorcAST): TransformContext = {
    normalize(SetCounterTerminator(enclosingCounter, Some(n), this))
  }

  def enclosingCounter: Option[PorcAST]
  def setCounter(n: PorcAST): TransformContext = {
    normalize(SetCounterTerminator(Some(n), enclosingTerminator, this))
  }

  def setCounterTerminator(n: PorcAST): TransformContext = {
    normalize(SetCounterTerminator(Some(n), Some(n), this))
  }

  // TODO: add enclosing exception context, this will not effect compatibility since it is always dynamic anyway

  def compatibleFor(e: Expr)(o: TransformContext): Boolean = {
    compatibleForSite(e)(o)
  }
  def compatibleForSite(e: Expr)(o: TransformContext): Boolean = {
    val fv = e.freevars
    bindings.filter(b => fv.contains(b.variable)) == o.bindings.filter(b => fv.contains(b.variable))
  }

  def size: Int
  def bindings: Set[Binding]
}

object TransformContext {
  def apply(): TransformContext = Empty

  val cache = new SingletonCache[TransformContext]()
  //mutable.WeakHashMap[TransformContext, WeakReference[TransformContext]]()

  // This is a very important optimization as contexts are constantly compared to each other and if that's a pointer compare than we win.
  private[TransformContext] def normalize(c: TransformContext) = {
    cache.normalize(c)
  }
  
  def clear() {
    cache.clear()
  }

  object Empty extends TransformContext {
    def apply(v: Var) = throw new NoSuchElementException(v.toString)
    def contains(e: Var): Boolean = false

    def canEqual(that: Any): Boolean = that.isInstanceOf[this.type]
    def productArity: Int = 0
    def productElement(n: Int): Any = ???

    def enclosingTerminator = None
    def enclosingCounter = None

    def size = 0
    def bindings: Set[Binding] = Set()
  }

  case class ExtendBindings(nbindings: ArrayBuffer[Binding], prev: TransformContext) extends TransformContext {
    def apply(v: Var) = nbindings.find(_.variable == v).getOrElse(prev(v))
    def contains(v: Var): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)

    def enclosingTerminator = prev.enclosingTerminator
    def enclosingCounter = prev.enclosingCounter

    def size = nbindings.length + prev.size
    def bindings = prev.bindings ++ nbindings
  }
  case class SetCounterTerminator(enclosingCounter: Option[PorcAST], enclosingTerminator: Option[PorcAST], prev: TransformContext) extends TransformContext {
    def apply(v: Var) = prev(v)
    def contains(v: Var): Boolean = prev.contains(v)

    def size = prev.size
    def bindings = prev.bindings
  }
}

/** The class representing binding in the context. The analysis results stored in it refer
  * to a simple direct reference to the variable.
  */
sealed trait Binding extends PrecomputeHashcode with Product {
  /** The variable that is bound.
    */
  def variable: Var

  val ctx: TransformContext
  val ast: PorcAST

  override def toString = s"$productPrefix($variable)" //${ast.toString.take(50).replace('\n', ' ')}
}

case class StrictBound(ctx: TransformContext, ast: Expr, variable: Var) extends Binding

case class LetBound(ctx: TransformContext, ast: Let) extends Binding {
  def variable: Var = ast.x
}

case class SiteBound(ctx: TransformContext, ast: Site, d: SiteDef) extends Binding {
  assert(ast.defs.contains(d))
  def variable: Var = d.name
}
case class RecursiveSiteBound(ctx: TransformContext, ast: Site, d: SiteDef) extends Binding {
  assert(ast.defs.contains(d))
  def variable: Var = d.name
}

case class SiteArgumentBound(ctx: TransformContext, ast: SiteDef, variable: Var) extends Binding {
  assert(ast.allArguments.contains(variable))
}

case class ContinuationArgumentBound(ctx: TransformContext, ast: Continuation, variable: Var) extends Binding {
  assert(ast.argument == variable)
}

final case class WithContext[+E <: PorcAST](e: E, ctx: TransformContext) {
  def subtrees: Iterable[WithContext[PorcAST]] = this match {
    case LetIn(x, v, b) => Seq(v, b)
    case SiteIn(l, ctx, b) => l.map(_ in ctx).toSeq :+ b
    case SiteDefIn(n, args, ctx, b) => Seq(b)

    case CallIn(t, a, ctx) => Seq(t) :+ (a in ctx)
    case SiteCallIn(target, p, c, t, args, ctx) => Seq(target, p in ctx, c in ctx, t in ctx) ++ args.map(_ in ctx)
    case SiteCallDirectIn(target, a, ctx) => Seq(target) ++ a.map(_ in ctx)

    case ContinuationIn(args, ctx, b) => Seq(b)

    case NewCounterIn(c, h) => Seq(h)

    case e in ctx => e.subtrees.collect { case e : PorcAST => e in ctx }
  }
}
object WithContext {
  import scala.language.implicitConversions

  @inline
  implicit def withoutContext[E <: PorcAST](e: WithContext[E]): E = e.e
}