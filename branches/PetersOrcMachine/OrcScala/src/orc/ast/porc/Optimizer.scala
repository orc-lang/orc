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

trait Optimization extends ((WithContext[Command], AnalysisProvider[PorcAST]) => Option[Command]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name : String
}

case class Opt(name : String)(f : PartialFunction[(WithContext[Command], AnalysisProvider[PorcAST]), Command]) extends Optimization {
  def apply(e : WithContext[Command], analysis : AnalysisProvider[PorcAST]) : Option[Command] = f.lift((e, analysis))
}
case class OptFull(name : String)(f : (WithContext[Command], AnalysisProvider[PorcAST]) => Option[Command]) extends Optimization {
  def apply(e : WithContext[Command], analysis : AnalysisProvider[PorcAST]) : Option[Command] = f(e, analysis)
}


/**
  *
  * @author amp
  */
case class Optimizer(opts : Seq[Optimization]) {
  def apply(e : Command, analysis : AnalysisProvider[PorcAST]) : Command = {
    val trans = new ContextualTransform.Pre {
      override def onCommand = {
        case (e: WithContext[Command]) => {
          //println("Optimizing: " + (new PrettyPrint).reduce(e))
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.e != e2) {
                  println(s"${opt.name}: ${e.e.toString.replace("\n", " ").take(60)} ==> ${e2.toString.replace("\n", " ").take(60)}")
                  //println(s"${opt.name}: ${e.toString} ==> ${e2.toString}")
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
  val letInlineThreshold = 30
  val letInlineCodeExpansionThreshold = 30
  val referenceThreshold = 5
  
  val InlineLet = OptFull("inline-let") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case ClosureCallIn((t:Var) in ctx, args, _) => ctx(t) match {
        case LetBound(dctx, l) => {
          val compat = ctx.compatibleFor(l.d.body)(dctx)
          lazy val size = Analysis.cost(l.d.body)
          def smallEnough = size <= letInlineThreshold
          lazy val referencedN = Analysis.count(l.k, {
            case ClosureCall(`t`, _) => true
            case _ => false
          })
          //println(s"Inline attempt: ${e.e} ($referencedN, $size, $compat, ${l.d.body.referencesCounter}, ${l.d.body.referencesTerminator})")
          if ( !compat || (referencedN-1)*size > letInlineCodeExpansionThreshold )
            None // No inlining of recursive, heavily referenced, or large functions.
          else
            Some(l.d.body.substAll(((l.d.arguments: List[Var]) zip args).toMap))
        }
        case _ => None
      }
      case _ => None
    }
  }
  val EtaReduce = OptFull("eta-reduce") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case LetIn(ClosureDefIn(name, formals, _, ClosureCallIn(t, args, _)), body) if args.toList == formals.toList => {
        Some(body.substAll(Map((name, t))))
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
  
  val LetElim = Opt("let-elim") {
    case (LetIn(d, b), a) if !b.freevars.contains(d.name) => b
  }
  val SiteElim = Opt("site-elim") {
    case (SiteIn(ds, _, b), a) if (b.freevars & ds.map(_.name).toSet).isEmpty => b
  }
  val ForceElim = Opt("force-elim") {
    case (ForceIn(Tuple(List()) in _, b, _), a) => b(Tuple())
    case (ForceIn((t@Tuple(args)) in _, b, _), a) if args.forall(_.isInstanceOf[Constant]) => b(t)
  }
  val ForceElimVar = Opt("force-elim-var") {
    case (ForceIn((t@Tuple(args)) in ctx, b, _), a) if a(t in ctx).nonFuture => b(t)
  }
  
  val InlineSpawn = OptFull("inline-spawn") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case SpawnIn((t: ClosureVariable) in ctx, k) => ctx(t) match {
        case LetBound(dctx, Let(d, _)) => (d in ctx) match {
          case ClosureDefIn(_, List(h), _, body) => {
            if(body.immediatelyCalls(h)) {
              import PorcInfixNotation._
              val kCl = new ClosureVariable("k")
              Some(let(kCl() === k) { t(kCl) })
            } else 
              None
          }
          case _ => None
        }
        case _ => None
      }
      case _ => None
    }
  }
  val ExternalSiteCall = Opt("external-sitecall") {
    case (SiteCallIn(Constant(s: OrcSite) in _, List(args, p, h), _), a) => 
      import PorcInfixNotation._ 
      val pp = new ClosureVariable("pp")
      val x = new Variable("x")
      val impl = let(pp(x) === ExternalCall(s, x, p, h)) {
          Force(args, pp, h)
      }
      if( s.effectFree )
        impl
      else
        IsKilled(h (), impl)
  }

  val UnpackElim = Opt("unpack-elim") {
      case (UnpackIn(vs, Tuple(as), k), a) => {
        import a.ImplicitResults._
        k.substAll((vs zip as).toMap)
      }
  }

  val opts = List(ExternalSiteCall, InlineSpawn, InlineLet, LetElim, InlineSite, SiteElim, ForceElim, ForceElimVar, EtaReduce, UnpackElim)

}