//
// ContextualTransform.scala -- Scala class/trait/object ContextualTransform
// Project OrcScala
//
// $Id$
//
// Created by amp on May 31, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/** The context in which analysis is occuring.
  */
/*case class TransformContext(val bindings: Map[BoundVar, Bindings.Binding] = Map(), val typeBindings: Map[BoundTypevar, TypeBinding] = Map()) extends PrecomputeHashcode {
  import Bindings._
  def apply(e: BoundVar): Binding = bindings(e)
  def apply(e: BoundTypevar): TypeBinding = typeBindings(e)

  def extendBindings(bs: Iterable[Binding]): TransformContext = {
    new TransformContext(bindings ++ bs.map(b => (b.variable, b)), typeBindings)
  }
  def extendTypeBindings(bs: Iterable[TypeBinding]): TransformContext = {
    new TransformContext(bindings, typeBindings ++ bs.map(b => (b.variable, b)))
  }

  def +(b: Binding): TransformContext = {
    new TransformContext(bindings + ((b.variable, b)), typeBindings)
  }
  def +(b: TypeBinding): TransformContext = {
    new TransformContext(bindings, typeBindings + ((b.variable, b)))
  }
  
  def size = bindings.size + typeBindings.size
}
*/
// Chaining implementation for higher effiency in equality and hashing
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
  
  val cache = mutable.Map[TransformContext, TransformContext]()

  private[TransformContext] def normalize(c: TransformContext) = {
    cache.get(c).getOrElse {
      cache += c -> c
      c
    }
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
    val ast: Orc5CAST
    
    override def toString = s"$productPrefix($variable, ${ast.toString.take(50).replace('\n', ' ')})"
  }

  case class SeqBound(ctx: TransformContext, ast: Sequence) extends Binding {
    val variable = ast.x
  }

  case class LateBound(ctx: TransformContext, ast: LateBind) extends Binding {
    def variable = ast.x
    
    def valueExpr = WithContext(ast.right, ctx)
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

case class WithContext[+E <: Orc5CAST](e: E, ctx: TransformContext)
object WithContext {
  @inline
  implicit def withoutContext[E <: Orc5CAST](e: WithContext[E]): E = e.e
}

trait ContextualTransform extends Orc5CASTFunction {
  import Bindings._
  
  def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E
  
  def apply(a: Argument): Argument = transform(a)(TransformContext())
  def apply(e: Expression): Expression = transform(e)(TransformContext())
  def apply(t: Type): Type = transform(t)(TransformContext())
  def apply(d: Def): Def = transform(d)(TransformContext())

  def onExpression(implicit ctx: TransformContext): PartialFunction[Expression, Expression] = new PartialFunction[Expression, Expression] {
    def isDefinedAt(e: Expression) = pf.isDefinedAt(e in ctx)
    def apply(e: Expression) = pf(e in ctx)
    
    val pf = onExpressionCtx
  }
  def onExpressionCtx: PartialFunction[WithContext[Expression], Expression] = EmptyFunction

  def onArgument(implicit ctx: TransformContext): PartialFunction[Argument, Argument] = EmptyFunction

  def onType(implicit ctx: TransformContext): PartialFunction[Type, Type] = EmptyFunction

  def onDef(implicit ctx: TransformContext): PartialFunction[Def, Def] = EmptyFunction

  def recurseWithContext(implicit ctx: TransformContext) =
    new Orc5CASTFunction {
      def apply(a: Argument) = transform(a)
      def apply(e: Expression) = transform(e)
      def apply(t: Type) = transform(t)
      def apply(d: Def) = transform(d)
    }

  def transform(a: Argument)(implicit ctx: TransformContext): Argument = {
    order[Argument](onArgument, (x:Argument) => x)(a)
    /*val pf = onArgument
    if (pf isDefinedAt a) {
      val v = pf(a)
      a.pushDownPosition(v.pos)
      // We are replacing an argument, do not transfer variable name
      v
    } else
      a*/
  }

  def transform(expr: Expression)(implicit ctx: TransformContext): Expression = {
    val recurse = recurseWithContext
    order[Expression](onExpression, {
        case Stop() => Stop()
        case a: Argument => recurse(a)
        case Call(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          Call(newtarget, newargs, newtypeargs)
        }
        case left || right => recurse(left) || recurse(right)
        case e@(left > x > right) => recurse(left) > x > transform(right)(ctx + SeqBound(ctx, e.asInstanceOf[Sequence]))
        case e@(left < x <| right) => transform(left)(ctx + LateBound(ctx, e.asInstanceOf[LateBind])) < x <| recurse(right)
        case left ow right => recurse(left) ow recurse(right)
        case Limit(f) => Limit(recurse(f))
        case e@DeclareDefs(defs, body) => {
          val newctxrec = ctx extendBindings (defs map { RecursiveDefBound(ctx, e, _) })
          val newdefs = defs map { transform(_)(newctxrec) }
          val newctx = ctx extendBindings (defs map { DefBound(newctxrec, e, _) })
          val newbody = transform(body)(newctx)
          DeclareDefs(newdefs, newbody)
        }
        case e@DeclareType(u, t, body) => {
          val newctx = ctx + TypeBinding(ctx, u)
          val newt = transform(t)(newctx)
          val newbody = transform(body)(newctx)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      })(expr)
    /*val pf = onExpression
    if (pf isDefinedAt e) {
      e -> pf
    } else {
      val recurse = recurseWithContext
      e -> {
        case Stop() => Stop()
        case a: Argument => recurse(a)
        case Call(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          Call(newtarget, newargs, newtypeargs)
        }
        case left || right => recurse(left) || recurse(right)
        case left > x > right => recurse(left) > x > transform(right)(ctx + SeqBound(ctx, e.asInstanceOf[Sequence]))
        case e@(left < x <| right) => transform(left)(ctx + LateBound(ctx, e)) < x <| recurse(right)
        case left ow right => recurse(left) ow recurse(right)
        case Limit(f) => recurse(f)
        case e@DeclareDefs(defs, body) => {
          val newctxrec = ctx extendBindings (defs map { RecursiveDefBound(ctx, e, _) })
          val newdefs = defs map { transform(_)(newctxrec) }
          val newctx = ctx extendBindings (defs map { DefBound(ctx, e, _) })
          val newbody = transform(body)(newctx)
          DeclareDefs(newdefs, newbody)
        }
        case e@DeclareType(u, t, body) => {
          val newctx = ctx + TypeBinding(ctx, u)
          val newt = transform(t)(newctx)
          val newbody = transform(body)(newctx)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      }
    }*/
  }

  def transform(t: Type)(implicit ctx: TransformContext): Type = {
    def recurse(t: Type) = transform(t)
    order[Type](onType, {
        case Bot() => Bot()
        case Top() => Top()
        case ImportedType(cl) => ImportedType(cl)
        case ClassType(cl) => ClassType(cl)
        case u: Typevar => u
        case TupleType(elements) => TupleType(elements map recurse)
        case RecordType(entries) => {
          val newEntries = entries map { case (s, t) => (s, recurse(t)) }
          RecordType(newEntries)
        }
        case TypeApplication(tycon, typeactuals) => {
          TypeApplication(recurse(tycon), typeactuals map recurse)
        }
        case AssertedType(assertedType) => AssertedType(recurse(assertedType))
        case FunctionType(typeformals, argtypes, returntype) => {
          val newtypecontext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})
          val newargtypes = argtypes map { transform(_)(newtypecontext) }
          val newreturntype = transform(returntype)(newtypecontext)
          FunctionType(typeformals, newargtypes, newreturntype)
        }
        case TypeAbstraction(typeformals, t) => {
          TypeAbstraction(typeformals, transform(t)(ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})))
        }
        case VariantType(self, typeformals, variants) => {
          val newTypeContext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)}) + TypeBinding(ctx, self)
          val newVariants =
            for ((name, variant) <- variants) yield {
              (name, variant map { transform(_)(newTypeContext) })
            }
          VariantType(self, typeformals, newVariants)
        }
      })(t)
    /*val pf = onType
    if (pf isDefinedAt t) {
      t -> pf
    } else {
      def recurse(t: Type) = transform(t)
      t -> {
        case Bot() => Bot()
        case Top() => Top()
        case ImportedType(cl) => ImportedType(cl)
        case ClassType(cl) => ClassType(cl)
        case u: Typevar => u
        case TupleType(elements) => TupleType(elements map recurse)
        case RecordType(entries) => {
          val newEntries = entries map { case (s, t) => (s, recurse(t)) }
          RecordType(newEntries)
        }
        case TypeApplication(tycon, typeactuals) => {
          TypeApplication(recurse(tycon), typeactuals map recurse)
        }
        case AssertedType(assertedType) => AssertedType(recurse(assertedType))
        case FunctionType(typeformals, argtypes, returntype) => {
          val newtypecontext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})
          val newargtypes = argtypes map { transform(_)(newtypecontext) }
          val newreturntype = transform(returntype)(newtypecontext)
          FunctionType(typeformals, newargtypes, newreturntype)
        }
        case TypeAbstraction(typeformals, t) => {
          TypeAbstraction(typeformals, transform(t)(ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})))
        }
        case VariantType(self, typeformals, variants) => {
          val newTypeContext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)}) + TypeBinding(ctx, self)
          val newVariants =
            for ((name, variant) <- variants) yield {
              (name, variant map { transform(_)(newTypeContext) })
            }
          VariantType(self, typeformals, newVariants)
        }
      }
    }*/
  }

  def transform(d: Def)(implicit ctx: TransformContext): Def = {
    order[Def](onDef, {
      case d @ Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newcontext = ctx extendBindings (formals map { ArgumentBound(ctx, d, _) }) extendTypeBindings (typeformals map { TypeBinding(ctx, _) })
        val newbody = transform(body)(newcontext)
        val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
        val newreturntype = returntype map { transform(_)(newcontext) }
        Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
      }
    })(d)
    /*val pf = onDef
    if (pf isDefinedAt d) {
      d -> pf
    } else {
      d -> {
        case d@Def(name, formals, body, typeformals, argtypes, returntype) => {
          val newcontext = ctx extendBindings (formals map {ArgumentBound(ctx, d, _)}) extendTypeBindings (typeformals map {TypeBinding(ctx, _)})
          val newbody = transform(body)(newcontext)
          val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
          val newreturntype = returntype map { transform(_)(newcontext) }
          Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
        }
      }
    }*/
  }

}

object ContextualTransform {
  trait NonDescending extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      e ->> pf.applyOrElse(e, descend)
    }
  }
  trait Pre extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = e ->> pf.lift(e).getOrElse(e)
      e1 ->> descend(e1)
    }
  }
  trait Post extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = e ->> descend(e)
      e1 ->> pf.lift(e1).getOrElse(e1)
    }
  }
}
