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

import orc.values.sites.{Site => OrcSite}
import orc.values.sites.DirectSite

trait Optimization extends ((WithContext[Expr], AnalysisProvider[PorcAST]) => Option[Expr]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name : String
}

case class Opt(name : String)(f : PartialFunction[(WithContext[Expr], AnalysisProvider[PorcAST]), Expr]) extends Optimization {
  def apply(e : WithContext[Expr], analysis : AnalysisProvider[PorcAST]) : Option[Expr] = f.lift((e, analysis))
}
case class OptFull(name : String)(f : (WithContext[Expr], AnalysisProvider[PorcAST]) => Option[Expr]) extends Optimization {
  def apply(e : WithContext[Expr], analysis : AnalysisProvider[PorcAST]) : Option[Expr] = f(e, analysis)
}


/**
  *
  * @author amp
  */
case class Optimizer(opts : Seq[Optimization]) {
  def apply(e : Expr, analysis : AnalysisProvider[PorcAST]) : Expr = {
    val trans = new ContextualTransform.Pre {
      override def onExpr = {
        case (e: WithContext[Expr]) => {
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.e != e2) {
                  Logger.finer(s"${opt.name}: ${e.e.toString.replace("\n", " ").take(60)} ==> ${e2.toString.replace("\n", " ").take(60)}")
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
}

object Optimizer {
  object <::: {
    def unapply(e: WithContext[PorcAST]) = e match {
      case Sequence(l) in ctx =>
         Some(Sequence(l.init) in ctx, l.last in ctx)
      case _ => None
    }
  }
  object :::> {
    def unapply(e: WithContext[PorcAST]) = e match {
      case Sequence(e :: l) in ctx =>
         Some(e, Sequence(l) in ctx)
      case _ => None
    }
  }
  
  val letInlineThreshold = 30
  val letInlineCodeExpansionThreshold = 30
  val referenceThreshold = 5
  
  val InlineLet = OptFull("inline-let") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case CallIn((t:Var) in ctx, args, _) => ctx(t) match {
        case LetBound(dctx, Let(`t`, Lambda(formals, b), k)) => {
          val compat = ctx.compatibleFor(b)(dctx)
          lazy val size = Analysis.cost(b)
          def smallEnough = size <= letInlineThreshold
          lazy val referencedN = Analysis.count(k, {
            case Call(`t`, _) => true
            case _ => false
          })
          //val referencedN = 2
          //println(s"Inline attempt: ${e.e} ($referencedN, $size, $compat, ${l.d.body.referencesCounter}, ${l.d.body.referencesTerminator})")
          if ( !compat || (referencedN-1)*size > letInlineCodeExpansionThreshold )
            None // No inlining of recursive, heavily referenced, or large functions.
          else
            Some(b.substAll((formals zip args).toMap))
        }
        case _ => None
      }
      case _ => None
    }
  }
  
  val EtaReduce = Opt("eta-reduce") {
    case (LambdaIn(formals, _, CallIn(t, args, _)), a) if args.toList == formals.toList => t
  }
  
  val LetElim = Opt("let-elim") {
    case (LetIn(x, v, b), a) if !b.freevars.contains(x) && a(v).doesNotThrowHalt => b 
    case (LetIn(x, v, b), a) if !b.freevars.contains(x) => v ::: b
  }
  val VarLetElim = Opt("var-let-elim") {
    case (LetIn(x, (y:Var) in _, b), a) => b.substAll(Map((x, y)))
  }
  val SiteElim = Opt("site-elim") {
    case (SiteIn(ds, _, b), a) if (b.freevars & ds.map(_.name).toSet).isEmpty => b
  }
  
  val ForceElim = Opt("force-elim") {
    case (ForceIn(List(), _, b), a) => b()
    case (ForceIn(args, ctx, b), a) if args.forall(v => a(v in ctx).isNotFuture) => b(args: _*)
    // FIXME: Add form that removes the variables that are not futures but leaves the futures.
  }

  val spawnCostInlineThreshold = 30

  val InlineSpawn = OptFull("inline-spawn") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case SpawnIn((t: Var) in ctx) => ctx(t) match {
        case LetBound(dctx, Let(`t`, Lambda(_,b), _)) => {
          val c = Analysis.cost(b)
          if( c <= spawnCostInlineThreshold )
            Some(t())
            else
              None
        }
        case _ => None
      }
      case _ => None
    }
  }
  
  val ExternalSiteCall = Opt("external-sitecall") {
    case (SiteCallIn(OrcValue(s: OrcSite) in _, args, p, ctx), a) => 
      import PorcInfixNotation._ 
      val pp = new Var("pp")
      val (xs, ys) = (args collect {
        case x : Var => (x, new Var())
      }).unzip
      
      def pickArg(a: Value) = {
        if(xs contains a) {
          ys(xs.indexOf(a))
        } else {
          a
        }          
      } 
      
      val callArgs = args map pickArg
      val callImpl = s match {
        case s : DirectSite => {
          val v = new Var("v")
          TryOnHalted( {
            let((v, DirectSiteCall(OrcValue(s), callArgs))) {
              p(v)
            }
          }, Unit())
        }
        case _ => ExternalCall(s, callArgs, p)
      }
      
      val impl = let((pp, lambda(ys:_*){ callImpl })) {
        Force(xs, pp)
      }
      if( s.effectFree )
        impl
      else
        CheckKilled() ::: impl
  }
  
  val OnHaltedElim = Opt("onHalted-elim") {
    case (TryOnHaltedIn(LetIn(x, v, TryOnHaltedIn(b, h1)), h2), a) if h1.e == h2.e =>
      TryOnHalted(Let(x, v, b), h2)
    case (TryOnHaltedIn(s <::: TryOnHaltedIn(b, h1), h2), a) if h1.e == h2.e =>
      TryOnHalted(s ::: b, h2)
  }

  val opts = List(ExternalSiteCall, InlineSpawn, EtaReduce, ForceElim, VarLetElim, InlineLet, LetElim, SiteElim, OnHaltedElim)

  /*
  This may not be needed because site inlining is already done in Orc5C
  
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
}