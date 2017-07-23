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
import orc.lib.state.NewFlag
import orc.lib.state.PublishIfNotSet
import orc.lib.state.SetFlag
import orc.ast.porc.SetDiscorporate
import orc.ast.porc.MethodDirectCall
import orc.ast.porc.Continuation
import orc.compile.AnalysisCache
import orc.compile.Logger
import orc.util.{ Ternary, TUnknown, TTrue, TFalse }
import orc.ast.porc.PorcUnit

case class ConversionContext(p: porc.Variable, c: porc.Variable, t: porc.Variable, recursives: Set[BoundVar], callgraph: CallGraph, effects: EffectAnalysis) {
}

/** @author amp
  */
class OrctimizerToPorc {
  def apply(prog: Expression, cache: AnalysisCache): porc.MethodCPS = {
    val z = prog.toZipper()
    val callgraph: CallGraph = cache.get(CallGraph)((z, None))
    val effects: EffectAnalysis = cache.get(EffectAnalysis)((z, None))

    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    implicit val clx = ConversionContext(p = newP, c = newC, t = newT, recursives = Set(), callgraph = callgraph, effects = effects)
    val body = expression(z)
    prog ->> porc.MethodCPS(newVarName("Prog"), newP, newC, newT, false, Nil, body)
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
 
  /** Catch porc exceptions and halt the current C.
    */
  def catchExceptions(e: porc.Expression)(implicit ctx: ConversionContext): porc.Expression = {
    porc.TryOnException({
      e
    }, {
      porc.HaltToken(ctx.c)
    })
  }

  /** Run expression f to bind future fut.
    *
    * This uses the current counter and terminator, but does not publish any value.
    */
  def buildFuture(fut: porc.Variable, f: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import porc.PorcInfixNotation._
    val oldCtx = ctx

    val comp = newVarName("comp")
    val v = newVarName("v")
    val cr = newVarName("cr")
    val newP = newVarName("P")
    val newC = newVarName("C")

    let(
      (comp, porc.Continuation(Seq(), {
        val crImpl = porc.Continuation(Seq(), {
          porc.BindStop(fut) :::
            porc.HaltToken(ctx.c)
        })
        let(
          (cr, crImpl),
          (newC, porc.NewCounter(ctx.c, cr)),
          (newP, porc.Continuation(Seq(v), {
            porc.Bind(fut, v) :::
              porc.HaltToken(newC)
          }))) {
            implicit val ctx = oldCtx.copy(p = newP, c = newC)
            catchExceptions {
              expression(f)
            }
          }
      }))) {
        porc.NewToken(ctx.c) :::
          catchExceptions { porc.Spawn(ctx.c, ctx.t, true, comp) }
      }
  }

  // FIXME: If an executing expression is killed, it will not halt it's counter. 
  //        I think in all cases Killed and Halted will always be handled the same way. The exception might be otherwise which
  //        could easily remedy the problem by placing a kill check in the LHS branch.

  // FIXME: Make it an error to have an exception escape a Porc function (even p and stuff).
  //        This would enable checks at the top-level (the scheduler and such) to catch mistakes.
  //        In addition, there could be a check at the root of closures.
  //        The exception would be that direct callable methods would be allowed to throw. However these are not used yet.

  def expression(expr: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import porc.PorcInfixNotation._
    val oldCtx = ctx

    val code = expr match {
      case Stop.Z() =>
        porc.HaltToken(ctx.c)
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

        // Use a kill check for effectful calls or recursive ones.
        val killCheck = if (ctx.effects.effects(c) || !isNotRecursive) {
          porc.CheckKilled(ctx.t)
        } else {
          porc.PorcUnit()
        }

        // TODO: Spawning on publication is a big issue since it results in O(n^2) spawns during stack
        //       unrolling. Can we avoid the need for the spawn in P.
        // TODO: Consider a hybrid approach which allows a few direct calls and then bounces. Maybe back these semantics into spawn.

        if (!isDirect) {
          if (isNotRecursive) { 
            killCheck :::
            porc.MethodCPSCall(isExternal, argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)).view.force)
          } else {
            // For possibly recusive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = newVarName("P")
            val v = newVarName("temp")
            val comp1 = newVarName("comp")
            val comp2 = newVarName("comp")
            killCheck :::
            let(
              (newP, porc.Continuation(Seq(v), {
                let((comp1, porc.Continuation(Seq(), ctx.p(v)))) {
                  catchExceptions { porc.Spawn(ctx.c, ctx.t, true, comp1) }
                }
              })),
              (comp2, porc.Continuation(Seq(), {
                catchExceptions { porc.MethodCPSCall(isExternal, argument(target), newP, ctx.c, ctx.t, args.map(argument(_)).view.force) }
              }))) {
                catchExceptions { porc.Spawn(ctx.c, ctx.t, true, comp2) }
              }
          }
        } else {
          val v = newVarName("temp")
          killCheck :::
          let((v, porc.MethodDirectCall(isExternal, argument(target), args.map(argument(_)).view.force))) {
            ctx.p(v)
          }
        }
      }
      case Parallel.Z(left, right) => {
        // TODO: While it is sound to never add a spawn here it might be good to add them sometimes.
        porc.NewToken(ctx.c) :::
          catchExceptions(expression(left)) :::
          expression(right)
      }
      case Branch.Z(left, x, right) => {
        val newP = newVarName("P")
        val v = lookup(x)
        let((newP, porc.Continuation(Seq(v), catchExceptions(expression(right))))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Trim.Z(f) => {
        val newT = newVarName("T")
        val newP = newVarName("P")
        val v = newVarName()
        // FIXME: This probably needs to have a counter for every terminator to kill the terminator when it's empty. Without this terminators can collect in each others lists.
        let((newT, porc.NewTerminator(ctx.t)),
          (newP, porc.Continuation(Seq(v), catchExceptions(porc.Kill(newT) ::: ctx.p(v))))) {
            // TODO: Is this correct? Can a Trim halt and need to inform the enclosing scope?
            catchExceptions {
              expression(f)(ctx.copy(t = newT, p = newP))
            }
          }
      }
      case Future.Z(f) => {
        val fut = newVarName("fut")
        let((fut, porc.NewFuture())) {
          buildFuture(fut, f) :::
            ctx.p(fut)
        }
      }
      case Force.Z(xs, vs, e) => {
        val porcXs = xs.map(lookup)
        val newP = newVarName("P")
        val body = expression(e)
        let((newP, porc.Continuation(porcXs, catchExceptions(body)))) {
          porc.Force(newP, ctx.c, ctx.t, vs.map(argument))
        }
      }
      case Resolve.Z(futures, e) => {
        val newP = newVarName("P")
        val v = argument(e)
        // TODO: PERFORMANCE: Add an instruction here that allows v to rewrite itself when based on the fact futures are resolved.
        let((newP, porc.Continuation(Seq(), ctx.p(v)))) {
          porc.Resolve(newP, ctx.c, ctx.t, futures.map(argument))
        }
      }
      case Otherwise.Z(left, right) => {
        val newC = newVarName("C")
        val flag = newVarName("flag")
        val cr = newVarName("cr")

        val cl = {
          val newP = newVarName("P")
          val v = newVarName()
          let(
            (newP, porc.Continuation(Seq(v), {
              setFlag(flag) :::
                porc.NewToken(ctx.c) :::
                porc.HaltToken(newC) :::
                ctx.p(v)
            }))) {
              // Move into ctx with new C and P
              implicit val ctx = oldCtx.copy(p = newP, c = newC)
              catchExceptions {
                expression(left)
              }
            }
        }
        val crImpl = porc.Continuation(Seq(), {
          // Here we branch on flag using the Halted exception.
          // We halt the token if we never passed it to right.
          catchExceptions {
            publishIfNotSet(flag) :::
              expression(right)
          }
        })

        let(
          (flag, newFlag()),
          (cr, crImpl)) {
            let((newC, porc.NewCounter(ctx.c, cr))) {
              cl
            }
          }
      }
      case IfLenientMethod.Z(a, f, g) => {
        porc.IfDef(argument(a), expression(f), expression(g))
      }
      case DeclareMethods.Z(defs, body) => {
        val b = if (defs.exists(_.isInstanceOf[Service.Z]))
          SetDiscorporate(ctx.c) ::: expression(body)
        else
          expression(body)
        porc.MethodDeclaration(defs.map(callable(defs.map(_.name), _)).view.force, b)
      }

      case New.Z(self, _, bindings, _) => {
        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = newVarName(f.name)
          val (value, binder) = b match {
            case FieldFuture.Z(e) =>
              (porc.NewFuture(), Some(buildFuture(varName, e)))
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

      case GetField.Z(o, f) => {
        val v = newVarName(f.name)
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
    }
    expr.value ->> code
  }

  def argument(a: Argument.Z): porc.Argument = {
    a.value -> {
      case c @ Constant(v) => porc.Constant(v)
      case (x: BoundVar) => lookup(x)
      //case _ => ???
    }
  }

  def callable(recursiveGroup: Seq[BoundVar], d: Method.Z)(implicit ctx: ConversionContext): porc.Method = {
    val oldCtx = ctx
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val Method.Z(f, formals, body, _, _, _) = d
    val args = formals.map(lookup)
    val name = lookup(f)
    val (bodyT, bodyPrefix) = d match {
      case _: Routine.Z =>
        (newT, porc.PorcUnit())
      case _: Service.Z =>
        (ctx.t, porc.CheckKilled(ctx.t))
    }
    val newBody = {
      implicit val ctx = oldCtx.copy(p = newP, c = newC, t = bodyT, recursives = oldCtx.recursives ++ recursiveGroup)
      catchExceptions {
        bodyPrefix ::: expression(body)
      }
    }
    d.value ->> porc.MethodCPS(name, newP, newC, newT, d.isInstanceOf[Service.Z], args, newBody)
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
