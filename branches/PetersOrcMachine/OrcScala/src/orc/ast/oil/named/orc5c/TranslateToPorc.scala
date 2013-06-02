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

/**
  *
  * @author amp
  */
object TranslateToPorc { 
  import PorcInfixNotation._
  case class TranslationContext(
      p: porc.Value,
      h: porc.Value,
      sites: Map[OrcSite, porc.SiteVariable] = Map(), 
      variables: Map[BoundVar, porc.Var] = Map(),
      recursives: Set[BoundVar] = Set()) {
    def +(v: (BoundVar, porc.Var)) = this.copy(variables = variables + v)
    def ++(s: Iterable[(BoundVar, porc.Var)]) = this.copy(variables = variables ++ s)
    def apply(v: BoundVar) = variables(v)

    //def +(s: OrcSite, v: porc.SiteVariable) = this.copy(sites = sites + (s -> v))
    //def ++(s: Iterable[(OrcSite, porc.SiteVariable)]) = this.copy(sites = sites ++ s)
    def apply(v: OrcSite) = sites(v)
    
    def isRecursive(v: BoundVar) = recursives(v)
    
    def addRecursives(vs: Iterable[BoundVar]) = this.copy(recursives = recursives ++ vs)
    
    def setP(v: Value) = copy(p=v)
    def setH(v: Value) = copy(h=v)
  }
  
  def orc5cToPorc(e: Expression): porc.Command = {
    val topP = new Variable(Some("Publish"))
    val topH = new Variable(Some("Halt"))
    
    val (sites, names, defs) = e.referencedSites.toList.map(wrapSite).unzip3
   
    val p = translate(e)(TranslationContext(topP, topH, sites = (sites zip names).toMap))
    defs.foldLeft(p)((p, s) => Site(List(s), p))
  }
  
  def wrapSite(s: OrcSite) = {
    val name = new SiteVariable(Some("_" + s.name))
    val args = new Variable("args")
    val p = new Variable("P")
    val h = new Variable("H")
    (s, name, SiteDef(name, List(args, p, h), {
      val pp = new ClosureVariable("pp")
      val x = new Variable("x")
      val impl = let(pp(x) === ExternalCall(s, x, p, h)) {
        Force(args, pp, h)
      }
      if(s.effectFree)
        impl
        else
          IsKilled(h(Tuple()), impl)
    }))
  }
  
  def translate(e: Expression)(implicit ctx: TranslationContext): porc.Command = {
    import ctx.{h, p}
    e ->> e match {
      case Stop() => ClosureCall(h, Tuple())
      case f || g => {
        val gCl = new ClosureVariable("g")
        val hInG = new Variable("h")
        let(gCl(hInG) === translate(g)(ctx setH hInG)) {
          spawn(gCl) {
            translate(f)
          }
        }
      }
      case f > x > g => {
        val p1Cl = new ClosureVariable("p'")
        val hInP1 = new Variable("h")
        val x1 = new Variable(x.optionalVariableName)
        let(p1Cl(x1, hInP1) === {
          val gCl = new ClosureVariable("g")
          val hInG = new Variable("h'")
          let(gCl(hInG) === (translate(g)(ctx + ((x, x1)) setH hInG))) {
            spawn(gCl) {
              ClosureCall(hInP1, Tuple())
            }
          }
        }) {
          translate(f)(ctx setP p1Cl)
        }
      }
      case f ow g => {
        val hasPublished = new Variable("hasPublished")
        flag(hasPublished) {
          NewCounter {
            val h1 = new ClosureVariable("h'")
            let(h1() === {
              restoreCounter {
                getCounterHalt { ch =>
                  readFlag(hasPublished) {
                    ch(Tuple())
                  } {
                    translate(g)(ctx setH ch)
                  }
                }
              }{
                Die()
              }
            }) {
              setCounterHalt(h1) {
                val p1 = new ClosureVariable("p'")
                val hlocal = new ClosureVariable("hlocal")
                val hInP1 = new Variable("h''")
                val x = new Variable("x")
               
                let(p1(x,hInP1) === {
                  setFlag(hasPublished) { p(Tuple(x, hInP1)) }
                },
                    hlocal() === { 
                  restoreCounter {
                    getCounterHalt { ch =>
                      readFlag(hasPublished) {
                        h(Tuple())
                      } {
                        translate(g)
                      }
                    }
                  }{
                    h(Tuple())
                  }
                }) {
                  translate(f)(ctx setP p1 setH hlocal)
                }
              }
            }
          }
        }
      }
      case f < x <| g => {
        val x1 = x ->> new Variable()
        future(x1) {
          val fCl = new ClosureVariable("f")
          val hInF = new Variable("h'")
          let(fCl(hInF) === {
            translate(f)(ctx + ((x, x1)) setH hInF)
          }) {
            spawn(fCl) {
              NewCounter {
                val h1 = new ClosureVariable("h''")
                let(h1() === {
                  restoreCounter {
                    stop(x1) {
                      getCounterHalt { ch =>
                        ch(Tuple())
                      }
                    }
                  } {
                    Die()
                  }
                }) {
                  setCounterHalt(h1) {
                    val p1 = new ClosureVariable("p'")
                    val hlocal = new ClosureVariable("hlocal")
                    val hInP1 = new Variable("h''")
                    val xv = new Variable("xv")

                    let(p1(xv, hInP1) === {
                      bind(x1, xv) { hInP1(Tuple()) }
                    },
                      hlocal() === {
                        restoreCounter {
                          stop(x1) { h(Tuple()) }
                        } {
                          h(Tuple())
                        }
                      }) {
                        translate(g)(ctx setP p1 setH hlocal)
                      }
                  }
                }
              }
            }
          }
        }
      }
      case Limit(f) => {
        getTerminator { t =>
          NewTerminator {
            val killHandler = new ClosureVariable("kH")
            let(killHandler() === {
              kill {
                CallKillHandlers {
                  Die()
                }
              } {
                Die()
              }
            }) {
              addKillHandler(t, killHandler) {
                val p1 = new ClosureVariable("p'")
                val hInP1 = new Variable("h'")
                val xv = new Variable("xv")
                let(p1(xv, hInP1) === {
                  kill {
                    CallKillHandlers {
                      p(Tuple(xv, hInP1))
                    }
                  } {
                    hInP1(Tuple())
                  }
                }) {
                  translate(f)(ctx setP p1)
                }
              }
            }
          }
        }
      }
      case Call(target : BoundVar, args, _) if ctx.isRecursive(target) => {
        IsKilled(h(Tuple()),
            argumentToPorc(target, ctx) sitecall Tuple(Tuple(args.map(argumentToPorc(_, ctx))) :: List(p, h))
        )
      }      
      case Call(target, args, _) => {
        argumentToPorc(target, ctx) sitecall Tuple(Tuple(args.map(argumentToPorc(_, ctx))) :: List(p, h))
      }
      case DeclareDefs(defs, body) => {
        val names = defs.map(_.name)
        val newnames = (for(name <- names) yield (name, new SiteVariable(name.optionalVariableName))).toMap
        val ctx1 : TranslationContext = ctx ++ newnames
        val sitedefs = for(Def(name, formals, body, _, _, _) <- defs) yield {
          val newformals = for(x <- formals) yield new Variable(x.optionalVariableName)
          val p1 = new Variable("P")
          val h1 = new Variable("H")
          val args = new Variable("args")
          SiteDef(newnames(name), 
              List(args, p1, h1),
              Unpack(newformals, args, translate(body)(ctx1 ++ (formals zip newformals) setP p1 setH h1 addRecursives names)))
        }
        Site(sitedefs, translate(body)(ctx1))
      }
      case DeclareType(_, _, b) => translate(b)
      case HasType(b, _) => translate(b)
      case v : Constant => {
        p(Tuple(argumentToPorc(v, ctx), h))
      }
      case v : BoundVar => {
        val pp = new ClosureVariable("pp")
        val x = new Variable("x")
        let(pp(x) === p(Tuple(x, h))) {
          Force(Tuple(ctx(v)), pp, h)
        }
      }
      case _ => throw new Error(s"Unable to handle expression $e")
    }
    
  }
  
  def argumentToPorc(v : Argument, ctx : TranslationContext) : Value = {
    v -> {
      case v : BoundVar => ctx(v)
      case Constant(s : OrcSite) => ctx(s)
      case Constant(c) => porc.Constant(c)
    }
  }
}