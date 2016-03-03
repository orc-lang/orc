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
import java.util.logging.Level

/** The context in which analysis is occuring.
  */
// Chaining implementation for higher effiency in equality and hashing
abstract class TransformContext extends PrecomputeHashcode with Product {
  import TransformContext._
  def apply(e: Var): Binding
  def contains(e: Var): Boolean

  def extendBindings(bs: Seq[Binding]): TransformContext = {
    normalize(ExtendBindings(bs.toList, this))
  }
  def +(b: Binding): TransformContext = {
    normalize(ExtendBindings(List(b), this))
  }
  
  def lookupBoundTo(v: Expr): Option[LetBound]    

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
  var maxCacheSize = 0

  // This is a very important optimization as contexts are constantly compared to each other and if that's a pointer compare then we win.
  private[TransformContext] def normalize(c: TransformContext) = {
    val r = cache.normalize(c)
    if ((r eq c) && Logger.julLogger.isLoggable(Level.FINER)) {
      c match {
        case ExtendBindings((ctop: LetBound) :: _, _) => {
          val messages = for ((ext, i) <- cache.items.collect({ case e@ExtendBindings((b: LetBound) :: _, _) => (e, b) }) if i.ast == ctop.ast) yield {
            s"Found match: $ext"
          }
          if(messages.size > 1) {
            Logger.finer(s"Looking for contexts which match: $ctop\n${messages.mkString("\n")}")
          }
        }
        case _ => {}
      }
    }
    if (cache.size > maxCacheSize + 500 && Logger.julLogger.isLoggable(Level.FINE)) {
      Logger.fine(s"Porc context cache is ${cache.size} elements.")
      maxCacheSize = cache.size
    }
    r
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

    def lookupBoundTo(v: Expr): Option[LetBound] = None

    def size = 0
    def bindings: Set[Binding] = Set()
  }

  case class ExtendBindings(nbindings: List[Binding], prev: TransformContext) extends TransformContext {
    def apply(v: Var) = nbindings.find(_.variable == v).getOrElse(prev(v))
    def contains(v: Var): Boolean = nbindings.find(_.variable == v).isDefined || prev.contains(v)

    def enclosingTerminator = prev.enclosingTerminator
    def enclosingCounter = prev.enclosingCounter

    def size = nbindings.length + prev.size
    def bindings = prev.bindings ++ nbindings

    def lookupBoundTo(v: Expr): Option[LetBound] = {
      val thisRes = nbindings.collect({
        case b@LetBound(_, Let(_, lv, _)) if lv == v => b
      }).headOption
      if (thisRes.isDefined)
        thisRes
      else
        prev.lookupBoundTo(v)
    }

    override def +(b: Binding): TransformContext = {
      normalize(ExtendBindings(b :: nbindings, prev))
    }
  }
  
  case class SetCounterTerminator(enclosingCounter: Option[PorcAST], enclosingTerminator: Option[PorcAST], prev: TransformContext) extends TransformContext {
    def apply(v: Var) = prev(v)
    def contains(v: Var): Boolean = prev.contains(v)

    def size = prev.size
    def bindings = prev.bindings

    def lookupBoundTo(v: Expr): Option[LetBound] = prev.lookupBoundTo(v)
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
  def value = ast.v
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

case class SpawnFutureBound(ctx: TransformContext, ast: SpawnFuture, variable: Var) extends Binding {
  assert(ast.pArg == variable || ast.cArg == variable)
}

//case class BoundTo(ctx: TransformContext, ast: Let) extends PrecomputeHashcode {
//  def variable = ast.x
//  def value = ast.v
//}

final case class WithContext[+E <: PorcAST](e: E, ctx: TransformContext) extends PrecomputeHashcode {
  def subtrees: Iterable[WithContext[PorcAST]] = this match {
    case LetIn(x, v, b) => Seq(v, b)
    case SiteIn(l, ctx, b) => l.map(_ in ctx).toSeq :+ b
    case SiteDefIn(n, args, ctx, b) => Seq(b)

    case CallIn(t, a, ctx) => Seq(t) :+ (a in ctx)
    case SiteCallIn(target, p, c, t, args, ctx) => Seq(target, p in ctx, c in ctx, t in ctx) ++ args.map(_ in ctx)
    case SiteCallDirectIn(target, a, ctx) => Seq(target) ++ a.map(_ in ctx)
    
    case SpawnFutureIn(c, t, pArg, cArg, expr) => Seq(c, t, expr)

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