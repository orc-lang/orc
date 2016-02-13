//
// Optimizer.scala -- Scala Optimizer
// Project OrcScala
//
// $Id$
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
import Bindings.{DefBound, RecursiveDefBound, SeqBound}
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


trait Optimization extends ((WithContext[Expression], ExpressionAnalysisProvider[Expression]) => Option[Expression]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name : String
  
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
abstract class Optimizer(co: CompilerOptions) {
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
                Logger.fine(s"${opt.name}: ${e.e.toString}\n==>\n${e2.toString}")
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
    case (FutureAt(g) > x > f, a) if a(f).forces(x) <= ForceType.Eventually && (a(g).publications only 1) 
            && a(g).nonBlockingPublish => 
              g > x > f
  }
  val FutureElim = Opt("future-elim") {
    // TODO: Add a halts with analysis (like I had back in the day) which will tell us if we can eliminate futures that may not publish.
    case (FutureAt(g) > x > f, a) if a(f).forces(x) == ForceType.Immediately(true) && (a(g).publications only 1) => g > x > f
    case (FutureAt(g) > x > f, a) if a(g).nonBlockingPublish && (a(g).publications only 1) => g > x > f
    case (FutureAt(g) > x > f, a) if !(f.freeVars contains x) => f || (g >> Stop()) 
    case (FutureAt(g) > x > (Stop() in _), a) => g > x > Stop()
  }
  val FutureForceElim = OptFull("future-force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case FutureAt(ForceAt((x: BoundVar) in ctx)) => ctx(x) match {
        case Bindings.SeqBound(ctx, Future(_) > _ > _) => Some(x)
        case b if isClosureBinding(b) => Some(x)
        case Bindings.ArgumentBound(ctx, _, _) => Some(x)
        case _ => None
      }
      case _ => None
    }
  }
  val ForceElim =  OptFull("force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case ForceAt((v: BoundVar) in _) > x > g 
          if !g.freeVars.contains(x) && g.forces(v) <= ForceType.Eventually && g.effectFree => 
        Some(g)
      case ForceAt(v) if v.valueForceDelay == Delay.NonBlocking => Some(v)
      case ForceAt((x: BoundVar) in ctx) => {
        val cannotBeFuture = ctx(x) match {
          case Bindings.SeqBound(fromCtx, from > _ > _) =>
            from match {
              case Call(_, _, _) => 
                // This assumes that no call will ever return a future. This means that no optimization can make defs publish futures.
                true
              case _ => false
            }
          case _ => false          
        }
        
        if (cannotBeFuture) {
          Some(x)
        } else {
          // Search the context for a SeqBound of force(x)
          val bindOpt = ctx.bindings find {
            case Bindings.SeqBound(_, Force(`x`) > _ > _) => true
            case _ => false
          }
          
  
          bindOpt map {
            case Bindings.SeqBound(_, _ > y > _) => {
              // Just replace this force with y that was already bound to the force.
              y
            }
          }
        }
      }
      case _ => None
    }
  }
  
  val LiftForce = OptFull("lift-force") { (e, a) =>
    import a.ImplicitResults._
    val freevars = e.freeVars
    val vars = e.forceTypes.flatMap {
      case (v, t) if freevars.contains(v) && t <= ForceType.Eventually => Some(v)
      case _ => None
    }
    e match {
      case Pars(es, ctx) if es.size > 1 && vars.size > 0 => {
        val forceLimit = ForceType.Immediately(true)
        val (bestVar, matchingEs) = vars.map({ v =>
          val alreadyLifted = ctx.bindings exists {
            case Bindings.SeqBound(_, Force(`v`) > _ > _) => true
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
          
          def processedForcers = {
            val y = new BoundVar()
            val trans = new ContextualTransform.NonDescending {
              override def onExpressionCtx = {
                case ForceAt(`bestVar` in _) => 
                  y
              }
            }
            Force(bestVar) > y > trans(Pars(forcers))
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

  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f != Stop() && 
      (a(f).publications only 0) && 
      a(f).effects == Effects.None && 
      a(f).timeToHalt == Delay.NonBlocking => 
        Stop()
  }
  val SeqElim = Opt("seq-elim") {
    case (f > x > g, a) if a(f).silent => f
    case (f > x > g, a) if a(f).effectFree && a(f).nonBlockingPublish && 
      (a(f).publications only 1) && a(f).nonBlockingHalt && !g.freeVars.contains(x) => g
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
    case (FutureAt(DeclareDefsAt(defs, ctx, b)), a)  => {
       DeclareDefs(defs, Future(b))
    }
  }
 
  val StopElim = OptSimple("stop-elim") {
    case (Stop() in _) || g => g.e
    case f || (Stop() in _) => f.e
    case (Stop() in _) ConcatAt g => g.e
    case f ConcatAt (Stop() in _) => f.e
  }
  
  val ParFlatten = OptFull("par-flatten") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case Pars(es, ctx) if es.size > 1 && es.exists(e => (e in ctx).nonBlockingHalt) => {
        val (nbs, bs) = es.partition(e => (e in ctx).nonBlockingHalt)
        /*if(nbs.size > 1)
          Logger.finer(s"Found par-flatten: $nbs\n|\n$bs")
        */
        (nbs.size, bs.isEmpty) match {
          case (n, _) if n < 1 => None
          case (_, false) => Some(Concat(nbs.reduce(Concat), Pars(bs)))
          case (_, true) => Some(nbs.reduce(Concat))
        }
      }
      case _ => None
    }
  }
  
  val ConstProp = Opt("constant-propogation") {
    case (((y : Constant) in _) > x > g, a) => g.e.subst(y, x)
    case (((y : Argument) in ctx) > x > g, a) if a(y in ctx).nonBlockingPublish => g.subst(y, x) 
    // FIXME: This may not be triggering in every case that it should.
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(g > x > Pars(es, ctx)), a) if a(g).nonBlockingPublish && 
            es.exists(e => !e.freeVars.contains(x)) => {
      val (f, h) = es.partition(e => e.freeVars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (g > x > Pars(f)) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }
  
  val LimitElim = Opt("limit-elim") {
    case (LimitAt(f), a) if a(f).publications <= 1 && a(f).effects <= Effects.BeforePub => f
  }
  val LimitCompChoice = Opt("limit-compiler-choice") {
    case (LimitAt(Pars(fs, ctx)), a) if fs.size > 1 && fs.exists(f => a(f in ctx).nonBlockingPublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f in ctx).nonBlockingPublish)
      Limit(f1)
    }    
  }
  
  val SiteForceElim = OptFull("site-force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case call@CallAt(target@Constant(_ : Site) in _, args, targs, ctx) => {
        val newargs = for (a <- args) yield {
          a match {
            case x: BoundVar => ctx(x) match {
              case Bindings.SeqBound(_, Force(v) > _ > _) => v
              case _ => x
            }
            case _ => a
          }
        }
        Some(Call(target, newargs, targs))
      }
      case _ => None
    }
  }
  
  // This only implements the very simplest way of detecting when the Flag will never be set or never be used.
  // This could be made smarter number of other ways. However note that because of dead code elimination this
  // will catch the main case where something that never publishes controls SetFlag.
  val FlagElim = OptFull("flag-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case CallAt(Constant(NewFlag) in _, _, _, ctx) > x > e => {
        var hasSet = false
        var hasCheck = false
        var hasOther = false
        (new ContextualTransform.NonDescending {
          override def onExpressionCtx = {
            case CallAt(Constant(SetFlag) in _, List(`x`), _, ctx) => {
              hasSet = true
              Stop()
            }
            case CallAt(Constant(PublishIfNotSet) in _, List(`x`), _, ctx) => {
              hasCheck = true
              Stop()
            }
            case `x` => {
              hasOther = true
              Stop()
            }
          }
          override def onArgument(implicit ctx: TransformContext) = {
            case `x` => {
              hasOther = true
              x
            }
          }
        })(e)
        //Logger.finer(s"Checked flag $x: set $hasSet check $hasCheck other $hasOther")
        if(!hasOther && (hasSet || hasCheck) && (!hasSet || !hasCheck)) {
          // If one or the other, but not both are set
          val s = if (hasCheck) PublishIfNotSet else SetFlag
          Some((new ContextualTransform.NonDescending {
            override def onExpressionCtx = {
              case CallAt(Constant(`s`) in _, List(`x`), _, ctx) => {
                Constant(Signal)
              }
            }
          })(e))
        } else {
          None
        }
      }
      case _ => None
    }
  }


  
  val inlineCostThreshold = co.options.optimizationFlags("orct:inline-threshold").asInt(15)
  val higherOrderInlineCostThreshold = co.options.optimizationFlags("orct:higher-order-inline-threshold").asInt(100)
  
  val InlineDef = OptFull("inline-def") { (e, a) =>
    import a.ImplicitResults._
    
    e match {
      case CallAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        case Bindings.DefBound(dctx, decls, d) => {
          val DeclareDefsAt(_, declsctx, _) = decls in dctx
          val DefAt(_, _, body, _, _, _, _) = d in declsctx
          val cost = Analysis.cost(body)
          val bodyfree = body.freeVars
          val recursive = bodyfree.contains(d.name)
          val ctxsCompat = areContextsCompat(a, decls, d, ctx, dctx)
          val hasDefArg = args.exists { 
            case x: BoundVar => ctx(x) match {
              case Bindings.SeqBound(yctx, Force(y: BoundVar) > _ > _) => isClosureBinding(yctx(y))
              case b => isClosureBinding(b)
            }
            case _ => false
          }
          //if (cost > costThreshold) 
          //  println(s"WARNING: Not inlining ${d.name} because of cost ${cost}")
          if (!recursive && hasDefArg && cost <= higherOrderInlineCostThreshold && ctxsCompat) {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          } else if (!recursive && cost <= inlineCostThreshold && ctxsCompat) {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          } else {
            Logger.finer(s"Failed to inline: ${e.e} hasDefArg=$hasDefArg ctxsCompat=$ctxsCompat cost=$cost recursive=$recursive (higherOrderInlineCostThreshold=$higherOrderInlineCostThreshold inlineCostThreshold=$inlineCostThreshold)")
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
    
    e match {
      case CallAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        case Bindings.RecursiveDefBound(dctx, decls, d) => {
          val DeclareDefsAt(_, declsctx, _) = decls in dctx
          val DefAt(_, _, body, _, _, _, _) = d in declsctx
          def cost = Analysis.cost(body)
          if (Analysis.cost(body) > unrollCostThreshold) {
            Logger.finer(s"Failed to unroll: ${e.e} cost=$cost (unrollCostThreshold=$unrollCostThreshold")
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
        case e@(left > x > right) => left > replaceVar(x) > right
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

    trans(bodyWithValArgs.substAllTypes(typeSubst.toMap))
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
      Logger.finer(s"Incompatible ctxs: decls: ${decls.defs.map(_.name).mkString("[", ",", "]")} d=$d\n$ctxTrimed\n$dctxTrimed")
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
  
  def isClosureBinding(b: Bindings.Binding) = {
    import Bindings._
    b match {
      case DefBound(_,_,_) | RecursiveDefBound(_,_,_) => true
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
      case CallAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _)
          if (v in ctx).nonBlockingPublish => 
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) 
            if i < args.size && (args(i) in tctx).nonBlockingPublish => 
              Some(Force(args(i)))
          case _ => None
        }
      //case (FieldAccess(v: BoundVar, Field(TupleFieldPattern(num))) in ctx) > x > e 
      case CallAt((v: BoundVar) in ctx, List(Constant(bi: BigInt)), _, _) > x > e
          if (v in ctx).nonBlockingPublish && !e.freeVars.contains(x) => 
        val i = bi.toInt
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size => Some(e)
          case _ => None
        }
      case CallAt(Constant(TupleArityChecker) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) 
          if (v in ctx).nonBlockingPublish => 
        val i = bi.intValue
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) if i == args.size => Some(v)
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
      FutureElimFlatten, FutureElim, 
      FutureForceElim, ForceElim,
      SiteForceElim, TupleElim, AccessorElim,
      LimitCompChoice, LimitElim, ConstProp, 
      StopEquiv, StopElim,
      SeqExp, SeqElim, SeqElimVar,
      InlineDef, TypeElim,
      ParFlatten, FlagElim
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
    /*private def latebinds(p: Expression): (Expression, List[(BoundVar, Expression)]) = {
      p match {
        case f < x <| g => {
          val (e1, lbs) = latebinds(f)
          (e1, lbs :+ (x, g))
        }
        case e => (e, Nil)
      }
    }*/
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
    
    def apply(ss: List[(WithContext[Expression], BoundVar)], en: Expression) : Expression = {
      ss match {
        case Nil => en
        case (e, x) :: sst => e > x > apply(sst, en)
      }
    }
  }
   
  // TODO: Might need a sequence version of this for rearranging sequences of futures.
  /*
  /**
   * Match an expression in the form: e <x1<| g1 <x2<| g2 ... where x1,...,xn are distinct
   * and no gi references any xi.  
   */
  object IndependantLateBinds {
    private def indepentant(lbs : List[(BoundVar, Expression)]) : Boolean = {
      val vars = lbs.map(_._1).toSet
      val freevars = lbs.flatMap(_._2.freevars).toSet
      vars.size == lbs.size && 
      vars.forall(v => !freevars.contains(v))
    } 
    
    def unapply(e: Expression): Option[(Expression, List[(BoundVar, Expression)])] = {
      e match {
        case LateBinds(e, lbs) if lbs.size > 0 && indepentant(lbs) => Some(e, lbs)
        case e => None
      }
    }
    def unapply(e: WithContext[Expression]): Option[(WithContext[Expression], List[(BoundVar, WithContext[Expression])])] = {
      e match {
        case LateBinds(e, lbs) if lbs.size > 0 && indepentant(lbs.map(p => (p._1, p._2.e))) => Some(e, lbs)))))))))
        case e => None
      }
    }
  }
  */
}

