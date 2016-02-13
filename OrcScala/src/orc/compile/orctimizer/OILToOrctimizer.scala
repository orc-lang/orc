//
// NamedToNameless.scala -- Scala trait NamedToNameless
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.ast.oil.named._
import orc.ast.orctimizer.{named => orct}
import orc.lib.state.NewFlag
import orc.lib.state.PublishIfNotSet
import orc.lib.state.SetFlag
import scala.collection.mutable
import orc.lib.builtin.MakeSite
import orc.error.compiletime.FeatureNotSupportedException

/** @author amp
  */
// Conversions from named to nameless representations
class OILToOrctimizer {
  private def newFlag() = {
    orct.Call(orct.Constant(NewFlag), List(), None)
  }
  private def setFlag(flag: orct.BoundVar) = {
    orct.Call(orct.Constant(SetFlag), List(flag), None)
  }
  private def publishIfNotSet(flag: orct.BoundVar) = {
    orct.Call(orct.Constant(PublishIfNotSet), List(flag), None)
  }
  
  private def isDef(b: BoundVar)(implicit ctx: Map[BoundVar, Expression]) = ctx.get(b) match {
    case Some(_: DeclareDefs) => true
    case _ => false
  }
  
  def apply(e: Expression)(implicit ctx: Map[BoundVar, Expression]): orct.Expression = {
    e -> {
      case Stop() => orct.Stop()
      case a: Argument => orct.Force(apply(a))
      case Call(target, args, typeargs) => {
        // Compute the target value and a flag to state of it should be forced
        val (newTarget, forceTarget) = target match {
          case Constant(MakeSite) =>
            throw new FeatureNotSupportedException("Classes or MakeSite", e.pos)

          case (c: Constant) =>
            (apply(c), None)
          case (x: BoundVar) if isDef(x) =>
            (apply(x), None)
          case _ =>
            val t = new orct.BoundVar(Some(s"f_$target"))
            (t, Some(t))
        }

        // Compute the set of arguments and collect arguments that need forcing
        val bindings = new collection.mutable.ListBuffer[(Argument, orct.BoundVar)]()
        val newArgs = args map { arg =>
          arg match {
            case c: Constant =>
              apply(c)
            case _ =>
              /*val newArg = new orct.BoundVar(Some(s"f_$arg"))
              bindings += ((arg, newArg))
              newArg
              */
              apply(arg)
          }
        }
        
        // Build the actual call
        val call = orct.Call(newTarget, newArgs, typeargs map { _ map apply })
        
        // Generate forcing operations for arguments
        /*val argsBound = bindings.foldRight(call: orct.Expression) { (as, acc) =>
          val (arg, newArg) = as
          orct.Sequence(orct.Future(orct.Force(apply(arg))), newArg, acc)
        }*/
        
        // Force target if needed
        forceTarget map { t =>
          orct.Sequence(orct.Force(apply(target)), t, call)
        } getOrElse {
          call
        }
      }
      case left || right => orct.Parallel(apply(left), apply(right))
      case left > x > right => {
        val bctx = ctx + ((x, e))
        orct.Sequence(apply(left), apply(x), apply(right)(bctx)) 
      }
      case left < x <| right => {
        val bctx = ctx + ((x, e))
        orct.Sequence(orct.Future(apply(right)), apply(x), apply(left)(bctx))
      }
      case Limit(f) => orct.Limit(apply(f))
      case left ow right => 
        val x = new orct.BoundVar()
        val flag = new orct.BoundVar()
        val cl = orct.Sequence(apply(left), x, orct.Sequence(setFlag(flag), new orct.BoundVar(), x))
        val cr = orct.Sequence(publishIfNotSet(flag), new orct.BoundVar(), apply(right))
        orct.Sequence(newFlag(), flag, orct.Concat(cl, cr))
      case DeclareDefs(defs, body) => {
        val bctx = ctx ++ (defs map { d => (d.name, e) })        
        orct.DeclareDefs(defs map { apply(_)(bctx) }, apply(body)(bctx))
      }
      case DeclareType(x, t, body) => {
        orct.DeclareType(apply(x), apply(t), apply(body))
      }
      case HasType(body, expectedType) => orct.HasType(apply(body), apply(expectedType))
      case VtimeZone(timeOrder, body) => orct.VtimeZone(apply(timeOrder), apply(body))
      case FieldAccess(o, f) => {
        val t = new orct.BoundVar(Some(s"f_$o"))
        orct.Force(apply(o)) > t > orct.FieldAccess(t, f)
      }
    }    
  }

  def apply(a: Argument): orct.Argument = {
    a -> {
      case Constant(v) => orct.Constant(v)
      case (x: BoundVar) => apply(x)
      case UnboundVar(s) => orct.UnboundVar(s)
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in OILToOrctimizer")
    }
  }

  val boundVarCache = mutable.HashMap[BoundVar, orct.BoundVar]()
    
  def apply(a: BoundVar): orct.BoundVar = {
    a -> {
      case (x: BoundVar) => {
        boundVarCache.getOrElseUpdate(x, new orct.BoundVar())
      }
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in OILToOrctimizer")
    }
  }

  def apply(t: Type): orct.Type = {
    t -> {
      case u: BoundTypevar => new orct.BoundTypevar()
      case Top() => orct.Top()
      case Bot() => orct.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        orct.FunctionType(typeformals map apply, argtypes map apply, apply(returntype))
      }
      case TupleType(elements) => orct.TupleType(elements map apply)
      case RecordType(entries) => {
        val newEntries = entries map { case (s, t) => (s, apply(t)) }
        orct.RecordType(newEntries)
      }
      case TypeApplication(tycon, typeactuals) => {
        orct.TypeApplication(apply(tycon), typeactuals map apply)
      }
      case AssertedType(assertedType) => orct.AssertedType(apply(assertedType))
      case TypeAbstraction(typeformals, t) => {
        orct.TypeAbstraction(typeformals map apply, apply(t))
      }
      case ImportedType(classname) => orct.ImportedType(classname)
      case ClassType(classname) => orct.ClassType(classname)
      case VariantType(self, typeformals, variants) => {
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map apply)
          }
        orct.VariantType(apply(self), typeformals map apply, newVariants)
      }
      case UnboundTypevar(s) => orct.UnboundTypevar(s)
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamedToNameless.namedToNameless(Type, List[BoundTypeVar])")
    }
  }
  
  def apply(t: BoundTypevar): orct.BoundTypevar = {
    t -> {
      case u: BoundTypevar => new orct.BoundTypevar()
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamedToNameless.namedToNameless(Type, List[BoundTypeVar])")
    }
  }
  
  def apply(defn: Def)(implicit ctx: Map[BoundVar, Expression]): orct.Def = {
    defn -> {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        orct.Def(apply(name), formals map apply, apply(body), typeformals map apply, 
            argtypes map { _ map apply }, returntype map apply)
      }
    }
  }

}
