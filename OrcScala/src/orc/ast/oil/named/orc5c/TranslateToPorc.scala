//
// TranslateToPorc.scala -- Scala class/trait/object TranslateToPorc
// Project OrcScala
//
// $Id$
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

import orc.ast.porc
import orc.ast.porc._
import orc.values.sites.{Site => OrcSite}
import orc.values.sites.TotalSite1
import orc.values.Signal
import orc.values.Format
import orc.Handle
import orc.PublishedEvent
import orc.lib.builtin.MakeSite

/**
  *
  * @author amp
  */
object TranslateToPorc { 
  import PorcInfixNotation._
  
  // TODO: Use a varient of the TransformContext allowing us to know how variables where bound and translate to better code.
  
  case class SiteInfo(val arity: Int, val closedVariables: Set[porc.Var])
  
  case class TranslationContext(
      p: porc.Value,
      sites: Map[OrcSite, porc.Var] = Map(), 
      variables: Map[BoundVar, porc.Var] = Map(),
      siteInfo: Map[BoundVar, SiteInfo] = Map(),
      recursives: Set[BoundVar] = Set()) {
    def +(v: (BoundVar, porc.Var)) = this.copy(variables = variables + v)
    def ++(s: Iterable[(BoundVar, porc.Var)]) = this.copy(variables = variables ++ s)
    def apply(v: BoundVar) = variables(v)
    def site(v: BoundVar) = siteInfo.get(v)

    //def +(s: OrcSite, v: porc.SiteVariable) = this.copy(sites = sites + (s -> v))
    //def ++(s: Iterable[(OrcSite, porc.SiteVariable)]) = this.copy(sites = sites ++ s)
    def apply(v: OrcSite) = sites(v)
    
    def isRecursive(v: BoundVar) = recursives(v)
    
    def addRecursives(vs: Iterable[BoundVar]) = this.copy(recursives = recursives ++ vs)
    def addSiteInfo(vs: Iterable[(BoundVar, SiteInfo)]) = this.copy(siteInfo = siteInfo ++ vs)
    
    def setP(v: Value) = copy(p=v)
  }
  
  def orc5cToPorc(e: Expression): porc.Expr = {
    val topP = new porc.Var(Some("Publish"))
    val topH = new porc.Var(Some("Halt"))
    
    //val (sites, names, defs) = e.referencedSites.toList.sortBy(_.name).map(wrapSite).unzip3
    val (sites, names, defs) = porcImplementedSiteDefs.unzip3
    // TODO: Add support for direct call stubs.
    
    val p = translate(e)(TranslationContext(topP, sites = (sites zip names).toMap))
    val sp = defs.foldLeft(p)((p, s) => Site(List(s), p))
    
    val x = new porc.Var("x")

    val printSite = new orc.values.sites.Site {
      override val name = "PublishToUser"
        
      def call(args: List[AnyRef], h: Handle) {
        h.notifyOrc(PublishedEvent(args(0)))
        h.publish(Signal)
      }
      override val immediateHalt = true
      override val immediatePublish = true
      override val publications = (1, Some(1))
    }
    
    val noopP = new porc.Var("noop")

    let((topH, porc.Lambda(Nil, porc.Unit()))) {
      SetCounterHalt(topH) :::
      let((noopP, Lambda(List(x), Unit())),
          (topP, Lambda(List(x), ExternalCall(printSite, List(x), noopP)))) {
            sp
          }
    }
  }
  
  val porcImplementedSites = Map[OrcSite, (porc.Var) => (List[porc.Var], porc.Expr)](
    /*MakeSite -> { (args, p, h) =>
      import porc._
      val f = new Var("f")
      val f1 = new Var("f'")
      val args1 = new Var("args")
      val p1 = new Var("P")
      val h1 = new Var("H")
      val topH = new Var("Halt")
      Unpack(List(f), args,
        Site(List(SiteDef(f1, List(args1, p1, h1),
          NewTerminator {
            NewCounterDisconnected {
              let(topH() === restoreCounter {
                    h1()
                  } {
                    Die()
                  }) {
                setCounterHalt(topH) {
                  f sitecall (args1, p1, topH)
                }
              }
            }
          })),
          p(f1, h)))
    }*/)
    
  val porcImplementedSiteDefs = {
    for ((s, b) <- porcImplementedSites) yield {
      import porc._
      val name = new Var(Some("_" + s.name))
      val p = new Var("P")
      val (args,body) = porcImplementedSites.get(s) match {
        case Some(b) => b(p)
      }
      (s, name, SiteDef(name, args, p, body))
    }
  }.toSeq
  
  /*def wrapSite(s: OrcSite) = {
    val name = new SiteVariable(Some("_" + s.name))
    val args = new Variable("args")
    val p = new Variable("P")
    val h = new Variable("H")
    val body = porcImplementedSites.get(s) match {
      case Some(b) => b(args, p, h)
      case None => {
        val pp = new ClosureVariable("pp")
        val x = new Variable("x")
        val impl = let(pp(x) === ExternalCall(s, x, p, h)) {
          Force(args, pp, h)
        }
        if (s.effectFree)
          impl
        else
          IsKilled(h(), impl)
      }
    }
    (s, name, SiteDef(name, List(args, p, h), body))
  }*/
  
  def translate(e: Expression)(implicit ctx: TranslationContext): porc.Expr = {
    import ctx.{p => P}
    e -> {
      case Stop() => Unit()
      case f || g => {
        val gCl = new porc.Var("g")
        let((gCl, Lambda(List(), translate(g)(ctx)))) {
          Spawn(gCl) :::
          translate(f)
        }
      }
      case f > x > g => {
        val p1Cl = new porc.Var("p'")
        val x1 = x ->> new porc.Var()
        let((p1Cl, lambda(x1){ translate(g)(ctx + (x, x1)) })) {
          translate(f)(ctx setP p1Cl)
        }
      }
      case f ow g => {
        val hasPublished = new porc.Var("hasPublished")
        let((hasPublished, NewFlag())) {
          NewCounter {
            val h1 = new porc.Var("h'")
            val b = new porc.Var("b")
            let((h1, lambda(){
              restoreCounter {
                let((b, ReadFlag(hasPublished))) {
                  If(b, Unit(), translate(g))
                } :::
                CallCounterHalt()
              }{
                Unit()
              }
            })) {
              val p1 = new porc.Var("p'")
              val x = new porc.Var("x")
              SetCounterHalt(h1) :::
              let((p1, lambda(x) {
                SetFlag(hasPublished) ::: P(x)
              })) {
                  TryOnKilled({
                    translate(f)(ctx setP p1)
                  }, {
                    restoreCounter {
                      DecrCounter()
                    } {
                      Unit()
                    } :::
                    Killed()
                  }) :::
                  restoreCounter {
                    DecrCounter() :::
                      let((b, ReadFlag(hasPublished))) {
                        If(b, Unit(), translate(g))
                      }
                  } {
                    Unit()
                  }
              }
            }
          }
        }
      }
      case f < x <| g => {
        val x1 = x ->> new porc.Var()
        let((x1, NewFuture())) {
          val fCl = new porc.Var("f")
          let((fCl, lambda() {
            translate(f)(ctx + ((x, x1)))
          })) {
            Spawn(fCl) :::
            NewCounter {
              val h1 = new porc.Var("h''")
              let((h1, lambda() {
                restoreCounter {
                  porc.Stop(x1) :::
                  CallCounterHalt()
                } {
                  Unit()
                }
              })) {
                val p1 = new porc.Var("p'")
                val xv = new porc.Var("xv")
                SetCounterHalt(h1) :::
                let((p1, lambda(xv) {
                  Bind(x1, xv)
                })) {
                 TryOnKilled({
                  translate(g)(ctx setP p1)
                 }, {
                  restoreCounter {
                    DecrCounter() :::
                    porc.Stop(x1)
                  } {
                    Unit()
                  } :::
                  Killed()
                 }) :::
                  restoreCounter {
                    DecrCounter() :::
                    porc.Stop(x1)
                  } {
                    Unit()
                  }
                }
              }
            }
          }
        }
      }
      case Limit(f) => {
        val t = new porc.Var("T")
        let((t, GetTerminator())) {
          NewTerminator {
            val killHandler = new porc.Var("kH")
            val b = new porc.Var("b")
            let((killHandler, lambda() {
              let((b, SetKill())) {
                If(b,
                  CallKillHandlers(),
                  Unit())
              }
            })) {
              val p1 = new porc.Var("p'")
              val xv = new porc.Var("xv")
              AddKillHandler(t, killHandler) :::
              let((p1, lambda(xv) {
                let((b, SetKill())) {
                  If(b,
                      CallKillHandlers() ::: P(xv) ::: Killed(),
                      Killed())
                }
              })) {
                TryOnKilled(translate(f)(ctx setP p1), Unit())
              }
            }
          }
        }
      }
      case Call(target : BoundVar, args, _) if ctx.isRecursive(target) => {
        val g = new porc.Var("g")
        CheckKilled() :::
        let((g, lambda() { SiteCall(argumentToPorc(target, ctx), args.map(argumentToPorc(_, ctx)), P) })) {
          Spawn(g)
        }
      }      
      case Call(target : BoundVar, args, _) => {
        // TODO: Is this correct? This seems to force the target, but not force anything bound in it's closure.
        val pp = new porc.Var("pp")
        val x = new porc.Var("x")
        val x1 = new porc.Var("x'")
        let((pp, lambda(x) { SiteCall(x, args.map(argumentToPorc(_, ctx)), P) })) {
          Force(List(ctx(target)), pp)
        } 
      }
      case Call(c@Constant(_), args, _) => {
        SiteCall(argumentToPorc(c, ctx), args.map(argumentToPorc(_, ctx)), P)
      }
      
      // Handle "magic" sites.
      /*case Call(Constant(MakeStrict), List(f : BoundVar), _) if (ctx site f).isDefined  => {
        val Some(SiteInfo(arity, _)) = ctx site f
        
        val fStrict = new porc.Var(f.optionalVariableName map (_ ++ "Strict"))
        val formals = for(i <- 0 to arity) yield new porc.Var(s"x$i_")
        val formals2 = for(i <- 0 to arity) yield new porc.Var(s"y$i_")
        val p1 = new porc.Var("P")
        val ff = new porc.Var("ff")
        Site(SiteDef(fStrict, formals, p1, {
          let((ff, lambda(formals2: _*)(SiteCall(f, formals2, p1)))) {
            Force(formals, ff)
          }
        }), {
          P(fStrict)
        })
      }
      case Call(Constant(MakeSingleValued), List(f : BoundVar), _) if (ctx site f).isDefined  => {
        val Some(SiteInfo(arity, _)) = ctx site f
        val formals = for(i <- 0 to arity) yield new BoundVar(Some(s"x$i"))
        val fSingle = new BoundVar()
        translate(DeclareDefs(List(Def(fSingle, formals, Limit(Call(f, formals, None)), None, None)), f))
      }
      case Call(Constant(MakeResilient), List(f : BoundVar), _) if (ctx site f).isDefined  => {
        val Some(SiteInfo(arity, _)) = ctx site f
        
        val fResilient = new porc.Var(f.optionalVariableName map (_ ++ "Resilient"))
        val formals = for(i <- 0 to arity) yield new porc.Var(s"x$i_")
        val p1 = new porc.Var("P")
        val ff = new porc.Var("ff")
        val t = new porc.Var("T")
        Site(SiteDef(fResilient, formals, p1, {
          let((t, lambda(formals2: _*)(SiteCall(f, formals2, p1)))) {
            Force(formals, ff)
          }
        }), {
          P(fResilient)
        })
      }*/
      
      case Call(target, args, _) => {
        SiteCall(argumentToPorc(target, ctx), args.map(argumentToPorc(_, ctx)), P)
      }
      case DeclareDefs(defs, body) => {
        val names = defs.map(_.name)
        val newnames = (for(name <- names) yield (name, name ->> new porc.Var())).toMap
        val closedVariables = (for(Def(_, formals, body, _, _, _) <- defs) yield {
          body.freevars -- formals
        }).flatten.toSet -- names
        val closedPorcVars = closedVariables.map(ctx(_))
        val ctx1 : TranslationContext = (ctx ++ newnames).addSiteInfo(defs map {d => (d.name, SiteInfo(d.formals.size, closedPorcVars))})
        val ctxdefs = ctx1 addRecursives names
        val sitedefs = for(Def(name, formals, body, _, _, _) <- defs) yield {
          val newformals = for(x <- formals) yield x ->> new porc.Var()
          val p1 = new porc.Var("P")
          SiteDef(newnames(name), 
              newformals, p1,
             translate(body)(ctxdefs ++ (formals zip newformals) setP p1))
        }
        Site(sitedefs, translate(body)(ctx1))
      }
      case DeclareType(_, _, b) => translate(b)
      case HasType(b, _) => translate(b)
      case v : Constant => {
        P(argumentToPorc(v, ctx))
      }
      case v : BoundVar if (ctx site v).isEmpty => {
        //val pp = new porc.Var("pp")
        //val x = new porc.Var("x")
        //let((pp, lambda(x) { P(x) })) {
        Force(List(ctx(v)), P)
        //} 
      }
      case v : BoundVar if (ctx site v).isDefined => {
        val Some(SiteInfo(_, cvs)) = ctx site v
        val pp = new porc.Var("pp")
        val x = new porc.Var("x")
        let((pp, lambda(x :: (cvs.toList map {v => new porc.Var()}) : _*) { P(x) })) {
          Force(ctx(v) :: cvs.toList, pp)
        }
      }
      case _ => throw new Error(s"Unable to handle expression $e")
    }
    
  }
  
  def argumentToPorc(v : Argument, ctx : TranslationContext) : Value = {
    v -> {
      case v : BoundVar => ctx(v)
      case Constant(s : OrcSite) if ctx.sites.contains(s) => ctx(s)
      case Constant(c) => porc.OrcValue(c)
    }
  }
}