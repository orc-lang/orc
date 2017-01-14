//
// Optimizer.scala -- Scala Optimizer
// Project OrcScala
//
// Created by amp on Sept 16, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.compile.Logger
import orc.values.OrcRecord
import orc.ast.orctimizer.named._
import Bindings.{DefBound, RecursiveDefBound, SeqBound, FutureBound}
import orc.values.Field
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.TupleArityChecker
import orc.compile.CompilerOptions
import orc.types
import orc.values.sites.Delay
import orc.values.sites.Effects
import orc.values.sites.Site
import orc.lib.state.NewFlag
import orc.lib.state.SetFlag
import orc.lib.state.PublishIfNotSet
import orc.values.Signal
import orc.ast.hasAutomaticVariableName
import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable
import orc.ast.orctimizer.named.Bindings.SeqBound
import orc.compile.OptimizerStatistics
import orc.compile.NamedOptimization


trait Optimization extends ((WithContext[Expression], ExpressionAnalysisProvider[Expression]) => Option[Expression]) with NamedOptimization {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  val name : String
  
  override def toString = name
}

case class Opt(name : String)(f : PartialFunction[(WithContext[Expression], ExpressionAnalysisProvider[Expression]), Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f.lift((e, analysis))
}
case class OptSimple(name : String)(f : PartialFunction[WithContext[Expression], Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f.lift(e)
}
case class OptFull(name : String)(f : (WithContext[Expression], ExpressionAnalysisProvider[Expression]) => Option[Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f(e, analysis)
}

// TODO: Implement compile time evaluation of select sites.

/* Assumptions in the optimizer:
 * 
 * No call (def or site) can publish a future.
 * 
 */

/**
  *
  * @author amp
  */
abstract class Optimizer(co: CompilerOptions) extends OptimizerStatistics {
  def opts: Seq[Optimization]
  
  def transformFrom(f: PartialFunction[WithContext[Expression], Expression]): ContextualTransform
  
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression]) : Expression = {
    val trans = transformFrom {
      case (e: WithContext[Expression]) => {
        val e1 = opts.foldLeft(e)((e, opt) => {
          opt(e, analysis) match {
            case None => e
            case Some(e2) =>
              if (e.e != e2) {
                import orc.util.StringExtension._
                Logger.fine(s"${opt.name}: ${e.e.toString.truncateTo(60)}\n====>\n${e2.toString.truncateTo(60)}")
                countOptimization(opt)                
                e2 in e.ctx
              } else
                e
          }
        })
        e1.e
      }
    }
   
    val r = trans(e)

    val check = transformFrom {
      case (e: Expression) in ctx => {
        for (v <- e.freeVars) {
          try {
            ctx(v)
          } catch {
            case exc: UnboundVariableException => {
              throw new AssertionError(s"Variable $v not bound in expression:\n$e\n=== in context: $ctx\n=== in overall program:\n$r", exc)
            }
          }
        }
        e
      }
    }
    
    check(r)

    r
  }

  import Optimizer._

  val flattenThreshold = co.options.optimizationFlags("orct:future-flatten-threshold").asInt(5)

  val FutureElimFlatten = Opt("future-elim-flatten") {
    // TODO: This may not be legal. What about small expressions that could still block on something.
    /*case (FutureAt(g) > x > f, a) if a(f).forces(x) <= ForceType.Eventually && (a(g).publications only 1) 
            && Analysis.cost(g) <= flattenThreshold => 
              g > x > f
              */
    case (e, a) if false => e
  }
  val FutureElim = OptFull("future-elim") { (e, a) =>
    import a.ImplicitResults._
    (e, a) match {
      case IndependentFutures(futs, core) => {
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
          if(interestingElimables > 1)
            Logger.fine(s"Eliminating futures: $futs\n${e.e}\n====>\n${keep.mkString(" >>\n")} >> --\n${elim.mkString(" >>\n")} >> --\n${core.e}")
          Some(result)
        } else None
      }
      case _ => None
    }
  }
  val UnusedFutureElim = Opt("unused-future-elim") {
    case (FutureAt(x, f, g), a) if !(g.freeVars contains x) => g || (f >> Stop()) 
  }
  
  // TODO: Evaluate and port if needed.
  /*
  val FutureForceElim = OptFull("future-force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case FutureAt(x, ForceAt((y: BoundVar) in ctx, true), r) => 
        def successRes = Some(r.subst(y, x))
        ctx(y) match {
          case Bindings.FutureBound(ctx, _) => successRes
          case b if isClosureBinding(b) => successRes
          case Bindings.ArgumentBound(ctx, _, _) => successRes
          case _ => None
        }
      case _ => None
    }
  }
  */
  
  val ForceElim =  OptFull("force-elim") { (e, a) =>
    import a.ImplicitResults._
    val ctx = e.ctx
    e match {
      /*case ForceAt(xs, vs, _, g) if !g.freeVars.contains(x) && g.forces(v) <= ForceType.Eventually(false) && g.effectFree => 
        Some(g)*/
      //case ForceAt(xs, vs, _, g) if v.isFuture || v.isDef => Some(v)
      case fe@ForceAt(xs, vs, forceClosures, e) => {
        // Determine which of vs cannot be futures/closures (with checking of forceClosures)
        def cannotBeFuture(a: Argument) = a match {
          case x: BoundVar =>
            ctx(x) match {
              case Bindings.SeqBound(_, from > _ > _) =>
                true
              case _: Bindings.DefBound | _: Bindings.RecursiveDefBound if !forceClosures =>
                true
              case Bindings.ForceBound(_, _, _) => true
              case _ => false          
            }
          case _: Constant => true
          case _ => false
        }
        
        // Search for elements in vs that have already been forced in the context (with the same or better forceClosures)
        def forceInContext(a: Argument): Option[Argument] = a match {
          case x: BoundVar =>
            // Search the context for a ForceBound of x which has at least as strong a force type
            val bindOpt = ctx.bindings find {
              case Bindings.ForceBound(_, Force(xs, vs, fc, _), `x`) if !forceClosures || fc => true
              case _ => false
            }
            
            bindOpt map {
              case Bindings.ForceBound(_, fe@Force(_, _, _, _), _) => {
                //Logger.finer(s"Found preforced: $a from $fe")
                // Just replace this force with y that was already bound to the force.
                x
              }
            }
          case _ => None
        }
        
        //Logger.fine(s"Checking force: ${(xs zip vs.map(_.e))} ${forceClosures}")
        
        // Right replaces in e, Left leaves force
        val forceChanges = for((x, v) <- fe.e.asInstanceOf[Force].toMap) yield {
          //Logger.fine(s"$x = $v")
          //Logger.fine(s"${cannotBeFuture(v)} ${forceInContext(v)}")
          if(cannotBeFuture(v)) {
            (x, Right(v))
          } else {
            forceInContext(v) match {
              case Some(r) => 
                (x, Right(r))
              case None =>
                (x, Left(v))
            }
          }
        }
        
        // Replace xs in e and rebuild
        val (newXs, newVs) = forceChanges.collect({
          case (x, Left(v)) => (x, v)
        }).unzip
        
        val newE = forceChanges.foldLeft(e.e)((e, p) => p match {
          case (x, Right(v)) => e.subst(v, x: Argument)
          case _ => e
        })
        
        if(newXs.size > 0)
          Some(Force(newXs.toList, newVs.toList, forceClosures, newE))
        else 
          Some(newE)
      }
      case _ => None
    }
  }

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
            case b@Bindings.ForceBound(_, _, _) if b.publishForce => true 
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

  val IfDefElim = OptFull("ifdef-elim") { (e, a) =>
    import a.ImplicitResults._
    val ctx = e.ctx
    e match {
      case IfDefAt(a, f, g) =>
        a.e match {
          case _ if a.siteMetadata.isDefined =>
            // Sites are never defs and non-sites never get site metadata
            Some(g)
          case x: BoundVar =>
            ctx(x) match {
              case Bindings.SeqBound(_, from > _ > _) => None
              case _: Bindings.DefBound | _: Bindings.RecursiveDefBound =>
                Some(f)
              case Bindings.ForceBound(_, _, _) => None
              case _ => None
            } 
          case c: Constant =>
            // Constants are never defs
            Some(g)
          case _ => None
        }
      case _ => None
    }
  }

  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f != Stop() && 
      (a(f).publications only 0) && 
      a(f).effects == Effects.None && 
      a(f).timeToHalt == Delay.NonBlocking => 
        Stop()
  }
  val SeqElim = OptFull("seq-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > g if f.silent => Some(f)
      case f > x > g if f.effectFree && f.nonBlockingPublish && 
        (f.publications only 1) && f.nonBlockingHalt && !g.freeVars.contains(x) => Some(g)
      case f > x > g =>
        //Logger.finest(s"Failed to elimate >>: ${f.effectFree} && ${f.nonBlockingPublish} && ${f.publications} only 1 && ${f.timeToHalt} && ${!g.freeVars.contains(x)} : ${e.e}")
        None
      case _ => None
    }
  }
  val SeqElimVar = OptFull("seq-elim-var") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > y if x == y.e => Some(f)
      case ((x: Var) in _) > y > f if f.forces(y) == ForceType.Immediately(true) => Some(f.subst(x, y))
      //case ForceAt((x: Var) in _) > y > f if f.forces(x) == ForceType.Immediately(true) && !(a(f).freeVars contains y) => Some(f)
      case _ => None
    }
  }

  /*
  val SeqRHSInline= OptFull("seq-rhs-inline") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > g if g strictOn x => 
      case _ => None
    }
  } 
  */
  
  val SeqExp = Opt("seq-expansion") {
    case (e@(Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => a(f in ctx).silent) => {
      // This doesn't really eliminate any code and I cannot think of a case where 
      val (sil, nsil) = fs.partition(f => a(f in ctx).silent)
      (sil.isEmpty, nsil.isEmpty) match {
        case (false, false) => (Pars(nsil) > x > g) || Pars(sil)
        case (false, true) => Pars(sil)
        case (true, false) => e
      }
    }
    case (e@(Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => f.isInstanceOf[Constant]) => {
      val cs = fs.collect{ case c : Constant => c }
      val es = fs.filter(f => !f.isInstanceOf[Constant])
      (cs.isEmpty, es.isEmpty) match {
        case (false, false) => (Pars(es) > x > g.e) || Pars(cs.map(g.e.subst(_, x)))
        case (false, true) => Pars(cs.map(g.e.subst(_, x)))
        case (true, false) => e.e
      }
    }
  }
  
  val SeqReassoc = Opt("seq-reassoc") {
    case (Seqs(ss, en), a) if ss.size > 1 => {
      Seqs(ss, en) 
    }
  }
  val DefSeqNorm = Opt("def-seq-norm") {
    case (DeclareDefsAt(defs, ctx, b) > x > e, a) if (e.freeVars & defs.map(_.name).toSet).isEmpty  => {
       DeclareDefs(defs, b > x > e)
    }
  }
 
  val StopElim = OptSimple("stop-elim") {
    case (Stop() in _) || g => g.e
    case f || (Stop() in _) => f.e
    case (Stop() in _) OtherwiseAt g => g.e
    case f OtherwiseAt (Stop() in _) => f.e
  }
    
  val ConstProp = Opt("constant-propogation") {
    case (((y : Constant) in _) > x > g, a) => g.e.subst(y, x)
    case (((y : Argument) in ctx) > x > g, a) if a(y in ctx).nonBlockingPublish => g.subst(y, x) 
    // FIXME: This may not be triggering in every case that it should.
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(g > x > Pars(es, ctx)), a) if a(g).nonBlockingPublish && (a(g).publications only 1) && 
            es.exists(e => !e.freeVars.contains(x)) => {
      val (f, h) = es.partition(e => e.freeVars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (g > x > Pars(f)) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }
  
  val TrimElim = Opt("limit-elim") {
    case (TrimAt(f), a) if a(f).publications <= 1 && a(f).effects <= Effects.BeforePub => f
  }
  val TrimCompChoice = Opt("limit-compiler-choice") {
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
        case Bindings.DefBound(dctx, decls, d) => {
          val DeclareDefsAt(_, declsctx, _) = decls in dctx
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
        case Bindings.RecursiveDefBound(dctx, decls, d) => {
          val DeclareDefsAt(_, declsctx, _) = decls in dctx
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
      case Some(as) => (d.typeformals:List[Typevar]) zip as
      case None => (d.typeformals:List[Typevar]) map { (t) => (t, Bot()) }
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
        case Future(x, left, right) => Future(replaceVar(x), left, right)
        case Force(xs, vs, b, e) => Force(xs.map(replaceVar), vs, b, e)
      }
      
      override def onDef(implicit ctx: TransformContext) = {
        case d @ Def(name, formals, body, typeformals, argtypes, returntype) => {
          d.copy(name=replaceVar(name), formals=formals.map(replaceVar))
        }
      }

      override def onArgument(implicit ctx: TransformContext) = {
        case x: BoundVar if !freevars.contains(x) => replaceVar(x)
      }
    }

    val result = trans(bodyWithValArgs.substAllTypes(typeSubst.toMap))
    //Logger.finer(s"Inlined:\n$result")
    result
  }

  def areContextsCompat(a: ExpressionAnalysisProvider[Expression], decls: DeclareDefs, 
      d: Def, dctx: TransformContext, ctx: TransformContext) = {
    val DeclareDefsAt(_, declsctx, _) = decls in dctx
    val DefAt(_, _, body, _, _, _, _) = d in declsctx
    val bodyfree = body.freeVars
    def isRelevant(b: Bindings.Binding) = bodyfree.contains(b.variable)
    val ctxTrimed = ctx.bindings.filter(isRelevant).map(_.nonRecursive)
    val dctxTrimed = dctx.bindings.filter(isRelevant).map(_.nonRecursive)
    val res = compareBindingSets(ctxTrimed, dctxTrimed)
    if(!res) {
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
    case (DeclareDefsAt(defs, ctx, b), a) if (b.freeVars & defs.map(_.name).toSet).isEmpty => b
  }

  def containsType(b: Expression, tv: BoundTypevar) = {
    var found = false
    (new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = {
        case `tv` => found = true; tv
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
      case DefBound(_,_,_) | RecursiveDefBound(_,_,_) => true
      case ForceBound(ctx, f, x) => 
        f.argForVar(x) match {
          case y: BoundVar => isClosureBinding(ctx(y)) 
        }
      case SeqBound(ctx, (y: BoundVar) > x > _) => isClosureBinding(ctx(y))
      case _ => false
    } 
  }
  
  val AccessorElim = Opt("accessor-elim") {
    case (FieldAccess(Constant(r : OrcRecord), f) in ctx, a) if r.entries.contains(f.field) => 
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
      case CallSiteAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _)
          if (v in ctx).nonBlockingPublish => 
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, CallSite(Constant(TupleConstructor), args, _) > `v` > _) 
            if i < args.size && (args(i) in tctx).nonBlockingPublish => 
              Some(Force.asExpr(args(i), true))
          case _ => None
        }
      //case (FieldAccess(v: BoundVar, Field(TupleFieldPattern(num))) in ctx) > x > e 
      case CallSiteAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _) > x > e
          if (v in ctx).nonBlockingPublish && !e.freeVars.contains(x) => 
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, CallSite(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size => Some(e)
          case _ => None
        }
      case CallSiteAt(Constant(TupleArityChecker) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) 
          if (v in ctx).nonBlockingPublish => 
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
}

case class StandardOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(
      SeqReassoc,
      DefSeqNorm, DefElim, 
      LiftUnrelated, LiftForce,
      FutureElimFlatten, UnusedFutureElim, FutureElim, 
      /*FutureForceElim,*/ ForceElim, IfDefElim,
      TupleElim, AccessorElim,
      TrimCompChoice, TrimElim, ConstProp, 
      StopEquiv, StopElim,
      SeqExp, SeqElim, SeqElimVar,
      InlineDef, TypeElim
      )

  val opts = allOpts.filter{ o =>
    val b = co.options.optimizationFlags(s"orct:${o.name}").asBool()
    Logger.finest(s"${if(b) "ENABLED" else "disabled"} ${o.name}")
    b
  }
  
  def transformFrom(f: PartialFunction[WithContext[Expression], Expression]) = new ContextualTransform.Pre {
    override def onExpressionCtx = f
  }
}

case class UnrollOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(
      UnrollDef
      )

  val opts = allOpts

  def transformFrom(f: PartialFunction[WithContext[Expression], Expression]) = new ContextualTransform.Post {
    override def onExpressionCtx = f
  }
}

object Optimizer {
  import WithContext._
  
  //Logger.logAllToStderr()
  
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
    
    def apply(l : Traversable[Expression]) = l.reduce(_ || _)
  }
  
  /**
   * Match a sequence of expressions in the form: e1 >x1> ... >xn-1> en (ignoring association)
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
    
    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]) : Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression) : Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => e > x > apply(sst, en)
      }
    }
  }
  
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
      if(seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }

  /* This uses the identity:
   * future x = future y = e # f # g 
   * ===
   * future y = e # future x = f # g
   */
  
  object Futures {
    private def futsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case FutureAt(x, f, g) => {
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
    
    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]) : Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression) : Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => Future(x, e, apply(sst, en))
      }
    }
  }

   
  /**
   * Match an expression in the form: e <x1<| g1 <x2<| g2 ... where x1,...,xn are distinct
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
      if(seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }
}

