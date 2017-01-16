//
// NamedToNameless.scala -- Scala trait NamedToNameless
// Project OrcScala
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
import scala.collection.mutable
import orc.error.compiletime.FeatureNotSupportedException

/** @author amp
  */
// Conversions from named to nameless representations
class OILToOrctimizer {  
  private def isDef(b: BoundVar)(implicit ctx: Map[BoundVar, Expression]) = ctx.get(b) match {
    // TODO: How to handle sites?
    case Some(d: DeclareCallables) if d.defs.head.isInstanceOf[Def] => true
    case _ => false
  }
  
  def apply(e: Expression)(implicit ctx: Map[BoundVar, Expression]): orct.Expression = {
    e -> {
      case Stop() => orct.Stop()
      case (a: Argument) > x > e => {
        // Special case to optimize the pattern where we are directly forcing something.
        val bctx = ctx + ((x, e))
        orct.Force(List(apply(x)), List(apply(a)), true, apply(e)(bctx))
      }
      case a: Argument => {
        val x = new orct.BoundVar()
        orct.Force(List(x), List(apply(a)), true, x)
      }
      case Call(target, args, typeargs) => {        
        // TODO: Add special case for site constants and maybe known defs (if we have enough information for that)
        
        val t = new orct.BoundVar(Some(s"f_$target"))
        orct.Force(t, apply(target), false, 
            orct.IfDef(t, {
              orct.CallDef(t, args map apply, typeargs map { _ map apply })
            }, {
              val uniqueArgs = args.toSet.toList
              val argVarsMap = uniqueArgs.map(a => (a, new orct.BoundVar(Some(s"f_$a")))).toMap
              val call = orct.CallSite(t, args map argVarsMap, typeargs map { _ map apply })
              if (uniqueArgs.size > 0) {
                orct.Force(uniqueArgs map argVarsMap, uniqueArgs map apply, true, call)
              } else {
                call
              }
            })
            )
      }
      case left || right => orct.Parallel(apply(left), apply(right))
      case left > x > right => {
        val bctx = ctx + ((x, e))
        orct.Branch(apply(left), apply(x), apply(right)(bctx)) 
      }
      case Graft(x, left, right) => {
        val bctx = ctx + ((x, e))
        orct.Future(apply(x), apply(right), apply(left)(bctx))
      }
      case Trim(f) => orct.Trim(apply(f))
      case left ow right => 
        orct.Otherwise(apply(left), apply(right))
      case DeclareCallables(defs, body) => {
        val bctx = ctx ++ (defs map { d => (d.name, e) })        
        orct.DeclareCallables(defs map { apply(_)(bctx) }, apply(body)(bctx))
      }
      case DeclareType(x, t, body) => {
        orct.DeclareType(apply(x), apply(t), apply(body))
      }
      case HasType(body, expectedType) => orct.HasType(apply(body), apply(expectedType))
      case VtimeZone(timeOrder, body) => orct.VtimeZone(apply(timeOrder), apply(body))
      case FieldAccess(o, f) => {
        val t = new orct.BoundVar(Some(s"f_$o"))
        orct.Force(List(t), List(apply(o)), true, orct.FieldAccess(t, f))
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
  
  def apply(defn: Callable)(implicit ctx: Map[BoundVar, Expression]): orct.Callable = {
    defn -> {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        orct.Def(apply(name), formals map apply, apply(body), typeformals map apply, 
            argtypes map { _ map apply }, returntype map apply)
      }
      case Site(name, formals, body, typeformals, argtypes, returntype) => {
        orct.Site(apply(name), formals map apply, apply(body), typeformals map apply, 
            argtypes map { _ map apply }, returntype map apply)
      }
    }
  }

}
