//
// OILToOrctimizer.scala -- Scala class OILToOrctimizer
// Project OrcScala
//
// Created by amp on Sep 6, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.ast.oil.named._
import orc.ast.orctimizer.{ named => orct }
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

  /*
   * TODO:
   *
   * Determine encoding for orc sites into Orctimizer
   * Add Orctimizer AST nodes for sites (as needed) and object instantiation.
   * Implement translation from OIL to Orct.
   * Check the results manually for a number of cases.
   *
   * Do not implement Vtime, but do insert comments about implementing onIdle.
   */

  def apply(e: Expression)(implicit ctx: Map[BoundVar, Expression]): orct.Expression = {
    e -> {
      case Stop() => orct.Stop()
      case Sequence(a: Argument, x, e) => {
        // Special case to optimize the pattern where we are directly forcing something.
        val bctx = ctx + ((x, e))
        orct.Force(List(apply(x)), List(apply(a)), true, apply(e)(bctx))
      }
      case a: Constant => {
        apply(a)
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
          }))
      }
      case Parallel(left, right) => orct.Parallel(apply(left), apply(right))
      case Sequence(left, x, right) => {
        val bctx = ctx + ((x, e))
        orct.Branch(apply(left), apply(x), apply(right)(bctx))
      }
      case Graft(x, left, right) => {
        val bctx = ctx + ((x, e))
        orct.Branch(orct.Future(apply(right)), apply(x), apply(left)(bctx))
      }
      case Trim(f) => orct.Trim(apply(f))
      case Otherwise(left, right) =>
        orct.Otherwise(apply(left), apply(right))
      case DeclareCallables(defs, body) => {
        val bctx = ctx ++ (defs map { d => (d.name, e) })
        orct.DeclareCallables(defs map { apply(_)(bctx) }, apply(body)(bctx))
      }
      case DeclareType(x, t, body) => {
        orct.DeclareType(apply(x), apply(t), apply(body))
      }
      case HasType(body, expectedType) => orct.HasType(apply(body), apply(expectedType))
      case FieldAccess(o, f) => {
        val t = new orct.BoundVar(Some(s"f_$o"))
        val fv1 = new orct.BoundVar(Some(s"f_${o}_${f.field}'"))
        val fv2 = new orct.BoundVar(Some(s"f_${o}_${f.field}"))
        orct.Force(List(t), List(apply(o)), true, {
          orct.FieldAccess(t, f) > fv1 >
          orct.Force(List(fv2), List(fv1), true, {
            fv2
          })
        })
      }
      case New(self, selfT, members, objT) => {
        val bctx = ctx + ((self, e))
        val newMembers = members.mapValues(e => e match {
          case a: Argument =>
            orct.FieldArgument(apply(a))
          case _ =>
            orct.FieldFuture(apply(e)(bctx))
        }).view.force
        orct.New(apply(self), selfT map apply, newMembers, objT map apply)
      }

      case VtimeZone(timeOrder, body) =>
        // TODO: Implement onIdle in the Orctimizer
        throw new FeatureNotSupportedException("Virtual time").setPosition(e.sourceTextRange.getOrElse(null))
      case Hole(_, _) =>
        throw new FeatureNotSupportedException("Hole").setPosition(e.sourceTextRange.getOrElse(null))
    }
  }

  def apply(a: Argument): orct.Argument = {
    a -> {
      case Constant(v) => orct.Constant(v)
      case (x: BoundVar) => apply(x)
      case UnboundVar(s) => orct.UnboundVar(s)
      //case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in OILToOrctimizer")
    }
  }

  val boundVarCache = mutable.HashMap[BoundVar, orct.BoundVar]()

  def apply(a: BoundVar): orct.BoundVar = {
    a -> {
      case (x: BoundVar) => {
        boundVarCache.getOrElseUpdate(x, new orct.BoundVar())
      }
      //case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in OILToOrctimizer")
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
      case IntersectionType(a, b) => orct.IntersectionType(apply(a), apply(b))
      case UnionType(a, b) => orct.UnionType(apply(a), apply(b))
      case NominalType(a) => orct.NominalType(apply(a))
      case StructuralType(members) => orct.StructuralType(members.mapValues(apply).view.force)
    }
  }

  def apply(t: BoundTypevar): orct.BoundTypevar = {
    t -> {
      case u: BoundTypevar => new orct.BoundTypevar()
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
