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

import scala.collection.mutable

import orc.ast.orctimizer.named._
import orc.ast.porc
import orc.ast.porc.{ MethodCPSCall, MethodDirectCall, SetDiscorporate }
import orc.compile.{ AnalysisCache, CompilerOptions }
import orc.lib.state.{ NewFlag, PublishIfNotSet, SetFlag }
import orc.lib.builtin.structured.{ TupleConstructor }
import orc.util.TUnknown
import orc.lib.builtin.structured.TupleArityChecker

case class ConversionContext(
    p: porc.Variable, c: porc.Variable, t: porc.Variable,
    recursives: Set[BoundVar],
    callgraph: CallGraph,
    publications: PublicationCountAnalysis,
    effects: EffectAnalysis) {
}

/** @author amp
  */
class OrctimizerToPorc(co: CompilerOptions) {
  def apply(prog: Expression, cache: AnalysisCache): porc.MethodCPS = {
    val z = prog.toZipper()
    val callgraph: CallGraph = cache.get(CallGraph)((z, None))
    val publications: PublicationCountAnalysis = cache.get(PublicationCountAnalysis)((z, None))
    val effects: EffectAnalysis = cache.get(EffectAnalysis)((z, None))

    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    implicit val clx = ConversionContext(p = newP, c = newC, t = newT, recursives = Set(), callgraph = callgraph, publications = publications, effects = effects)
    val body = expression(z)
    prog ->> porc.MethodCPS(newVarName("Prog"), newP, newC, newT, false, Nil, body)
  }

  val useDirectCalls = co.options.optimizationFlags("porc:directcalls").asBool(true)
  val useDirectGetFields = co.options.optimizationFlags("porc:directgetfields").asBool(true)

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

  /** Spawn if we are not in a sequentialized section.
    */
  def probablySpawn(scope: Expression.Z)(mustSpawn: Boolean, comp: porc.Argument)(implicit ctx: ConversionContext): porc.Expression = {
    if (AnnotationHack.inAnnotation[Sequentialize](scope)) {
      comp()
    } else {
      porc.Spawn(ctx.c, ctx.t, mustSpawn, comp)
    }
  }

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
    import orc.ast.porc.PorcInfixNotation._
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
          (newC, porc.NewSimpleCounter(ctx.c, cr)),
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
        	// TODO: The `false` could actually cause semantic problems in the case of sites which block the calling thread. Metadata is probably needed.
          catchExceptions {
            probablySpawn(f)(false, comp)
          }
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
    import CallGraphValues._
    import FlowGraph._
    import orc.ast.porc.PorcInfixNotation._

    val oldCtx = ctx

    val code = expr match {
      case Stop.Z() =>
        porc.HaltToken(ctx.c)
      case c @ Call.Z(target, args, typeargs) => {
        val potentialTargets = ctx.callgraph.valuesOf(target)

        val isExternal = potentialTargets.view.collect({
          case n: NodeValue[_] => n.isExternalMethod
        }).fold(TUnknown)(_ union _)

        // TODO: Handle internal direct callables.
        val isDirect = {
          potentialTargets.forall({
            case NodeValue(ConstantNode(Constant(s: orc.values.sites.SiteMetadata), _)) if s.isDirectCallable =>
              true
            case NodeValue(ExitNode(Call.Z(Constant.Z(TupleConstructor), _, _))) =>
              true
            case NodeValue(ExitNode(Call.Z(Constant.Z(TupleArityChecker), _, _))) =>
              true
            case _ =>
              false
          })
        }

        val isNotRecursive = {
          potentialTargets.forall({
            case NodeValue(MethodNode(c, _)) if ctx.recursives.contains(c.name) =>
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

        if (!useDirectCalls || !isDirect) {
          if (isNotRecursive) {
            killCheck :::
            porc.MethodCPSCall(isExternal, argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)).view.force)
          } else {
            // For possibly recursive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = newVarName("P")
            val v = newVarName("temp")
            val comp1 = newVarName("comp")
            val comp2 = newVarName("comp")
            killCheck :::
            let(
              (newP, porc.Continuation(Seq(v), {
                let((comp1, porc.Continuation(Seq(), ctx.p(v)))) {
                  catchExceptions { probablySpawn(expr)(true, comp1) }
                }
              })),
              (comp2, porc.Continuation(Seq(), {
                catchExceptions { porc.MethodCPSCall(isExternal, argument(target), newP, ctx.c, ctx.t, args.map(argument(_)).view.force) }
              }))) {
                catchExceptions { probablySpawn(expr)(true, comp2) }
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
        val comp = newVarName("comp")
        let(
          (comp, porc.Continuation(Seq(), {
            catchExceptions {
              expression(left)
            }
          }))) {
            porc.NewToken(ctx.c) :::
            	// TODO: The `false` could actually cause semantic problems in the case of sites which block the calling thread. Metadata is probably needed.
              catchExceptions { probablySpawn(expr)(false, comp) } :::
              expression(right)
          }
      }
      case Branch.Z(left, x, right) => {
        val newP = newVarName("P")
        val v = lookup(x)
        let((newP, porc.Continuation(Seq(v), catchExceptions(expression(right))))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Trim.Z(f) => {
        val newP = newVarName("P")
        val newK = newVarName("K")
        val newC = newVarName("Ct")
        val newT = newVarName("T")
        val v = newVarName()

        let((newT, porc.NewTerminator(ctx.t)),
          (newC, porc.NewTerminatorCounter(ctx.c, newT)),
          (newP, porc.Continuation(Seq(v),
            let((newK, porc.Continuation(Seq(), {
                ctx.p(v) :::
                  porc.HaltToken(newC)
              }))) {
              porc.Kill(newC, newT, newK)
            }))) {
            implicit val ctx = oldCtx.copy(t = newT, c = newC, p = newP)
            // TODO: Is this correct? Can a Trim halt and need to inform the enclosing scope?
            catchExceptions {
              expression(f)
            }
          }
      }
      case Future.Z(f) => {
        val fut = newVarName("fut")
        val zeroOrOnePubRhs = ctx.publications.publicationsOf(f) <= 1
        let((fut, porc.NewFuture(zeroOrOnePubRhs))) {
          buildFuture(fut, f) :::
            ctx.p(fut)
        }
      }
      case Force.Z(xs, vs, e) => {
        val porcXs = xs.map(lookup)
        val newP = newVarName("P")
        val body = expression(e)
        let((newP, porc.Continuation(porcXs, catchExceptions(body)))) {
          porc.Force(newP, ctx.c, ctx.t, vs.map(argument).view.force)
        }
      }
      case Resolve.Z(futures, e) => {
        val newP = newVarName("P")
        val v = argument(e)
        // TODO: PERFORMANCE: Add an instruction here that allows v to rewrite itself when based on the fact futures are resolved.
        let((newP, porc.Continuation(Seq(), ctx.p(v)))) {
          porc.Resolve(newP, ctx.c, ctx.t, futures.map(argument).view.force)
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
              val newP2 = newVarName("P")
              val v2 = newVarName()
              let(
                (newP2, porc.Continuation(Seq(v2), {
                  porc.NewToken(ctx.c) :::
                    porc.HaltToken(newC) :::
                    ctx.p(v)
                }))) {
                  setFlag(newP2, newC, ctx.t, flag)
                }
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
            val newP2 = newVarName("P")
            val v2 = newVarName()
            let(
              (newP2, porc.Continuation(Seq(v2), {
                catchExceptions {
                  expression(right)
                }
              }))) {
                publishIfNotSet(newP2, ctx.c, ctx.t, flag)
              }
          }
        })

        let(
          (flag, newFlag()),
          (cr, crImpl)) {
            let((newC, porc.NewSimpleCounter(ctx.c, cr))) {
              cl
            }
          }
      }
      case IfLenientMethod.Z(a, f, g) => {
        porc.IfLenientMethod(argument(a), expression(f), expression(g))
      }
      case DeclareMethods.Z(defs, body) => {
        val b = if (defs.exists(_.isInstanceOf[Service.Z]))
          SetDiscorporate(ctx.c) ::: expression(body)
        else
          expression(body)
        porc.MethodDeclaration(ctx.t, defs.map(callable(defs.map(_.name), _)).view.force, b)
      }
      case New.Z(self, _, bindings, _) => {
        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = newVarName(f.name)
          val (value, binder) = b match {
            case FieldFuture.Z(e) =>
              val zeroOrOnePubRhs = ctx.publications.publicationsOf(e) <= 1
              (porc.NewFuture(zeroOrOnePubRhs), Some(buildFuture(varName, e)))
            case FieldArgument.Z(a) =>
              (argument(a), None)
          }
          ((varName, value), (f, varName), binder)
        }
        val (fieldVars, fields, binders) = {
          val (fvs, fs, bs) = fieldInfos.unzip3
          (fvs.toSeq, fs.toMap, bs.flatten)
        }

        let(fieldVars :+ ((selfV, porc.New(fields))): _*) {
          binders.foldRight(ctx.p(selfV): porc.Expression)((a, b) => porc.Sequence(Seq(a, b)))
        }
      }
      case GetField.Z(o, f) => {
        if (useDirectGetFields) {
          val v = o.value ->> newVarName(f.name)
          let((v, porc.GetField(argument(o), f))) {
            ctx.p(v)
          }
        } else {
          MethodCPSCall(true, porc.Constant(orc.lib.builtin.GetFieldSite), ctx.p, ctx.c, ctx.t, List(argument(o), porc.Constant(f)))
        }
      }
      case GetMethod.Z(o) => {
        if (useDirectGetFields) {
          val v = o.value ->> newVarName()
          let((v, porc.GetMethod(argument(o)))) {
            ctx.p(v)
          }
        } else {
          MethodCPSCall(true, porc.Constant(orc.lib.builtin.GetMethodSite), ctx.p, ctx.c, ctx.t, List(argument(o)))
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
    import orc.ast.porc.PorcInfixNotation._

    val oldCtx = ctx
    val argP = newVarName("P")
    val argC = newVarName("C")
    val argT = newVarName("T")
    val Method.Z(f, formals, body, _, _, _) = d
    val args = formals.map(lookup)
    val name = lookup(f)

    val newBody = {
      catchExceptions({
        d match {
          case _: Routine.Z => {
            implicit val ctx = oldCtx.copy(p = argP, c = argC, t = argT, recursives = oldCtx.recursives ++ recursiveGroup)
            expression(body)
          }
          case _: Service.Z => {
            val newC = newVarName("Cs")
            val newP = newVarName("P")
            val v = newVarName("v")
            implicit val ctx = oldCtx.copy(p = newP, c = newC, t = oldCtx.t, recursives = oldCtx.recursives ++ recursiveGroup)
            porc.CheckKilled(ctx.t) :::
              // FIXME: Audit.
              let((newC, porc.NewServiceCounter(argC, oldCtx.c, ctx.t)),
                (newP, porc.Continuation(Seq(v), {
                    porc.NewToken(argC) :::
                    porc.HaltToken(ctx.c) :::
                    argP(v)
                }))) {
                  expression(body)
                }
          }
        }
      })(oldCtx.copy(c = argC))
    }

    d.value ->> porc.MethodCPS(name, argP, argC, argT, d.isInstanceOf[Routine.Z], args, newBody)
  }

  private def newFlag(): porc.Expression = {
    MethodDirectCall(true, porc.Constant(NewFlag), List())
  }
  private def setFlag(p: porc.Variable, c: porc.Variable, t: porc.Variable, flag: porc.Variable): porc.Expression = {
    if (useDirectCalls) {
      MethodDirectCall(true, porc.Constant(SetFlag), List(flag)) :::
      p(porc.Constant(orc.values.Signal))
    } else {
      MethodCPSCall(true, porc.Constant(SetFlag), p, c, t, List(flag))
    }
  }
  private def publishIfNotSet(p: porc.Variable, c: porc.Variable, t: porc.Variable, flag: porc.Variable): porc.Expression = {
    if (useDirectCalls) {
      MethodDirectCall(true, porc.Constant(PublishIfNotSet), List(flag)) :::
      p(porc.Constant(orc.values.Signal))
    } else {
      MethodCPSCall(true, porc.Constant(PublishIfNotSet), p, c, t, List(flag))
    }
  }
}
