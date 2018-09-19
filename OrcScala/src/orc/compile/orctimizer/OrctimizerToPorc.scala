//
// OrctimizerToPorc.scala -- Scala class OrctimizerToPorc and related
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import scala.collection.mutable

import orc.ast.orctimizer.named._
import orc.ast.hasOptionalVariableName._
import orc.ast.porc
import orc.ast.porc.{ MethodCPSCall, MethodDirectCall, SetDiscorporate }
import orc.compile.{ AnalysisCache, CompilerOptions }
import orc.lib.state.{ NewFlag, PublishIfNotSet, SetFlag }
import orc.lib.builtin.structured.{ TupleConstructor }
import orc.util.TUnknown
import orc.lib.builtin.structured.TupleArityChecker
import orc.compile.Logger

case class ConversionContext(
    p: porc.Variable, c: porc.Variable, t: porc.Variable,
    recursives: Set[BoundVar],
    callgraph: CallGraph,
    publications: PublicationCountAnalysis,
    effects: EffectAnalysis,
    containingFunction: String) {
}

/** @author amp
  */
class OrctimizerToPorc(co: CompilerOptions) {
  def apply(prog: Expression, cache: AnalysisCache): porc.MethodCPS = {
    val z = prog.toZipper()
    val callgraph: CallGraph = cache.get(CallGraph)((z, None))
    val publications: PublicationCountAnalysis = cache.get(PublicationCountAnalysis)((z, None))
    val effects: EffectAnalysis = cache.get(EffectAnalysis)((z, None))

    val newP = Variable(id"P")
    val newC = Variable(id"C")
    val newT = Variable(id"T")
    implicit val clx = ConversionContext(p = newP, c = newC, t = newT, recursives = Set(), callgraph = callgraph, publications = publications, effects = effects, containingFunction = "Prog")
    val body = expression(z)
    prog ->> porc.MethodCPS(Variable(id"Prog"), newP, newC, newT, false, Nil, body)
  }

  val useDirectCalls = co.options.optimizationFlags("porc:directcalls").asBool(true)
  val useDirectGetFields = co.options.optimizationFlags("porc:directgetfields").asBool(true)
  val usePorcGraft = co.options.optimizationFlags("porc:usegraft").asBool(false)

  val vars: mutable.Map[BoundVar, porc.Variable] = new mutable.HashMap()
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, Variable(temp.optionalVariableName.getOrElse(id"v")))

  def Variable(s: String) = new porc.Variable(Some(s))

  /** Spawn if we are not in a sequentialized section.
    */
  def probablySpawn(scope: Expression.Z)(mustSpawn: Boolean, comp: porc.Argument)(implicit ctx: ConversionContext): porc.Expression = {
    if (AnnotationHack.inAnnotation[Sequentialize](scope) && !mustSpawn) {
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

  def buildGraft(v: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import orc.ast.porc.PorcInfixNotation._
    val oldCtx = ctx
    val cf = s"${ctx.containingFunction}"

    val vClosure = Variable(id"V_${cf}_$v")
    val newP = Variable(id"P_${cf}_$v")
    val newC = Variable(id"C_${cf}_$v")

    let(
      (vClosure, porc.Continuation(Seq(newP, newC), {
        implicit val ctx = oldCtx.copy(p = newP, c = newC)
        catchExceptions {
          expression(v)
        }
      }))) {
      porc.Graft(ctx.p, ctx.c, ctx.t, vClosure)
    }
  }


  /** Run expression f to bind future fut.
    *
    * This uses the current counter and terminator, but does not publish any value.
    */
  def buildSlowFuture(fut: porc.Variable, f: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import orc.ast.porc.PorcInfixNotation._
    val oldCtx = ctx
    val cf = s"${ctx.containingFunction}"

    val comp = Variable(id"comp_${cf}_$fut~")
    val v = Variable(id"v_${cf}_$fut~")
    val cr = Variable(id"cr_${cf}_$fut~")
    val newP = Variable(id"P_${cf}_$fut~")
    val newC = Variable(id"C_${cf}_$fut~")

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

  def expression(expr: Expression.Z)(implicit ctx: ConversionContext): porc.Expression = {
    import CallGraphValues._
    import FlowGraph._
    import orc.ast.porc.PorcInfixNotation._

    val cf = s"_${ctx.containingFunction}"

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
            case NodeValue(ConstantNode(Constant(s: orc.values.sites.DirectSiteMetadata), _)) =>
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
            catchExceptions(
              porc.MethodCPSCall(isExternal, argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)).toVector)
            )
          } else {
            // For possibly recursive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = Variable(id"P${cf}_$target")
            val v = Variable(id"res${cf}_$target")
            val comp1 = Variable(id"comp${cf}_$target")
            val comp2 = Variable(id"comp${cf}_$target")
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
          val v = Variable(id"res${cf}_$target")
          killCheck :::
          catchExceptions(
            let((v, porc.MethodDirectCall(isExternal, argument(target), args.map(argument(_)).view.force))) {
              ctx.p(v)
            }
          )
        }
      }
      case Parallel.Z(left, right) => {
        val comp = Variable(id"comp${cf}")
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
        val newP = Variable(id"P${cf}_$x")
        val v = lookup(x)
        let((newP, porc.Continuation(Seq(v), catchExceptions(expression(right))))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Trim.Z(f) => {
        val newP = Variable(id"P${cf}")
        val newK = Variable(id"K${cf}")
        val newC = Variable(id"Ct${cf}")
        val newT = Variable(id"T${cf}")
        val v = Variable(id"v${cf}")

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
      case Future.Z(f) if usePorcGraft => {
        buildGraft(f)
      }
      case Future.Z(f) => {
        val fut = Variable(id"fut${cf}_$f")
        val zeroOrOnePubRhs = ctx.publications.publicationsOf(f) <= 1
        let((fut, porc.NewFuture(zeroOrOnePubRhs))) {
          buildSlowFuture(fut, f) :::
            ctx.p(fut)
        }
      }
      case Force.Z(xs, vs, e) => {
        val porcXs = xs.map(lookup).toVector
        val newP = Variable(id"P${cf}")
        val body = expression(e)
        let((newP, porc.Continuation(porcXs, catchExceptions(body)))) {
          porc.Force(newP, ctx.c, ctx.t, vs.map(argument).toVector)
        }
      }
      case Resolve.Z(futures, e) => {
        val newP = Variable(id"P${cf}")
        val v = argument(e)
        let((newP, porc.Continuation(Seq(), ctx.p(v)))) {
          porc.Resolve(newP, ctx.c, ctx.t, futures.map(argument).toVector)
        }
      }
      case Otherwise.Z(left, right) => {
        val newC = Variable(id"C${cf}")
        val flag = Variable(id"flag${cf}")
        val cr = Variable(id"cr${cf}")

        val cl = {
          val newP = Variable(id"P${cf}")
          val v = Variable(id"v${cf}")
          let(
            (newP, porc.Continuation(Seq(v), {
              val newP2 = Variable(id"P2${cf}")
              val v2 = Variable(id"v2${cf}")
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
            val newP2 = Variable(id"P${cf}")
            val v2 = Variable(id"v${cf}")
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
        porc.MethodDeclaration(ctx.t, defs.map(callable(defs.map(_.name), _)).toVector, b)
      }
      case New.Z(self, _, bindings, _) if usePorcGraft => {
        Logger.warning(s"Fast futures are not used in objects yet. ($self)")

        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = Variable(id"${f.name}")
          val (value, binder) = b match {
            case FieldFuture.Z(e) =>
              val zeroOrOnePubRhs = ctx.publications.publicationsOf(e) <= 1
              (porc.NewFuture(zeroOrOnePubRhs), Some(buildSlowFuture(varName, e)))
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
          binders.foldRight(ctx.p(selfV): porc.Expression)((a, b) => porc.Sequence(Vector(a, b)))
        }
      }
      case New.Z(self, _, bindings, _) => {
        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = Variable(id"${f.name}")
          val (value, binder) = b match {
            case FieldFuture.Z(e) =>
              val zeroOrOnePubRhs = ctx.publications.publicationsOf(e) <= 1
              (porc.NewFuture(zeroOrOnePubRhs), Some(buildSlowFuture(varName, e)))
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
          binders.foldRight(ctx.p(selfV): porc.Expression)((a, b) => porc.Sequence(Vector(a, b)))
        }
      }
      case GetField.Z(o, f) => {
        if (useDirectGetFields) {
          val v = o.value ->> Variable(id"${f.name}")
          let((v, porc.GetField(argument(o), f))) {
            ctx.p(v)
          }
        } else {
          MethodCPSCall(true, porc.Constant(orc.lib.builtin.GetFieldSite), ctx.p, ctx.c, ctx.t, List(argument(o), porc.Constant(f)))
        }
      }
      case GetMethod.Z(o) => {
        if (useDirectGetFields) {
          val v = o.value ->> Variable(id"${o}")
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
    val Method.Z(f, formals, body, _, _, _) = d
    val argP = Variable(id"P_$f")
    val argC = Variable(id"C_$f")
    val argT = Variable(id"T_$f")
    val args = formals.map(lookup).toVector
    val name = lookup(f)

    val newBody = {
      catchExceptions({
        d match {
          case _: Routine.Z => {
            implicit val ctx = oldCtx.copy(p = argP, c = argC, t = argT, recursives = oldCtx.recursives ++ recursiveGroup, containingFunction = f.toString)
            expression(body)
          }
          case _: Service.Z => {
            val newC = Variable(id"Cs_$f")
            val newP = Variable(id"Ps_$f")
            val v = Variable(id"v_$f")
            implicit val ctx = oldCtx.copy(p = newP, c = newC, t = oldCtx.t, recursives = oldCtx.recursives ++ recursiveGroup, containingFunction = f.toString)
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
      })(oldCtx.copy(c = argC, containingFunction = f.toString))
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
