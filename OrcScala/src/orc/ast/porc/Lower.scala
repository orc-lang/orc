//
// Lower.scala -- Scala object Lower
// Project PorcE
//
// Created by amp on Oct, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.porc

import orc.ast.hasOptionalVariableName.VariableNameInterpolator
import orc.lib.state.NewGate

object Lower {
  case class ConversionContext(p: Argument, c: Argument, t: Argument) {
  }

  def Variable(s: String) = new Variable(Some(s))

  /** Catch porc exceptions and halt the current C.
    */
  def catchExceptions(e: Expression)(implicit ctx: ConversionContext): Expression = {
    TryOnException({
      e
    }, {
      HaltToken(ctx.c)
    })
  }

  /** Run expression f to bind future fut.
    *
    * This uses the current counter and terminator, but does not publish any value.
    */
  def buildSlowFuture(vClosure: Argument)(implicit ctx: ConversionContext): Expression = {
    import orc.ast.porc.PorcInfixNotation._

    val fut = Variable(id"fut_$vClosure~")

    val comp = Variable(id"comp_$vClosure~")
    val v = Variable(id"v_$vClosure~")
    val cr = Variable(id"cr_$vClosure~")
    val newP = Variable(id"P_$vClosure~")
    val newC = Variable(id"C_$vClosure~")

    let(
      (fut, NewFuture(false)),
      (comp, Continuation(Seq(), {
        val crImpl = Continuation(Seq(), {
          BindStop(fut) :::
            HaltToken(ctx.c)
        })
        let(
          (cr, crImpl),
          (newC, NewSimpleCounter(ctx.c, cr)),
          (newP, Continuation(Seq(v), {
            Bind(fut, v) :::
              HaltToken(newC)
          }))) {
            vClosure(newP, newC)
          }
      }))) {
        NewToken(ctx.c) :::
          catchExceptions {
            // TODO: The `false` could actually cause semantic problems in the case of sites which block the calling thread. Metadata is probably needed.
            Spawn(ctx.c, ctx.t, false, comp)
          } :::
          ctx.p(fut)
      }
  }

  /** Run expression f and call ctx.p when it publishes.
    *
    * This sill handle future-like semantics (only one call to P) but will
    * not actually run anything in parallel. P will be the P for f.
    */
  def buildNoFuture(vClosure: Argument)(implicit ctx: ConversionContext): Expression = {
    import orc.ast.porc.PorcInfixNotation._

    val gate = Variable(id"gate_$vClosure~")
    val v = Variable(id"v_$vClosure~")
    val cr = Variable(id"cr_$vClosure~")
    val newP = Variable(id"P_$vClosure~")
    val newC = Variable(id"C_$vClosure~")

    val crImpl = Continuation(Seq(), {
      val fut = Variable(id"fut_$vClosure~")
      catchExceptions {
        MethodDirectCall(true, gate, Nil) :::
        let((fut, NewFuture(true))) {
          BindStop(fut) :::
          ctx.p(fut) :::
          HaltToken(ctx.c)
        }
      }
    })

    let(
      (gate, newGate()),
      (cr, crImpl),
      (newC, NewSimpleCounter(ctx.c, cr)),
      (newP, Continuation(Seq(v), {
        catchExceptions {
          MethodDirectCall(true, gate, Nil) :::
          ctx.p(v)
        }
      }))) {
        vClosure(newP, newC)
      }
  }

  /** Lower expr based on .
    *
    */
  def apply(parallelGraft: Boolean = true)(expr: Expression): Expression = {
    val code = expr match {
      case Graft(p, c, t, vClosure) => {
        implicit val ctx = ConversionContext(p, c, t)
        if (parallelGraft)
          buildSlowFuture(vClosure)
        else
          buildNoFuture(vClosure)
      }

      case _ =>
        throw new IllegalArgumentException(s"Attempted to lower a node which does not require lowering: ${expr}")
    }
    expr ->> code
  }

  private def newGate(): Expression = {
    MethodDirectCall(true, Constant(NewGate), List())
  }
}
