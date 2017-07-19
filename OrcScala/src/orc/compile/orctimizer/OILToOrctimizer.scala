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
import orc.compile.Logger

/** @author amp
  */
// Conversions from named to nameless representations
class OILToOrctimizer {
  case class Context(valueSource: Map[BoundVar, Expression], variableMapping: Map[BoundVar, orct.BoundVar]) {
    def addValueSource(x: BoundVar, e: Expression) = {
      assert(!variableMapping.contains(x), s"Variable source for $x is already set")
      copy(valueSource = valueSource + ((x, e)))
    }
    def addVariableMapping(x: BoundVar, y: orct.BoundVar): Context = {
      assert(!variableMapping.contains(x), s"Variable mapping for $x is already set to ${variableMapping(x)}, not $y")
      copy(variableMapping = variableMapping + ((x, y)))
    }
    def addVariableMapping(x: BoundVar): Context = {
      val y = x ->> new orct.BoundVar()
      addVariableMapping(x, y)
    }
  }
  
  private def isDef(a: Argument)(implicit ctx: Context) = a match {
    case b: BoundVar =>
      ctx.valueSource.get(b) match {
        case Some(d: DeclareCallables) if d.defs.head.isInstanceOf[Def] => true
        case _ => false
      }
    case _ => false
  }
  private def isSite(a: Argument)(implicit ctx: Context) = a match {
    case b: BoundVar =>
      ctx.valueSource.get(b) match {
        case Some(d: DeclareCallables) if d.defs.head.isInstanceOf[Site] => true
        case _ => false
      }
    case _ => false
  }

  private def mayNeedPublishForce(a: Argument)(implicit ctx: Context): Boolean = a match {
    case b: BoundVar =>
      ctx.valueSource.get(b) match {
        case Some(d: DeclareCallables) => false
        case Some(_: New) => false
        //case Some(Sequence(l: Argument, _, _)) => mayNeedPublishForce(l)
        case _ => true
      }
    case _: Constant => false
    case _ => true
  }

  private def maybePublishForce(xs: List[orct.BoundVar], vs: List[Argument], expr: orct.Expression)(implicit ctx: Context): orct.Expression = {
    val m = (xs zip vs).toMap
    val (needForce, noForce) = m.partition(p => mayNeedPublishForce(p._2))
    val (newXs, newVs) = needForce.toList.unzip
    val force = if (needForce.isEmpty) {
      expr
    } else {
      orct.Force(newXs, newVs.map(a => apply(a)), expr)
    }
    //Logger.fine(s"Not forcing ${noForce}, but still forcing ${needForce}")
    noForce.foldLeft(force: orct.Expression) { (core, p) =>
      val (x, v) = p
      orct.Branch(apply(v), x, core)
    }
  }

  private def maybePublishForce(x: orct.BoundVar, v: Argument, expr: orct.Expression)(implicit ctx: Context): orct.Expression = {
    maybePublishForce(List(x), List(v), expr)
  }

  def apply(e: Expression)(implicit ctx: Context): orct.Expression = {
    e -> {
      case Stop() => orct.Stop()
      case Sequence(a: Argument, x, e) => {
        // Special case to optimize the pattern where we are directly forcing something.
        val bctx = ctx.addValueSource(x, e).addVariableMapping(x)
        maybePublishForce(apply(x)(bctx), a, apply(e)(bctx))
      }
      case a: Constant => {
        apply(a)
      }
      case a: Argument => {
        val x = new orct.BoundVar()
        maybePublishForce(x, a, x)
      }
      case Call(target, args, typeargs) => {
        val t = new orct.BoundVar(Some(s"f_$target"))
        def siteCall = {
          val uniqueArgs = args.toSet.toList
          val argVarsMap = uniqueArgs.map(a => (a, new orct.BoundVar(Some(s"f_$a")))).toMap
          val call = orct.CallSite(t, args map argVarsMap, typeargs map { _ map apply })
          if (uniqueArgs.size > 0) {
            maybePublishForce(uniqueArgs map argVarsMap, uniqueArgs, call)
          } else {
            call
          }
        }
        def defCall = orct.CallDef(t, args map apply, typeargs map { _ map apply })
        val call = if (isDef(target)) {
          defCall
        } else if (isSite(target)) {
          siteCall
        } else {
          orct.IfDef(t, defCall, siteCall)
        }
        orct.Force(t, apply(target), call)
      }
      case Parallel(left, right) => orct.Parallel(apply(left), apply(right))
      case Sequence(left, x, right) => {
        val bctx = ctx.addValueSource(x, e).addVariableMapping(x)
        orct.Branch(apply(left), apply(x)(bctx), apply(right)(bctx))
      }
      case Graft(x, left, right) => {
        val bctx = ctx.addValueSource(x, e).addVariableMapping(x)
        orct.Branch(orct.Future(apply(left)), apply(x)(bctx), apply(right)(bctx))
      }
      case Trim(f) => orct.Trim(apply(f))
      case Otherwise(left, right) =>
        orct.Otherwise(apply(left), apply(right))
      case DeclareCallables(defs, body) => {
        val bctx = ctx.copy(
            variableMapping = ctx.variableMapping ++ (defs map { d => (d.name, d.name ->> new orct.BoundVar()) }))
        val dctx = ctx.copy(valueSource = ctx.valueSource ++ (defs map { d => (d.name, e) }), 
            variableMapping = ctx.variableMapping ++ (defs map { d => (d.name, new orct.BoundVar(d.name.optionalVariableName.map(_ + "_temp"))) }))
        val newBody: orct.Expression = apply(body)(bctx)
        val defRes = defs map { apply(_)(dctx) }
        val (newDefs, captureds) = defRes.unzip
        val captured = captureds.flatten.toSeq
        
        val resolves = 
          defs.foldLeft(newBody)((core, d) => {
            val (realName, tempName) = (apply(d.name)(bctx), apply(d.name)(dctx))
            if (d.isInstanceOf[Site] || captured.isEmpty) {
              tempName > realName > core
            } else {
              orct.Future(orct.Resolve(captured, tempName)) > realName > core
            }
          })
        
        // Do not include the callables in their own scope. They don't force normally in that scope.
        orct.DeclareCallables(newDefs, resolves)
      }
      case DeclareType(x, t, body) => {
        orct.DeclareType(apply(x), apply(t), apply(body))
      }
      case HasType(body, expectedType) => orct.HasType(apply(body), apply(expectedType))
      case FieldAccess(o, f) => {
        val t = new orct.BoundVar(Some(s"f_$o"))
        val fv1 = new orct.BoundVar(Some(s"f_${o}_${f.name}'"))
        val fv2 = new orct.BoundVar(Some(s"f_${o}_${f.name}"))
        maybePublishForce(t, o, {
          orct.FieldAccess(t, f) > fv1 >
            orct.Force(fv2, fv1, fv2)
        })
      }
      case New(self, selfT, members, objT) => {
        val bctx = ctx.addValueSource(self, e).addVariableMapping(self)
        val newMembers = members.mapValues(e => e match {
          case a: Argument =>
            orct.FieldArgument(apply(a)(bctx))
          case _ =>
            orct.FieldFuture(apply(e)(bctx))
        }).view.force
        orct.New(apply(self)(bctx), selfT map apply, newMembers, objT map apply)
      }

      case VtimeZone(timeOrder, body) =>
        // TODO: Implement onIdle in the Orctimizer
        throw new FeatureNotSupportedException("Virtual time").setPosition(e.sourceTextRange.getOrElse(null))
      case Hole(_, _) =>
        throw new FeatureNotSupportedException("Hole").setPosition(e.sourceTextRange.getOrElse(null))
    }
  }

  def apply(a: Argument)(implicit ctx: Context): orct.Argument = {
    a -> {
      case Constant(v) => orct.Constant(v)
      case (x: BoundVar) => apply(x)
      case UnboundVar(s) => orct.UnboundVar(s)
    }
  }

  def apply(x: BoundVar)(implicit ctx: Context): orct.BoundVar = {
    x ->> {
      ctx.variableMapping(x)
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

  def apply(defn: Callable)(implicit ctx: Context): (orct.Callable, Set[orct.BoundVar]) = {
    defn match {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newName = apply(name)
        val bctx = formals.foldLeft(ctx)(_.addVariableMapping(_))
        val newFormals = formals.map(apply(_)(bctx))
        val newBody = apply(body)(bctx)
        val d = defn ->> orct.Def(newName, newFormals, newBody, typeformals map apply,
          argtypes map { _ map apply }, returntype map apply)
        (d, newBody.freeVars -- newFormals)
      }
      case Site(name, formals, body, typeformals, argtypes, returntype) => {
        val bctx = formals.foldLeft(ctx)(_.addVariableMapping(_))
        val newFormals = formals.map(apply(_)(bctx))
        val newBody = apply(body)(bctx)
        val d = defn ->> orct.Site(apply(name), newFormals, newBody, typeformals map apply,
          argtypes map { _ map apply }, returntype map apply)
        (d, Set())
      }
    }
  }

}
