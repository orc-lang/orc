//
// Optimizer.scala -- Scala Optimizer
// Project OrcScala
//
// Created by amp on Sept 16, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.compile.Logger
import orc.values.OrcRecord
import orc.ast.orctimizer.named._
import orc.values.Field
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.TupleArityChecker
import orc.compile.CompilerOptions
import orc.types
import orc.values.sites.Site
import orc.lib.state.NewFlag
import orc.lib.state.SetFlag
import orc.lib.state.PublishIfNotSet
import orc.values.Signal
import orc.ast.hasAutomaticVariableName
import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable
import orc.compile.OptimizerStatistics
import orc.compile.NamedOptimization
import orc.compile.AnalysisCache
import orc.compile.orctimizer.FlowGraph._
import orc.compile.orctimizer.CallGraphValues._
import orc.compile.orctimizer.DelayAnalysis.DelayInfo
import swivel.Zipper
import orc.compile.orctimizer.FlowGraph.EverywhereNode

class HashFirstEquality[T](val value: T) {
  override def toString() = value.toString()
  override def hashCode() = value.hashCode()
  override def equals(o: Any) = o match {
    case o: HashFirstEquality[T] =>
      value.hashCode() == o.value.hashCode() && value == o.value
    case _ =>
      false
  }
}

object HashFirstEquality {
  def apply[T](v: T) = {
    new HashFirstEquality(v)
  }
}

class AnalysisResults(cache: AnalysisCache, e: Expression.Z) {
  lazy val callgraph: CallGraph = cache.get(CallGraph)((e, None))
  lazy val publications: PublicationCountAnalysis = cache.get(PublicationCountAnalysis)((e, None))
  lazy val effectsAnalysis: EffectAnalysis = cache.get(EffectAnalysis)((e, None))
  lazy val delays: DelayAnalysis = cache.get(DelayAnalysis)((e, None))
  lazy val forces: ForceAnalysis = cache.get(ForceAnalysis)((e, None))

  private val exprMapping = mutable.HashMap[HashFirstEquality[Expression.Z], Expression.Z]()
  private val varMapping = mutable.HashMap[ValueNode, ValueNode]()

  // TODO: The mapping stuff seems to be a really large performance cost. Maybe if I make it a reference equality it will be better? But that would also be fragile.

  def addMapping(req: Argument.Z, real: Argument.Z): Unit = {
    varMapping += ((ValueNode(req), remap(ValueNode(real))))
  }
  def addMapping(req: Expression.Z, real: Expression.Z): Unit = {
    exprMapping += HashFirstEquality(req) -> remap(real)
  }

  private def remap(req: ValueNode) = varMapping.get(req).getOrElse(req)
  private def remap(req: Expression.Z) = exprMapping.get(HashFirstEquality(req)).getOrElse(req)

  def valuesOf(e: Argument.Z) = callgraph.valuesOf(remap(ValueNode(e)))
  def valuesOf(e: Expression.Z) = callgraph.valuesOf(remap(e))

  def publicationsOf(e: Expression.Z) = publications.publicationsOf(remap(e))

  def effects(e: Expression.Z): Boolean = {
    effectsAnalysis.effects(remap(e))
  }
  def effected(e: Expression.Z): Boolean = {
    effectsAnalysis.effected(remap(e))
  }

  def delayOf(e: Expression.Z): DelayInfo = {
    delays.delayOf(remap(e))
  }

  def nMappings = exprMapping.size + varMapping.size
}

trait Optimization extends ((Expression.Z, AnalysisResults) => Option[Expression]) with NamedOptimization {
  val name: String

  override def toString = name
}

case class Opt(name: String)(f: PartialFunction[(Expression.Z, AnalysisResults), Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f.lift((e, cache))
}
case class OptSimple(name: String)(f: PartialFunction[Expression.Z, Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f.lift(e)
}
case class OptFull(name: String)(f: (Expression.Z, AnalysisResults) => Option[Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f(e, cache)
}

// TODO: Implement compile time evaluation of select sites.

/* Assumptions in the optimizer:
 *
 * No call (def or site) can publish a future.
 *
 */

/** @author amp
  */
abstract class Optimizer(co: CompilerOptions) extends OptimizerStatistics {
  def opts: Seq[Optimization]

  private def traverse(e: Expression.Z)(f: (Expression.Z) => Expression) = {
    e.subtrees
  }

  def apply(e: Expression.Z, cache: AnalysisCache): Expression = {
    val results = new AnalysisResults(cache, e)

    val trans = new Transform {
      override val onExpression = {
        case (e: Expression.Z) => {
          import orc.util.StringExtension._
          //Logger.finer(s"Optimizing:${" " * e.parents.size}${e.value.getClass.getSimpleName} ${e.value.toString.truncateTo(40)}")
          val e1 = opts.foldLeft(e)((e, opt) => {
            //Logger.finer(s"invoking ${opt.name} on:\n${e.value.toString.truncateTo(120)}")
            opt(e, results) match {
              case None => e
              case Some(e2) =>
                val e3 = if (e.value != e2) {
                  Logger.finer(s"${opt.name}:\n${e.value.toString.truncateTo(120)}\n====>\n${e2.toString.truncateTo(120)}")
                  countOptimization(opt)
                  val e3 = e.replace(e2)
                  results.addMapping(e3, e)
                  e3
                } else {
                  e
                }

                e3
            }
          })
          e1.value
        }
      }
    }

    val r = trans(e)

    r
  }

  import Optimizer._

  /*
  val flattenThreshold = co.options.optimizationFlags("orct:future-flatten-threshold").asInt(5)

  val FutureElimFlatten = Opt("future-elim-flatten") {
    // TODO: This may not be legal. What about small expressions that could still block on something.
    /*case (FutureAt(g) > x > f, a) if a(f).forces(x) <= ForceType.Eventually && (a(g).publications only 1)
            && Analysis.cost(g) <= flattenThreshold =>
              g > x > f
              */
    case (e, a) if false => e
  }
  */

  /*
  Analysis needed:

  Information about forcing of expressions. What it forces, and if it halts with it. What is forces may also need to be categorized into before and after first side-effect/publication.

  */

  val FutureElim = OptFull("future-elim") { (e, a) =>
    e match {
      /*case IndependentFutures(futs, core) => {
        // futs is a seq of (f, x)
        // f_1 >x_1> ... >x_n-1> f_n >x_n> core
        var interestingElimables = 0
        def isElimable(p: (Expression, BoundVar)) = {
          val (fexp, x) = p
          val f = fexp in e.ctx
          val byImmediate1 = core.forces(x) == ForceType.Immediately(true) && (f.publications <= 1)
          val byImmediate2 = core.forces(x) == ForceType.Immediately(false) && (f.publications only 1)
          val byNonBlocking1 = f.nonBlockingPublish && (f.publications only 1)
          val byNonBlocking2 = f.nonBlockingPublish && (f.publications <= 1) && core.forces(x).haltsWith
          val result = byImmediate1 || byImmediate2 || byNonBlocking1 || byNonBlocking2
          if (byImmediate1 || byImmediate2) {
            interestingElimables += 1
            Logger.fine(s"Elimable by: $byImmediate1 || $byImmediate2 || $byNonBlocking1 || $byNonBlocking2\n$p")
          }
          result
        }

        // Split into elimable and non
        val (elim, keep) = futs.partition(isElimable)
        if (!elim.isEmpty) {
          // Build (very schematically):
          // keeps >> elims with future stripped >> core
          val rest :+ toElim = elim
          val result = Futures(keep ++ rest, Seqs(Seq(toElim), core))
          if (interestingElimables > 1)
            Logger.fine(s"Eliminating futures: $futs\n${e.e}\n====>\n${keep.mkString(" >>\n")} >> --\n${elim.mkString(" >>\n")} >> --\n${core.e}")
          Some(result)
        } else None
      }*/
      case Future.Z(body) => {
        // TODO: This is a hack. We just never lift out anything that references an object field. But really we just care about referencing 
        //       objects that could be recursive with an enclosing field. Otherwise, the optimization can create deadlocks.
        lazy val noObjectRefs = {
          case object FoundException extends RuntimeException
          try {
            (new Transform {
              override val onExpression = {
                case GetField.Z(_, _) => throw FoundException
              }
            })(body)
            true
          } catch {
            case FoundException =>
              false
          }
        }

        val byNonBlocking1 = a.delayOf(body).maxFirstPubDelay == ComputationDelay() && (a.publicationsOf(body) only 1)
        if (byNonBlocking1 && noObjectRefs)
          Some(body.value)
        else
          None
      }
      case n @ New.Z(self, _, fields, _) => {
        var changed = false
        val newFields = fields.mapValues({
          case f @ FieldFuture.Z(a: Argument.Z) if !a.freeVars.contains(self) => {
            changed = true
            (FieldArgument(a.value), None, None)
          }
          case f @ FieldFuture.Z(body) if !body.freeVars.contains(self) => {
            // TODO: This is a hack. We just never lift out anything that references an object field. But really we just care about referencing 
            //       objects that could be recursive with this one. This happens with the this arguments of partial constructors.
            lazy val noObjectRefs = {
              case object FoundException extends RuntimeException
              try {
                (new Transform {
                  override val onExpression = {
                    case GetField.Z(_, _) => throw FoundException
                  }
                })(body)
                true
              } catch {
                case FoundException =>
                  false
              }
            }

            val byNonBlocking1 = a.delayOf(body).maxFirstPubDelay == ComputationDelay() && (a.publicationsOf(body) only 1)
            lazy val x = new BoundVar()
            if (byNonBlocking1 && noObjectRefs) {
              changed = true
              (FieldArgument(x), Some(body), Some(x))
            } else
              (f.value, None, None)
          }
          case f => (f.value, None, None)
        }).view.force

        if (changed) {
          val exprs = newFields.values.collect({ case (_, Some(expr), Some(x)) => (expr, x) })
          val newF = newFields.mapValues(_._1).view.force
          Some(exprs.foldRight(n.value.copy(bindings = newF): Expression)((p, c) => Branch(p._1.value, p._2, c)))
        } else
          None
      }
      case _ => None
    }
  }

  val UnusedFutureElim = Opt("unused-future-elim") {
    case (Branch.Z(Future.Z(f), x, g), a) if !(g.freeVars contains x) =>
      (f.value >> Stop()) || g.value
  }

  val GetMethodElim = Opt("getmethod-elim") {
    case (GetMethod.Z(o), a) if a.valuesOf(o).forall({
      case n: NodeValue[_] => n.isMethod
      case _ => false
    }) =>
      o.value
  }

  val FutureForceElim = OptFull("future-force-elim") { (e, a) =>
    e match {
      case Branch.Z(Future.Z(e), x, f) =>
        val fforces = a.forces(f)
        val publications = a.publicationsOf(e)
        if(fforces.contains(x) && publications <= 1)
          Some(Branch(e.value, x, f.value))
        else {
          //Logger.info(s"Failed to apply future-force-elim $fforces $publications\nfuture { ${e.value.toString.take(100)} }\n>$x>\n${f.value.toString.take(100)}")
          None
        }
      case Branch.Z(e, x, f) => 
        //Logger.info(s"Failed to apply future-force-elim\n${e.value}\n>$x>\n${f.value}")
        None
      case _ => 
        None
    }
  }

  val ForceElim = OptFull("force-elim") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      case Force.Z(xs, vs, body) => {
        val (fs, nfs) = (xs zip vs).partition(v => a.valuesOf(v._2).futures.nonEmpty)
        def addAnalysis(p: (BoundVar, Argument.Z)) = {
          val (x, v) = p
          (x, v.value, a.valuesOf(v))
        }
        //Logger.fine(s"nfs = ${nfs.map(addAnalysis)}\nfs = ${fs.map(addAnalysis)}")
        val (newXs, newVs) = fs.unzip
        val newBody = body.value.substAll(nfs.map(p => (p._1, p._2.value)).toMap[Argument, Argument])
        if (fs.nonEmpty)
          Some(Force(newXs, newVs.map(_.value), newBody))
        else
          Some(newBody)
      }
      case _ => None
    }
  }

  val ResolveElim = OptFull("resolve-elim") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      case Resolve.Z(vs, body) => {
        val (fs, nfs) = vs.partition(v => a.valuesOf(v).futures.nonEmpty)
        if (fs.isEmpty)
          Some(body.value)
        else
          None
      }
      case _ => None
    }
  }

  val IfDefElim = OptFull("ifdef-elim") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      // TODO: The f == g should actually be equivalence up to local variable renaming.
      case IfLenientMethod.Z(v, f, g) if f.value == g.value =>
        Some(f.value)
      case IfLenientMethod.Z(v, f, g) =>
        val vs = CallGraph.targetsFromValue(a.valuesOf(v)).toSet
        val hasLenient = vs.exists(_ match {
          case NodeValue(MethodNode(_: Routine.Z, _)) => true
          case NodeValue(ExitNode(_: Call.Z)) => true
          case NodeValue(EverywhereNode) => true
          case _ => false
        })
        val hasStrict = vs.exists(_ match {
          case NodeValue(MethodNode(_: Service.Z, _)) => true
          case n: NodeValue[_] if !n.isExternalMethod.isFalse => true
          case NodeValue(ExitNode(_: Call.Z)) => true
          case NodeValue(EverywhereNode) => true
          case _ => false
        })
        if (hasLenient && !hasStrict) {
          Some(f.value)
        } else if (!hasLenient && hasStrict) {
          Some(g.value)
        } else {
          // If either both are available or neither then just leave this as is.
          None
        }
      case _ => None
    }
  }

  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f.value != Stop() &&
      (a.publicationsOf(f) only 0) &&
      (!a.effects(f)) &&
      a.delayOf(f).maxHaltDelay == ComputationDelay() =>
      //Logger.info(s"stop-equiv: ${f.value}\n====\n${a.delayOf(f)} ${a.publicationsOf(f)} ${a.effects(f)}")
      Stop()
  }

  val StopElim = OptSimple("stop-elim") {
    case Parallel.Z(Stop.Z(), g) => g.value
    case Parallel.Z(f, Stop.Z()) => f.value
    case Otherwise.Z(Stop.Z(), g) => g.value
    case Otherwise.Z(f, Stop.Z()) => f.value
    case Branch.Z(Stop.Z(), _, g) => Stop()
  }

  val BranchElimArg = OptFull("branch-elim-arg") { (e, a) =>
    e match {
      case Branch.Z(f, x, y) if x == y.value =>
        Some(f.value)
      case _ => None
    }
  }

  val BranchElimConstant = OptFull("branch-elim-const") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      case Branch.Z(a: Argument.Z, x, y) =>
        Some(y.value.subst(a.value, x))
      case Branch.Z(c, x, y) if (a.publicationsOf(c) only 1) && !a.effects(c) =>
        val vs = a.valuesOf(c).toSet
        val DelayInfo(delay, _) = a.delayOf(c)
        //println(s"branch-elim-const: $c\n${vs.values}, $effects, $delay")
        if (vs.size == 1 && delay == ComputationDelay()) {
          vs.head match {
            case NodeValue(ConstantNode(Constant(v), _)) =>
              Some(y.value.subst(Constant(v), x))
            case NodeValue(MethodNode(Method.Z(name, _, _, _, _, _), _)) if (e.freeVars contains name) && (e.contextBoundVars contains name) =>
              Some(y.value.subst(name, x))
            case _ =>
              None
          }
        } else {
          None
        }
      case _ =>
        None
    }
  }

  val BranchElim = OptFull("branch-elim") { (e, a) =>
    e match {
      case Branch.Z(f, x, g) if a.publicationsOf(f) only 0 => Some(f.value)
      case Branch.Z(f, x, g) if !a.effects(f) &&
        a.delayOf(f).maxFirstPubDelay == ComputationDelay() && a.delayOf(f).maxHaltDelay == ComputationDelay() &&
        (a.publicationsOf(f) only 1) && !g.freeVars.contains(x) => Some(g.value)
      case Branch.Z(f, x, g) =>
        //Logger.finest(s"Failed to elimate >>: ${f.effectFree} && ${f.nonBlockingPublish} && ${f.publications} only 1 && ${f.timeToHalt} && ${!g.freeVars.contains(x)} : ${e.e}")
        None
      case _ => None
    }
  }

  val TrimElim = Opt("trim-elim") {
    case _ if false => ???
    // TODO: Reenable this when DelayAnalysis is fixed for recursive functions.
    //case (Trim.Z(f), a) if a.publicationsOf(f) <= 1 && a.delayOf(f).maxHaltDelay == ComputationDelay() && !a.effects(f) => f.value
  }

  /*
  val LiftForce = OptFull("lift-force") { (e, a) =>
    import a.ImplicitResults._
    val freevars = e.freeVars
    val vars = e.forceTypes.flatMap {
      case (v, t) if freevars.contains(v) && t <= ForceType.Eventually(false) => Some(v)
      case _ => None
    }
    e match {
      case Pars(es, ctx) if es.size > 1 && vars.size > 0 => {
        val forceLimit = ForceType.Immediately(true)
        val (bestVar, matchingEs) = vars.map({ v =>
          val alreadyLifted = ctx.bindings exists {
            // TODO: The full force requirement may be too strong
            case b @ Bindings.ForceBound(_, _, _) if b.publishForce => true
            case _ => false
          }
          if (alreadyLifted || (v in ctx).valueForceDelay == Delay.NonBlocking) (v, 0)
          else (v, es.count { e => (e in ctx).forces(v) <= forceLimit })
        }).maxBy(_._2)

        if (matchingEs <= 1) {
          None
        } else {
          // We know at this point that there will be at least 2 matching elements.
          // But I leave checks for clarity.
          val (forcers, nonforcers) = es.partition(e => (e in ctx).forces(bestVar) <= forceLimit)

          // TODO: Allow lifting non-closure-forcing forces.
          def processedForcers = {
            val y = new BoundVar()
            val trans = new ContextualTransform.NonDescending {
              override def onExpressionCtx = {
                case ForceAt(xs, vs, true, e) if vs.exists(_.e == bestVar) =>
                  val revMap = (vs.map(_.e) zip xs).toMap
                  val (newVs, newXs) = (revMap - bestVar).unzip
                  val newE = e.subst(y, revMap(bestVar))
                  if (newXs.size > 0)
                    Force(newXs.toList, newVs.toList, true, newE)
                  else
                    newE
              }
            }
            Force(y, bestVar, true, trans(Pars(forcers)))
          }
          (forcers.size, nonforcers.isEmpty) match {
            case (n, _) if n <= 1 => None
            case (_, false) => Some(processedForcers || Pars(nonforcers))
            case (_, true) => Some(processedForcers)
          }
        }
      }
      case _ => None
    }
  }

  /*
  val BranchRHSInline= OptFull("branch-rhs-inline") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > g if g strictOn x =>
      case _ => None
    }
  }
  */

  val BranchExp = Opt("branch-expansion") {
    case (e @ (Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => a(f in ctx).silent) => {
      // This doesn't really eliminate any code and I cannot think of a case where
      val (sil, nsil) = fs.partition(f => a(f in ctx).silent)
      (sil.isEmpty, nsil.isEmpty) match {
        case (false, false) => (Pars(nsil) > x > g) || Pars(sil)
        case (false, true) => Pars(sil)
        case (true, false) => e
      }
    }
    case (e @ (Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => f.isInstanceOf[Constant]) => {
      val cs = fs.collect { case c: Constant => c }
      val es = fs.filter(f => !f.isInstanceOf[Constant])
      (cs.isEmpty, es.isEmpty) match {
        case (false, false) => (Pars(es) > x > g.e) || Pars(cs.map(g.e.subst(_, x)))
        case (false, true) => Pars(cs.map(g.e.subst(_, x)))
        case (true, false) => e.e
      }
    }
  }

  val BranchReassoc = Opt("branch-reassoc") {
    case (Seqs(ss, en), a) if ss.size > 1 => {
      Seqs(ss, en)
    }
  }
  val DefBranchNorm = Opt("def-branch-norm") {
    case (DeclareCallablesAt(defs, ctx, b) > x > e, a) if (e.freeVars & defs.map(_.name).toSet).isEmpty => {
      DeclareCallables(defs, b > x > e)
    }
  }

  val ConstProp = Opt("constant-propogation") {
    case (((y: Constant) in _) > x > g, a) => g.e.subst(y, x)
    case (((y: Argument) in ctx) > x > g, a) if a(y in ctx).nonBlockingPublish => g.subst(y, x)
    // FIXME: This may not be triggering in every case that it should.
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e @ (g > x > Pars(es, ctx)), a) if a(g).nonBlockingPublish && (a(g).publications only 1) &&
      es.exists(e => !e.freeVars.contains(x)) => {
      val (f, h) = es.partition(e => e.freeVars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (g > x > Pars(f)) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }

  val TrimCompChoice = Opt("trim-compiler-choice") {
    case (TrimAt(Pars(fs, ctx)), a) if fs.size > 1 && fs.exists(f => a(f in ctx).nonBlockingPublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f in ctx).nonBlockingPublish)
      Trim(f1)
    }
  }

  val inlineCostThreshold = co.options.optimizationFlags("orct:inline-threshold").asInt(15)
  val higherOrderInlineCostThreshold = co.options.optimizationFlags("orct:higher-order-inline-threshold").asInt(100)

  val InlineDef = OptFull("inline-def") { (e, a) =>
    import a.ImplicitResults._

    e match {
      case CallDefAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        // TODO: Add inlining for sites.
        case Bindings.CallableBound(dctx, decls, d: Def) => {
          val DeclareCallablesAt(_, declsctx, _) = decls in dctx
          val DefAt(_, _, body, _, _, _, _) = d in declsctx
          val cost = Analysis.cost(body)
          // If the body contains references to any other def in the recursive group.
          // TODO: This only really needs to check for calls I think.
          val bodyfree = body.freeVars
          val recursive = decls.defs exists { d1 => bodyfree.contains(d1.name) }
          val ctxsCompat = areContextsCompat(a, decls, d, ctx, dctx)
          val hasDefArg = args.exists {
            case x: BoundVar => isClosureBinding(ctx(x))
            case _ => false
          }
          //if (cost > costThreshold)
          //  println(s"WARNING: Not inlining ${d.name} because of cost ${cost}")
          if (!recursive && hasDefArg && cost <= higherOrderInlineCostThreshold && ctxsCompat) {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          } else if (!recursive && cost <= inlineCostThreshold && ctxsCompat) {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          } else {
            Logger.finest(s"Failed to inline: ${e.e} hasDefArg=$hasDefArg ctxsCompat=$ctxsCompat cost=$cost recursive=$recursive (higherOrderInlineCostThreshold=$higherOrderInlineCostThreshold inlineCostThreshold=$inlineCostThreshold)")
            None
          }
        }
        case _ => None
      }
      case _ => None
    }
  }

  val unrollCostThreshold = co.options.optimizationFlags("orct:unroll-threshold").asInt(45)

  val UnrollDef = OptFull("unroll-def") { (e, a) =>
    import a.ImplicitResults._

    throw new AssertionError("Unrolling doesn't work.")

    e match {
      case CallDefAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        // TODO: Add unrolling for sites.
        case Bindings.RecursiveCallableBound(dctx, decls, d: Def) => {
          val DeclareCallablesAt(_, declsctx, _) = decls in dctx
          val DefAt(_, _, body, _, _, _, _) = d in declsctx
          def cost = Analysis.cost(body)
          if (cost > unrollCostThreshold) {
            Logger.finest(s"Failed to unroll: ${e.e} cost=$cost (unrollCostThreshold=$unrollCostThreshold)")
            None
          } else {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          }
        }
        case _ => None
      }
      case _ => None
    }
  }

  // TODO: Rename all type variables inside the function body.
  def buildInlineDef(d: Def, args: List[Argument], targs: Option[List[Type]], ctx: TransformContext, a: ExpressionAnalysisProvider[Expression]) = {
    val bodyWithValArgs = d.body.substAll(((d.formals: List[Argument]) zip args).toMap)
    val typeSubst = targs match {
      case Some(as) => (d.typeformals: List[Typevar]) zip as
      case None => (d.typeformals: List[Typevar]) map { (t) => (t, Bot()) }
    }

    //Logger.finer(s"Inlining:\n$d\nwith args $args $targs")

    val boundVarCache = collection.mutable.HashMap[BoundVar, BoundVar]()
    def replaceVar(x: BoundVar) = {
      def newVar = {
        val name = x.optionalVariableName.map { n =>
          hasAutomaticVariableName.getNextVariableName(n.takeWhile(!_.isDigit).dropWhile(_ == '`'))
        }
        new BoundVar(name)
      }
      boundVarCache.getOrElseUpdate(x, newVar)
    }

    val freevars = bodyWithValArgs.freeVars

    val trans = new ContextualTransform.Pre {
      override def onExpression(implicit ctx: TransformContext) = {
        case left > x > right => left > replaceVar(x) > right
        case Force(xs, vs, b, e) => Force(xs.map(replaceVar), vs, b, e)
      }

      override def onCallable(implicit ctx: TransformContext) = {
        case d @ Def(name, formals, body, typeformals, argtypes, returntype) => {
          d.copy(name = replaceVar(name), formals = formals.map(replaceVar))
        }
        // TODO: Add handling of sites.
      }

      override def onArgument(implicit ctx: TransformContext) = {
        case x: BoundVar if !freevars.contains(x) => replaceVar(x)
      }
    }

    val result = trans(bodyWithValArgs.substAllTypes(typeSubst.toMap))
    //Logger.finer(s"Inlined:\n$result")
    result
  }

  def areContextsCompat(a: ExpressionAnalysisProvider[Expression], decls: DeclareCallables,
    d: Def, dctx: TransformContext, ctx: TransformContext) = {
    val DeclareCallablesAt(_, declsctx, _) = decls in dctx
    // TODO: This will crash on encountering a site. It should handle it.
    val DefAt(_, _, body, _, _, _, _) = d in declsctx
    val bodyfree = body.freeVars
    def isRelevant(b: Bindings.Binding) = bodyfree.contains(b.variable)
    val ctxTrimed = ctx.bindings.filter(isRelevant).map(_.nonRecursive)
    val dctxTrimed = dctx.bindings.filter(isRelevant).map(_.nonRecursive)
    val res = compareBindingSets(ctxTrimed, dctxTrimed)
    if (!res) {
      Logger.finest(s"Incompatible ctxs: decls: ${decls.defs.map(_.name).mkString("[", ",", "]")} d=$d\n$ctxTrimed\n$dctxTrimed")
    }
    res
  }

  def compareBindingSets(ctx1: Set[Bindings.Binding], ctx2: Set[Bindings.Binding]): Boolean = {
    ctx1 forall { e1 =>
      ctx2.exists { e2 =>
        e1.ast == e2.ast && e1.variable == e2.variable
      }
    }
  }

  val DefElim = Opt("def-elim") {
    case (DeclareCallablesAt(defs, ctx, b), a) if (b.freeVars & defs.map(_.name).toSet).isEmpty => b
  }

  def containsType(b: Expression, tv: BoundTypevar) = {
    var found = false
    (new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = {
        case `tv` =>
          found = true; tv
        case e: Typevar => e
      }
    })(b)
    found
  }
  val TypeElim = Opt("type-elim") {
    case (DeclareTypeAt(tv, t, b), a) if !containsType(b, tv) => b
  }

  def isClosureBinding(b: Bindings.Binding): Boolean = {
    import Bindings._
    b match {
      case CallableBound(_, _, d) if d.isInstanceOf[Def] => true
      case RecursiveCallableBound(_, _, d) if d.isInstanceOf[Def] => true
      // TODO: Should sites also be counted as closures?
      case ForceBound(ctx, f, x) =>
        f.argForVar(x) match {
          case y: BoundVar => isClosureBinding(ctx(y))
        }
      case SeqBound(ctx, (y: BoundVar) > x > _) => isClosureBinding(ctx(y))
      case _ => false
    }
  }

  val AccessorElim = Opt("accessor-elim") {
    case (FieldAccess(Constant(r: OrcRecord), f) in ctx, a) if r.entries.contains(f.field) =>
      Constant(r.entries(f.field))
    /*  case (CallAt(Constant(ProjectClosure) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("apply") =>
      Constant(r.getField(Field("apply")))
    case (CallAt(Constant(ProjectClosure) in _, List(v : BoundVar), _, ctx), a) if isClosureBinding(ctx(v)) =>
      v
    case (CallAt(Constant(ProjectUnapply) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("unapply") =>
      Constant(r.getField(Field("unapply")))
      */
  }

  val TupleFieldPattern = """_([0-9]+)""".r

  val TupleElim = OptFull("tuple-elim") { (e, a) =>
    import a.ImplicitResults._, Bindings._
    e match {
      //case FieldAccess(v: BoundVar, Field(TupleFieldPattern(num))) in ctx
      case CallSiteAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _) if (v in ctx).nonBlockingPublish =>
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, CallSite(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size && (args(i) in tctx).nonBlockingPublish =>
            Some(Force.asExpr(args(i), true))
          case _ => None
        }
      //case (FieldAccess(v: BoundVar, Field(TupleFieldPattern(num))) in ctx) > x > e
      case CallSiteAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _) > x > e if (v in ctx).nonBlockingPublish && !e.freeVars.contains(x) =>
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, CallSite(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size => Some(e)
          case _ => None
        }
      case CallSiteAt(Constant(TupleArityChecker) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) if (v in ctx).nonBlockingPublish =>
        val i = bi.intValue
        ctx(v) match {
          case SeqBound(tctx, CallSite(Constant(TupleConstructor), args, _) > `v` > _) if i == args.size => Some(v)
          case _ => None
        }
      // TODO: I may need a special case for removing tuple constructors.
      //case CallAt(Constant(TupleConstructor) in _, args, _, ctx) > x > e
      //        if args.size > 0 && args.forall(_.isInstanceOf[BoundVar]) && !e.freeVars.contains(x) =>
      case _ => None
    }
  }
  */
}

case class StandardOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(FutureForceElim, BranchElim, TrimElim, UnusedFutureElim, StopEquiv, 
      IfDefElim, ForceElim, ResolveElim, BranchElimArg, StopElim, BranchElimConstant, 
      FutureElim, GetMethodElim)
  /*
  val allOpts = List(
    BranchReassoc,
    DefBranchNorm, DefElim,
    LiftUnrelated, LiftForce,
    FutureElimFlatten, /*UnusedFutureElim,*/ FutureElim,
    /*FutureForceElim,*/ ForceElim, IfDefElim,
    TupleElim, AccessorElim,
    TrimCompChoice, TrimElim, ConstProp,
    StopEquiv, StopElim,
    BranchExp, BranchElim, BranchElimVar,
    InlineDef, TypeElim)
    */

  val opts = allOpts.filter { o =>
    val b = co.options.optimizationFlags(s"orct:${o.name}").asBool()
    Logger.fine(s"${if (b) "ENABLED" else "disabled"} ${o.name}")
    b
  }
}
/*
case class UnrollOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(
    UnrollDef)

  val opts = allOpts

  def transformFrom(f: PartialFunction[WithContext[Expression], Expression]) = new ContextualTransform.Post {
    override def onExpressionCtx = f
  }
}
*/

object Optimizer {
  //import WithContext._

  /*
  object Pars {
    private def pars(p: Expression): List[Expression] = {
      p match {
        case f || g => pars(f) ++ pars(g)
        case e => List(e)
      }
    }
    def unapply(e: Expression): Option[List[Expression]] = Some(pars(e))
    def unapply(e: WithContext[Expression]): Option[(List[Expression], TransformContext)] = {
      Some(pars(e.e), e.ctx)
    }

    def apply(l: Traversable[Expression]) = l.reduce(_ || _)
  }

  /** Match a sequence of expressions in the form: e1 >x1> ... >xn-1> en (ignoring association)
    */
  object Seqs {
    private def seqsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case f > x > g => {
          val (fss, fn) = seqsAt(f) // f1 >x1> ... >xn-1> fn
          val (gss, gn) = seqsAt(g) // g1 >y1> ... >yn-1> gn
          // TODO: Add an assert here to check that no variables are shadowed. This should never happen because of how variables are handled and allocated.
          // f1 >x1> ... >xn-1> fn >x> g1 >y1> ... >yn-1> gn
          (fss ::: (fn, x) :: gss, gn)
        }
        case e => (Nil, e)
      }
    }

    def unapply(e: WithContext[Expression]): Option[(List[(WithContext[Expression], BoundVar)], WithContext[Expression])] = {
      Some(seqsAt(e))
    }

    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]): Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression): Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => e > x > apply(sst, en)
      }
    }
  }
  */

  /*
  object IndependentSeqs {
    private def independent(p: WithContext[Expression], a: ExpressionAnalysisProvider[Expression]): (Seq[(Expression, BoundVar)], WithContext[Expression]) = {
      import a.ImplicitResults._
      val Seqs(seqs, core) = p
      def collectIndependents(
        seqs: List[(WithContext[Expression], BoundVar)],
        independents: Vector[(Expression, BoundVar)] = Vector[(Expression, BoundVar)](),
        ctx: TransformContext = core.ctx): (Seq[(Expression, BoundVar)], WithContext[Expression]) = seqs match {
        case (f, x) +: tl =>
          val isSafe = f.effectFree && f.nonBlockingPublish && (f.publications only 1)
          val noRefs = independents.map(_._2).forall { y => !f.freeVars.contains(y) }
          if (isSafe && noRefs) {
            // FIXME: This generates the context inside out. :/
            val _ > _ > (_ in newctx) = (f.e > x > core) in ctx
            collectIndependents(tl, independents :+ (f.e, x), newctx)
          } else {
            (independents, Seqs(seqs, core) in ctx)
          }
        case Nil =>
          (independents, core)
      }
      collectIndependents(seqs)
    }

    def unapply(p: (WithContext[Expression], ExpressionAnalysisProvider[Expression])): Option[(Seq[(Expression, BoundVar)], WithContext[Expression])] = {
      val (e, a) = p
      val (seqs, core) = independent(e, a)
      if (seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }
  */

  /*

  /* This uses the identity:
   * future x = future y = e # f # g
   * ===
   * future y = e # future x = f # g
   */

  object Futures {
    private def futsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case FutureAt(f) > x > g => {
          val (fss, fn) = futsAt(f) // f1 >x1> ... >xn-1> fn
          val (gss, gn) = futsAt(g) // g1 >y1> ... >yn-1> gn
          // TODO: Add an assert here to check that no variables are shadowed. This should never happen because of how variables are handled and allocated.
          // f1 >x1> ... >xn-1> fn >x> g1 >y1> ... >yn-1> gn
          (fss ::: (fn, x) :: gss, gn)
        }
        case e => (Nil, e)
      }
    }

    def unapply(e: WithContext[Expression]): Option[(List[(WithContext[Expression], BoundVar)], WithContext[Expression])] = {
      Some(futsAt(e))
    }

    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]): Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression): Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => Future(e) > x > apply(sst, en)
      }
    }
  }

  /** Match an expression in the form: e <x1<| g1 <x2<| g2 ... where x1,...,xn are distinct
    * and no gi references any xj.
    */
  object IndependentFutures {
    private def independent(p: WithContext[Expression], a: ExpressionAnalysisProvider[Expression]): (Seq[(Expression, BoundVar)], WithContext[Expression]) = {
      import a.ImplicitResults._
      val Futures(seqs, core) = p
      def collectIndependents(
        seqs: List[(WithContext[Expression], BoundVar)],
        independents: Vector[(Expression, BoundVar)] = Vector[(Expression, BoundVar)](),
        ctx: TransformContext = core.ctx): (Seq[(Expression, BoundVar)], WithContext[Expression]) = seqs match {
        case (f, x) +: tl =>
          val noRefs = independents.map(_._2).forall { y => !f.freeVars.contains(y) }
          if (noRefs) {
            // FIXME: This generates the context inside out. :/
            val _ > _ > (_ in newctx) = (f.e > x > core) in ctx
            collectIndependents(tl, independents :+ (f.e, x), newctx)
          } else {
            (independents, Futures(seqs, core) in ctx)
          }
        case Nil =>
          (independents, core)
      }
      collectIndependents(seqs)
    }

    def unapply(p: (WithContext[Expression], ExpressionAnalysisProvider[Expression])): Option[(Seq[(Expression, BoundVar)], WithContext[Expression])] = {
      val (e, a) = p
      val (seqs, core) = independent(e, a)
      if (seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }
  */
}
