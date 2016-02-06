//
// Optimizer.scala -- Scala class/trait/object Optimizer
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 3, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.values.sites.{ Site => OrcSite }
//TODO: import orc.values.sites.DirectSite
import orc.compile.CompilerOptions

trait Optimization extends ((WithContext[Expr], AnalysisProvider[PorcAST]) => Option[Expr]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name: String
}

case class Opt(name: String)(f: PartialFunction[(WithContext[Expr], AnalysisProvider[PorcAST]), Expr]) extends Optimization {
  def apply(e: WithContext[Expr], analysis: AnalysisProvider[PorcAST]): Option[Expr] = f.lift((e, analysis))
}
case class OptFull(name: String)(f: (WithContext[Expr], AnalysisProvider[PorcAST]) => Option[Expr]) extends Optimization {
  def apply(e: WithContext[Expr], analysis: AnalysisProvider[PorcAST]): Option[Expr] = f(e, analysis)
}

/** @author amp
  */
case class Optimizer(co: CompilerOptions) {
  def apply(e: PorcAST, analysis: AnalysisProvider[PorcAST]): PorcAST = {
    val trans = new ContextualTransform.Pre {
      override def onExpr = {
        case (e: WithContext[Expr]) => {
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.e != e2) {
                  Logger.fine(s"${opt.name}: ${e.e.toString.replace("\n", " ").take(60)} ==> ${e2.toString.replace("\n", " ").take(60)}")
                  e2 in e.ctx
                } else
                  e
            }
          })
          e1.e
        }
      }
    }

    trans(e)
  }

  import Optimizer._

  // FIXME: Some inlining is occuring that makes code execute in the wrong terminator.

  val letInlineThreshold = co.options.optimizationFlags("porc:let-inline-threshold").asInt(30)
  val letInlineCodeExpansionThreshold = co.options.optimizationFlags("porc:let-inline-expansion-threshold").asInt(30)
  val referenceThreshold = co.options.optimizationFlags("porc:let-inline-ref-threshold").asInt(5)

  val InlineLet = OptFull("inline-let") { (expr, a) =>
    import a.ImplicitResults._
    expr match {
      case LetIn(x in _, lam @ ContinuationIn(formal, _, impl), scope) =>
        def size = impl.cost
        lazy val (noncompatReferences, compatReferences, compatCallsCost) = {
          var refs = 0
          var refsCompat = 0
          var callsCost = 0
          (new ContextualTransform.Pre {
            override def onVar = {
              case `x` in _ =>
                refs += 1
                x
            }
            override def onValue = {
              case `x` in _ =>
                refs += 1
                x
            }
            override def onExpr = {
              case e @ (Call(`x`, _) in usectx) =>
                if (usectx.compatibleFor(impl)(expr.ctx)) {
                  refsCompat += 1
                  callsCost += e.cost
                } else {
                  //Logger.finest(s"Incompatible call site for $x: ${e.e}")
                }
                e
              case `x` in _ =>
                refs += 1
                x
            }
          })(scope)
          (refs - refsCompat, refsCompat, callsCost)
        }

        val codeExpansion = compatReferences * size - compatCallsCost -
          (if (noncompatReferences == 0) size else 0)

        val doInline = new ContextualTransform.NonDescending {
          override def onExpr = {
            case Call(`x`, arg) in usectx if usectx.compatibleFor(impl)(expr.ctx) =>
              impl.substAll(Map((formal, arg)))
          }
        }

        Logger.finer(s"Attempting inline: $x: $compatReferences $noncompatReferences $compatCallsCost $size; $codeExpansion")
        if (compatReferences > 0 && codeExpansion <= letInlineCodeExpansionThreshold) {
          if (noncompatReferences > 0)
            Some(Let(x, lam, doInline(scope)))
          else
            Some(doInline(scope))
        } else {
          None
        }
      case e =>
        None
    }
  }

  /*
  This may not be needed because site inlining is already done in Orc5C

  val spawnCostInlineThreshold = co.options.optimizationFlags("porc:spawn-inline-threshold").asInt(30)

  val InlineSpawn = OptFull("inline-spawn") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case SpawnIn((t: Var) in ctx) => ctx(t) match {
        case LetBound(dctx, l @ Let(`t`, Lambda(_, _), _)) => {
          val LetIn(_, LambdaIn(_, _, b), _) = l in dctx
          val c = b.cost
          if (c <= spawnCostInlineThreshold && b.fastTerminating)
            Some(t())
          else
            None
        }
        case _ => None
      }
      case _ => None
    }
  }

  
  val siteInlineThreshold = 50
  val siteInlineCodeExpansionThreshold = 50
  val siteInlineTinySize = 12
   
  val InlineSite = OptFull("inline-site") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case SiteCallIn((t:Var) in ctx, args, _) => ctx(t) match {
        case SiteBound(dctx, Site(_,k), d) => {
          def recursive = d.body.freevars.contains(d.name)
          lazy val compat = ctx.compatibleForSite(d.body)(dctx)
          lazy val size = Analysis.cost(d.body)
          def smallEnough = size <= siteInlineThreshold
          lazy val referencedN = Analysis.count(k, {
            case SiteCall(`t`, _) => true
            case _ => false
          })
          def tooMuchExpansion = (referencedN-1)*size > siteInlineCodeExpansionThreshold && size > siteInlineTinySize
          //println(s"Inline attempt: ${e.e} ($referencedN, $size, $compat)")
          if ( recursive || !smallEnough || !compat || tooMuchExpansion )
            None // No inlining of recursive functions or large functions.
          else
            Some(d.body.substAll(((d.arguments: List[Var]) zip args).toMap))
        }
        case _ => None
      }
      case _ => None
    }
  }
   */

  val allOpts = List(EtaReduce, VarLetElim, SpecializeSiteCall, InlineLet, LetElim, SiteElim, OnHaltedElim)

  val opts = allOpts.filter { o =>
    co.options.optimizationFlags(s"porc:${o.name}").asBool()
  }
}

object Optimizer {
  object <::: {
    def unapply(e: WithContext[PorcAST]) = e match {
      case Sequence(l) in ctx if !l.isEmpty =>
        Some(Sequence(l.init) in ctx, l.last in ctx)
      case _ => None
    }
  }
  object :::> {
    def unapply(e: WithContext[PorcAST]) = e match {
      case Sequence(e :: l) in ctx =>
        Some(e in ctx, Sequence(l) in ctx)
      case _ => None
    }
  }

  object LetStackIn {
    def unapply(e: WithContext[PorcAST]): Some[(Seq[(Option[WithContext[Var]], WithContext[Expr])], WithContext[PorcAST])] = e match {
      case LetIn(x, v, b) =>
        val LetStackIn(bindings, b1) = b
        Some(((Some(x), v) +: bindings, b1))
      case s :::> ss if !ss.es.isEmpty =>
        val LetStackIn(bindings, b1) = ss.simplify in ss.ctx
        //Logger.fine(s"unpacked sequence: $s $ss $bindings $b1")
        Some(((None, s) +: bindings, b1))
      case _ => Some((Seq(), e))
    }
  }

  object LetStack {
    def apply(bindings: Seq[(Option[WithContext[Var]], WithContext[Expr])], b: Expr) = {
      bindings.foldRight(b)((bind, b) => {
        bind match {
          case (Some(x), v) =>
            Let(x, v, b)
          case (None, v) =>
            v ::: b
        }
      })
    }
    /*def apply(bindings: Map[Var, Expr], b: Expr) = {
      bindings.foldRight(b)((bind, b) => {
        val (x, v) = bind
        Let(x, v, b)
      })
    }*/
  }

  val EtaReduce = Opt("eta-reduce") {
    case (ContinuationIn(formal, _, CallIn(t, arg, _)), a) if arg == formal => t
  }

  val LetElim = Opt("let-elim") {
    case (LetIn(x, v, b), a) if !b.freevars.contains(x) && a(v).doesNotThrowHalt => b
    case (LetIn(x, v, b), a) if !b.freevars.contains(x) => v ::: b
  }
  val VarLetElim = Opt("var-let-elim") {
    case (LetIn(x, (y: Var) in _, b), a) => b.substAll(Map((x, y)))
  }
  val SiteElim = Opt("site-elim") {
    case (SiteIn(ds, _, b), a) if (b.freevars & ds.map(_.name).toSet).isEmpty => b
  }

  val SpecializeSiteCall = OptFull("specialize-sitecall") { (e, a) =>
    import a.ImplicitResults._
    import PorcInfixNotation._
    e match {
      case SiteCallIn(target, p, c, t, args, ctx) 
          if target.isNotFuture && args.forall(v => (v in ctx).isNotFuture) &&
             target.siteMetadata.map(_.isDirectCallable).getOrElse(false)=>
        val v = new Var()
        Some(
          TryOnHalted({
            let((v, SiteCallDirect(target, args))) {
              p(v)
            }
          }, Unit()))
      case _ => None
    }
  }

  val OnHaltedElim = OptFull("onhalted-elim") { (e, a) =>
    e match {
      case TryOnHaltedIn(LetStackIn(bindings, TryOnHaltedIn(b, h1)), h2) if h1.e == h2.e =>
        Some(TryOnHalted(LetStack(bindings, b), h2))
      case _ => None
    }
  }
}