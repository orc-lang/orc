//
// OrctimizerToPorc.scala -- Scala class OrctimizerToPorc and related
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field
import orc.ast.porc
import orc.error.compiletime.FeatureNotSupportedException
import orc.ast.porc.TryOnHalted
import orc.lib.state.NewFlag
import orc.lib.state.PublishIfNotSet
import orc.lib.state.SetFlag
import orc.ast.porc.SetDiscorporate
import orc.ast.porc.MethodDirectCall
import orc.ast.porc.Continuation
import orc.compile.AnalysisCache
import orc.compile.Logger
import orc.util.{Ternary, TUnknown, TTrue, TFalse}

case class ConversionContext(p: porc.Variable, c: porc.Variable, t: porc.Variable, recursives: Set[BoundVar], callgraph: CallGraph) {
}

/** @author amp
  */
class OrctimizerToPorc {
  def apply(prog: Expression, cache: AnalysisCache): porc.MethodCPS = {
    val z = prog.toZipper()
    val callgraph: CallGraph = cache.get(CallGraph)((z, None))
    
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    implicit val clx = ConversionContext(p = newP, c = newC, t = newT, recursives = Set(), callgraph = callgraph)
    val body = expression(z)
    porc.MethodCPS(newVarName("Prog"), newP, newC, newT, false, Nil, body)
  }

  val vars: mutable.Map[BoundVar, porc.Variable] = new mutable.HashMap()
  val usedNames: mutable.Set[String] = new mutable.HashSet()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): porc.Variable = {
    val name = if (usedNames contains prefix) {
      varCounter += 1
      prefix + "_" + varCounter
    } else prefix
    usedNames += name
    new porc.Variable(Some(name))
  }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))

  def expression(expr: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import porc.PorcInfixNotation._
    val code = expr match {
      case Stop.Z() => porc.PorcUnit()
      case c @ Call.Z(target, args, typeargs) => {
        import CallGraph._

        val potentialTargets = ctx.callgraph.valuesOf(target)
        
        val isExternal: Ternary = {
          if (potentialTargets.forall(_.isInstanceOf[ExternalSiteValue])) {
            TTrue
          } else {            
            if (potentialTargets.forall(_.isInstanceOf[CallableValue])) {
              TFalse
            } else {
              TUnknown
            }
          }
        }
        
        // TODO: Handle internal direct callables.
        val isDirect = {
          potentialTargets.forall({
            case ExternalSiteValue(s) if s.isDirectCallable =>
              true
            case _ => 
              false
          })
        }
        
        val isNotRecursive = {
          potentialTargets.forall({
            case CallableValue(c, _) if ctx.recursives.contains(c.name) =>
              false
            case _ => 
              true
          })
        }
        
        //Logger.fine(s"Processing ${c.value}: ${target.value} => $potentialTargets ($isExternal $isDirect $isNotRecursive)")
        
        // TODO: Spawning on publication is a big issue since it results in O(n^2) spawns during stack
        //       unrolling. Can we avoid the need for the spawn in P.
        // TODO: Consider a hybrid approach which allows a few direct calls and then bounces. Maybe back these semantics into spawn.

        if (!isDirect) {
          if(isNotRecursive) {
            porc.MethodCPSCall(isExternal, argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)).view.force)
          } else {
            // For possibly recusive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = newVarName("P")
            val v = newVarName("temp")
            val comp1 = newVarName("comp")
            val comp2 = newVarName("comp")
            let((newP, porc.Continuation(Seq(v), let((comp1, porc.Continuation(Seq(), ctx.p(v)))) { porc.Spawn(ctx.c, ctx.t, comp1) })),
                (comp2, porc.Continuation(Seq(), porc.MethodCPSCall(isExternal, argument(target), newP, ctx.c, ctx.t, args.map(argument(_)).view.force)))) {
              porc.Spawn(ctx.c, ctx.t, comp2)
            }
          }
        } else {
          val v = newVarName("temp")
          let((v, porc.MethodDirectCall(isExternal, argument(target), args.map(argument(_)).view.force))) {
            ctx.p(v)
          }
        }
      }
      case Parallel.Z(left, right) => {
        // TODO: While it is sound to never add a spawn here it might be good to add them sometimes.
        expression(left) :::
          expression(right)
      }
      case Branch.Z(left, x, right) => {
        val newP = newVarName("P")
        val v = lookup(x)
        let((newP, porc.Continuation(Seq(v), expression(right)))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Trim.Z(f) => {
        val newT = newVarName("T")
        val newP = newVarName("P")
        val v = newVarName()
        let((newT, porc.NewTerminator(ctx.t)),
          (newP, porc.Continuation(Seq(v), porc.Kill(newT) ::: ctx.p(v)))) {
            porc.TryOnKilled(expression(f)(ctx.copy(t = newT, p = newP)), porc.PorcUnit())
          }
      }
      case Future.Z(f) => {
        val fut = newVarName("fut")
        val comp = newVarName("comp")
        val newP = newVarName("P")
        val newC = newVarName("C")
        let((fut, porc.NewFuture()),
            (comp, porc.Continuation(Seq(newP, newC), expression(f)(ctx.copy(p = newP, c = newC))))) {
          porc.SpawnBindFuture(fut, ctx.c, ctx.t, comp) :::
            ctx.p(fut)
        }
      }
      case Force.Z(xs, vs, forceClosures, e) => {
        val porcXs = xs.map(lookup)
        val newP = newVarName("P")
        //val v = newVarName("temp")
        val body = expression(e)
        let((newP, porc.Continuation(porcXs, body))) {
          porc.Force(newP, ctx.c, ctx.t, forceClosures, vs.map(argument))
        }
      }
      case Otherwise.Z(left, right) => {
        val newC = newVarName("C")
        val flag = newVarName("flag")
        val cr = newVarName("cr")

        val cl = {
          val newP = newVarName("P")
          val v = newVarName()
          let((newP, porc.Continuation(Seq(v), setFlag(flag) ::: ctx.p(v)))) {
            expression(left)(ctx.copy(p = newP, c = newC))
          }
        }
        val crImpl = porc.Continuation(Seq(), {
          TryOnHalted({
            publishIfNotSet(flag) :::
              expression(right)
          }, porc.PorcUnit())
        })

        let((flag, newFlag()),
            (cr, crImpl)) {
          let((newC, porc.NewCounter(ctx.c, cr))) {
            porc.TryFinally(cl, porc.Halt(newC))
          }
        }
      }
      case IfDef.Z(a, f, g) => {
        porc.IfDef(argument(a), expression(f), expression(g))
      }
      case DeclareCallables.Z(defs, body) => {
        val b = if (defs.exists(_.isInstanceOf[Site.Z]))
          SetDiscorporate(ctx.c) ::: expression(body)
        else
          expression(body)
        porc.MethodDeclaration(defs.map(callable(defs.map(_.name), _)).view.force, b)
      }

      case New.Z(self, _, bindings, _) => {
        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = newVarName(f.field)
          val (value, binder) = b match {
            case FieldFuture.Z(e) =>
              val newP = newVarName("P")
              val newC = newVarName("C")
              val comp = newVarName("comp")
              val binder = let((comp, porc.Continuation(Seq(newP, newC), expression(e)(ctx.copy(p = newP, c = newC))))) {
                porc.SpawnBindFuture(varName, ctx.c, ctx.t, comp)
              }
              (porc.NewFuture(), Some(binder))
            case FieldArgument.Z(a) =>
              (argument(a), None)
          }
          ((varName, value), (f, varName), binder)
        }
        val (fieldVars, fields, binders) = {
          val (fvs, fs, bs) = fieldInfos.unzip3
          (fvs.toSeq, fs.toMap, bs.flatten)
        }

        let(fieldVars :+ (selfV, porc.New(fields)): _*) {
          binders.foldRight(ctx.p(selfV): porc.Expression)((a, b) => porc.Sequence(Seq(a, b)))
        }
      }

      case FieldAccess.Z(o, f) => {
        val v = newVarName(f.field)
        let((v, porc.GetField(argument(o), f))) {
          ctx.p(v)
        }
      }
      case a: Argument.Z => {
        ctx.p(argument(a))
      }

      // We do not handle types
      case HasType.Z(body, expectedType) => expression(body)
      case DeclareType.Z(u, t, body) => expression(body)
      // case e => throw new NotImplementedError("orctimizerToPorc is not implemented for: " + e)
    }
    code
  }

  def argument(a: Argument.Z): porc.Argument = {
    a.value match {
      case c @ Constant(v) => porc.Constant(v)
      case (x: BoundVar) => lookup(x)
      //case _ => ???
    }
  }

  def callable(recursiveGroup: Seq[BoundVar], d: Callable.Z)(implicit ctx: ConversionContext): porc.Method = {
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val Callable.Z(f, formals, body, _, _, _) = d
    val args = formals.map(lookup)
    val name = lookup(f)
    val bodyT = d match {
      case Def.Z(_, _, body, typeformals, argtypes, returntype) =>
        newT
      case Site.Z(_, _, body, typeformals, argtypes, returntype) =>
        ctx.t
    }
    porc.MethodCPS(name, newP, newC, newT, d.isInstanceOf[Def.Z], args,
      expression(body)(ctx.copy(p = newP, c = newC, t = bodyT, recursives = ctx.recursives ++ recursiveGroup)))
  }

  private def newFlag(): MethodDirectCall = {
    MethodDirectCall(true, porc.Constant(NewFlag), List())
  }
  private def setFlag(flag: porc.Variable): MethodDirectCall = {
    MethodDirectCall(true, porc.Constant(SetFlag), List(flag))
  }
  private def publishIfNotSet(flag: porc.Variable): MethodDirectCall = {
    MethodDirectCall(true, porc.Constant(PublishIfNotSet), List(flag))
  }

}
