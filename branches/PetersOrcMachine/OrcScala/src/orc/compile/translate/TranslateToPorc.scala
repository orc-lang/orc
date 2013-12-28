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
package orc.compile.translate

import orc.ast.porc
import orc.ast.porc._
import orc.values.sites.{ Site => OrcSite }
import orc.values.sites.TotalSite1
import orc.values.Signal
import orc.values.Format
import orc.Handle
import orc.PublishedEvent
import orc.ast.oil.named._
import orc.ast.oil.named

import java.util.concurrent.atomic.AtomicBoolean

/** @author amp
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

    def setP(v: Value) = copy(p = v)
  }

  def orc5cToPorc(e: Expression): porc.Expr = {
    val topP = new porc.Var(Some("Publish"))
    val topH = new porc.Var(Some("Halt"))

    //val (sites, names, defs) = e.referencedSites.toList.sortBy(_.name).map(wrapSite).unzip3
    val (sites, names, defs) = porcImplementedSiteDefs.unzip3

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

    let((topH, porc.Lambda(Nil, porc.RestoreCounter(porc.Unit(), porc.Unit())))) {
      SetCounterHalt(topH) :::
        let((noopP, Lambda(List(x), Unit())),
          (topP, Lambda(List(x), ExternalCall(printSite, List(x), noopP)))) {
            TryOnKilled(sp, Unit()) :::
              topH()
          }
    }
  }

  /*
  def makeMakeResilient(n: Int, p: porc.Var) = {
    import porc._

    val f = new Var("f")
    val f1 = new Var("f'")
    val args1 = 0 until n map (i => new Var(s"x${i}_")) toList
    val p1 = new Var("P")

    //site MakeResilient (f) P =
    val code =
      //  site fRes (y1, ..., yn) P =
      Site(List(SiteDef(f1, args1, p1, {
        //let t = T in
        val t = new porc.Var("T")
        let((t, GetTerminator())) {
          // terminator in
          NewTerminator {
            //let reportedHalted = newFlag in
            //let kH = lambda (). callCounterHalt in
            val kH = new Var("kH")
            let((kH, lambda()(CallCounterHalt()))) {
              //addKillHandler t kH
              AddKillHandler(t, kH) :::
                //counter in
                NewCounter {
                  //let h' = lambda (). restoreCounter 
                  //         (if (isKilled t) unit callCounterHalt)
                  //         (unit) in
                  val h1 = new Var("h'")
                  let((h1, lambda() {
                    restoreCounter {
                      val tmp = new Var()
                      let((tmp, IsKilled(t))) {
                        If(tmp, Unit(), CallCounterHalt())
                      }
                    } {
                      Unit()
                    }
                  })) {
                    val caughtKilled = new Var("caughtKilled")
                    val p2 = new Var("p2_")
                    val x = new Var("x")
                    SetCounterHalt(h1) :::
                      let((caughtKilled, NewFlag()),
                        //    let p' = lambda (x).
                        //               try P(x) onKilled (setFlag caughtKilled; unit) in
                        (p2, lambda(x) {
                          TryOnKilled({
                            p1(x)
                          }, {
                            SetFlag(caughtKilled) :::
                              Unit()
                          })
                        })) {
                          val b = new Var("b")
                          // sitecall f (y1, ..., yn) p';
                          (f sitecall (p2, args1: _*)) :::
                            // let b = readFlag caughtKilled in
                            let((b, ReadFlag(caughtKilled))) {
                              If(b, Killed(), Unit()) :::
                                restoreCounter {
                                  DecrCounter()
                                } {
                                  Unit()
                                }
                            }
                        }
                  }
                }
            }

          }
        }
      })),
        //  P (fRes)  
        p(f1))
    (List(f), code)
  }
  */

  val porcImplementedSites: Map[OrcSite, (porc.Var) => (List[porc.Var], porc.Expr)] = Map()
  /*++ {
    MakeResilients.sites.zipWithIndex.map({ pair => val (s, i) = pair
      s -> ((p : porc.Var) => makeMakeResilient(i, p) )
    }).toMap
  }*/

  val porcImplementedSiteDefs = {
    for ((s, b) <- porcImplementedSites) yield {
      import porc._
      val name = new Var(Some("_" + s.name))
      val p = new Var("P")
      val (args, body) = porcImplementedSites.get(s) match {
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
    import ctx.{ p => P }
    e -> {
      case named.Stop() => Unit()
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
        let((p1Cl, lambda(x1) { translate(g)(ctx + (x, x1)) })) {
          translate(f)(ctx setP p1Cl)
        }
      }
      case f ow g => {
        val hasPublished = new porc.Var("hasPublished")
        let((hasPublished, NewFlag())) {
          NewCounter {
            val h1 = new porc.Var("h'")
            val b = new porc.Var("b")
            let((h1, lambda() {
              restoreCounter {
                TryOnKilled(
                  let((b, ReadFlag(hasPublished))) {
                    If(b, Unit(), translate(g))
                  },
                  CallCounterHalt() :::
                    Killed()) :::
                  CallCounterHalt()
              } {
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
                        h1() :::
                          Killed()
                      }) :::
                        h1()
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
              Kill(Unit(), Unit())
            })) {
              val p1 = new porc.Var("p'")
              val xv = new porc.Var("xv")
              AddKillHandler(t, killHandler) :::
                let((p1, lambda(xv) {
                  Kill({
                    P(xv)
                  }, {
                    Killed()
                  })
                })) {
                  TryOnKilled(translate(f)(ctx setP p1), Unit())
                }
            }
          }
        }
      }
      case named.Call(target: BoundVar, args, _) if ctx.isRecursive(target) => {
        val g = new porc.Var("g")
        CheckKilled() :::
          let((g, lambda() { SiteCall(argumentToPorc(target, ctx), args.map(argumentToPorc(_, ctx)), P) })) {
            Spawn(g)
          }
      }
      case named.Call(target: BoundVar, args, _) => {
        // TODO: Is this correct? This seems to force the target, but not force anything bound in it's closure.
        val pp = new porc.Var("pp")
        val x = new porc.Var("x")
        val x1 = new porc.Var("x'")
        let((pp, lambda(x) { SiteCall(x, args.map(argumentToPorc(_, ctx)), P) })) {
          Force(List(ctx(target)), pp)
        }
      }
      case named.Call(c @ Constant(_), args, _) => {
        SiteCall(argumentToPorc(c, ctx), args.map(argumentToPorc(_, ctx)), P)
      }

      case Resilient(body) => {
        //let t = T in
        val t = new porc.Var("T")
        let((t, GetTerminator())) {
          // terminator in
          NewTerminator {
            //let kH = lambda (). callCounterHalt in
            val kH = new porc.Var("kH")
            val enclosingHalted = new porc.Var("enclosingHalted")
            // Called when surrounding scope is killed. Called even if encosed scope has already halted.
            let((enclosingHalted, DirectSiteCall(OrcValue(newAtomicFlagSite), List())),
              (kH, lambda() {
                val tmp = new porc.Var()
                let((tmp, DirectSiteCall(OrcValue(setAtomicFlagSite), List(enclosingHalted)))) {
                  // Called when enclosed scope halts. Halts if enclosing scope has not been killed.
                  If(tmp, CallCounterHalt(), Unit())
                }
              })) {
                //addKillHandler t kH
                AddKillHandler(t, kH) :::
                  //counter in
                  NewCounter {
                    //let h' = lambda (). restoreCounter 
                    //         (if (isKilled t) unit callCounterHalt)
                    //         (unit) in
                    val h1 = new porc.Var("h'")
                    MakeCounterTopLevel() :::
                    let((h1, lambda() {
                      restoreCounter {
                        val tmp = new porc.Var()
                        let((tmp, DirectSiteCall(OrcValue(setAtomicFlagSite), List(enclosingHalted)))) {
                          // Called when enclosed scope halts. Halts if enclosing scope has not been killed.
                          If(tmp, CallCounterHalt(), Unit())
                        }
                      } {
                        Unit()
                      }
                    })) {
                      val caughtKilled = new porc.Var("caughtKilled")
                      val p2 = new porc.Var("p2_")
                      val x = new porc.Var("x")
                      SetCounterHalt(h1) :::
                        let((caughtKilled, NewFlag()),
                          //    let p' = lambda (x).
                          //               try P(x) onKilled (setFlag caughtKilled; unit) in
                          (p2, lambda(x) {
                            TryOnKilled({
                              P(x)
                            }, {
                              SetFlag(caughtKilled) :::
                                Unit()
                            })
                          })) {
                            val b = new porc.Var("b")
                            // body;
                            translate(body)(ctx setP p2) :::
                              // let b = readFlag caughtKilled in
                              let((b, ReadFlag(caughtKilled))) {
                                h1() :::
                                  If(b, Killed(), Unit())
                              }
                          }
                    }
                  }
              }
          }
        }
      }

      case named.Call(target, args, _) => {
        SiteCall(argumentToPorc(target, ctx), args.map(argumentToPorc(_, ctx)), P)
      }
      case DeclareDefs(defs, body) => {
        val names = defs.map(_.name)
        val newnames = (for (name <- names) yield (name, name ->> new porc.Var())).toMap
        val closedVariables = (for (Def(_, formals, body, _, _, _) <- defs) yield {
          body.freevars -- formals
        }).flatten.toSet -- names
        val closedPorcVars = closedVariables.map(ctx(_))
        val ctx1: TranslationContext = (ctx ++ newnames).addSiteInfo(defs map { d => (d.name, SiteInfo(d.formals.size, closedPorcVars)) })
        val ctxdefs = ctx1 addRecursives names
        val sitedefs = for (Def(name, formals, body, _, _, _) <- defs) yield {
          val newformals = for (x <- formals) yield x ->> new porc.Var()
          val p1 = new porc.Var("P")
          SiteDef(newnames(name),
            newformals, p1,
            translate(body)(ctxdefs ++ (formals zip newformals) setP p1))
        }
        Site(sitedefs, translate(body)(ctx1))
      }
      case DeclareType(_, _, b) => translate(b)
      case HasType(b, _) => translate(b)
      case v: Constant => {
        P(argumentToPorc(v, ctx))
      }
      case v: BoundVar if (ctx site v).isEmpty => {
        //val pp = new porc.Var("pp")
        //val x = new porc.Var("x")
        //let((pp, lambda(x) { P(x) })) {
        Force(List(ctx(v)), P)
        //} 
      }
      case v: BoundVar if (ctx site v).isDefined => {
        val Some(SiteInfo(_, cvs)) = ctx site v

        cvs.foldLeft[Expr](P(ctx(v))) { (k, v) =>
          val resolved = new porc.Var("resolved")
          val x = new porc.Var("x")
          let((resolved, lambda() { k })) {
            Resolve(v, resolved)
          }
        }
      }

      case VtimeZone(_, _) => throw new IllegalArgumentException("The Porc compiler does not support VTime. (Yet)")

      case _ => throw new Error(s"Unable to handle expression $e")
    }

  }

  def argumentToPorc(v: Argument, ctx: TranslationContext): Value = {
    v -> {
      case v: BoundVar => ctx(v)
      case Constant(s: OrcSite) if ctx.sites.contains(s) => ctx(s)
      case Constant(c) => porc.OrcValue(c)
    }
  }

  // XXX: It might be better to lift these out to the top level as user level sites.
  // Creating a new one way flag. It is set once and never cleared.
  val newAtomicFlagSite = new orc.values.sites.TotalSite with orc.values.sites.DirectTotalSite {
    override val name = "NewAtomicFlag"
    def evaluate(args: List[AnyRef]) = {
      new AtomicBoolean(false)
    }
    override val immediateHalt = true
    override val immediatePublish = true
    override val publications = (1, Some(1))
  }
  // Returns true if you are the first to set it
  val setAtomicFlagSite = new orc.values.sites.TotalSite with orc.values.sites.DirectTotalSite {
    override val name = "SetAtomicFlag"
    def evaluate(args: List[AnyRef]) = {
      val a = args.head.asInstanceOf[AtomicBoolean]
      !a.getAndSet(true): java.lang.Boolean
    }
    override val immediateHalt = true
    override val immediatePublish = true
    override val publications = (1, Some(1))
  }
}