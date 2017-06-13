//
// OrctimizerToPorc.scala -- Scala class OrctimizerToPorc and related
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field
import orc.ast.porc
import orc.error.compiletime.FeatureNotSupportedException
import orc.ast.porc.TryOnHalted
import orc.ast.porc.SiteCallDirect
import orc.lib.state.NewFlag
import orc.lib.state.PublishIfNotSet
import orc.lib.state.SetFlag
import orc.ast.porc.SetDiscorporate

case class ConversionContext(p: porc.Var, c: porc.Var, t: porc.Var, recursives: Set[BoundVar]) {
}

/** @author amp
  */
class OrctimizerToPorc {
  def apply(prog: Expression): porc.DefCPS = {
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val body = expression(prog)(ConversionContext(p = newP, c = newC, t = newT, recursives = Set()))
    porc.DefCPS(newVarName("Prog"), newP, newC, newT, Nil, body)
  }

  val vars: mutable.Map[BoundVar, porc.Var] = new mutable.HashMap()
  val usedNames: mutable.Set[String] = new mutable.HashSet()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): porc.Var = {
    val name = if (usedNames contains prefix) {
      varCounter += 1
      prefix + "_" + varCounter
    } else prefix
    usedNames += name
    new porc.Var(Some(name))
  }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))

  def expression(expr: Expression)(implicit ctx: ConversionContext): porc.Expr = {
    import porc.PorcInfixNotation._
    val code = expr match {
      case Stop() => porc.Unit()
      case c @ Call(target, args, typeargs) => {
        val porcCall = c match {
          case _: CallSite => porc.SiteCall
          case _: CallDef => porc.DefCall
        }
        // TODO: Spawning on publication is a big issue since it results in O(n^2) spawns during stack
        //       unrolling. Can we avoid the need for the spawn in P.

        // TODO: spawning for every call is overkill. Some will be optimized away. But it's still an issue.
        c match {
          case _: CallDef => {
            // For possibly recusive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = newVarName("P")
            val v = newVarName("temp")
            let((newP, porc.Continuation(v, porc.Spawn(ctx.c, ctx.t, ctx.p(v))))) {
              val call = porcCall(argument(target), newP, ctx.c, ctx.t, args.map(argument(_)))
              porc.Spawn(ctx.c, ctx.t, call)
            }
          }
          case _: CallSite => {
            porcCall(argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)))
          }
        }
      }
      case left Parallel right => {
        // TODO: While it sound to never add a spawn here it might be good to add them sometimes.
        expression(left) :::
          expression(right)
      }
      case Branch(left, x, right) => {
        val newP = newVarName("P")
        val v = lookup(x)
        let((newP, porc.Continuation(v, expression(right)))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Trim(f) => {
        val newT = newVarName("T")
        val newP = newVarName("P")
        val v = newVarName()
        let((newT, porc.NewTerminator(ctx.t)),
          (newP, porc.Continuation(v, porc.Kill(newT) ::: ctx.p(v)))) {
            porc.TryOnKilled(expression(f)(ctx.copy(t = newT, p = newP)), porc.Unit())
          }
      }
      case Future(f) => {
        val fut = newVarName("v")
        val newP = newVarName("P")
        val newC = newVarName("C")
        let((fut, porc.NewFuture())) {
          porc.SpawnBindFuture(fut, ctx.c, ctx.t, newP, newC, expression(f)(ctx.copy(p = newP, c = newC))) :::
            ctx.p(fut)
        }
      }
      case Force(xs, vs, forceClosures, e) => {
        val porcXs = xs.map(lookup)
        val newP = newVarName("P")
        val v = newVarName("temp")
        val body = let(porcXs.zipWithIndex map { case (x, i) => (x, porc.TupleElem(v, i)) }: _*) { expression(e) }
        let((newP, porc.Continuation(v, body))) {
          porc.Force(newP, ctx.c, ctx.t, forceClosures, vs.map(argument))
        }
      }
      case left Otherwise right => {
        val newC = newVarName("C")
        val flag = newVarName("flag")

        val cl = {
          val newP = newVarName("P")
          val v = newVarName()
          let((newP, porc.Continuation(v, setFlag(flag) ::: ctx.p(v)))) {
            expression(left)(ctx.copy(p = newP, c = newC))
          }
        }
        val cr = {
          TryOnHalted({
            publishIfNotSet(flag) :::
              expression(right)
          }, porc.Unit())
        }

        let((flag, newFlag())) {
          let((newC, porc.NewCounter(ctx.c, cr))) {
            porc.TryFinally(cl, porc.Halt(newC))
          }
        }
      }
      case IfDef(a, f, g) => {
        porc.IfDef(argument(a), expression(f), expression(g))
      }
      case DeclareCallables(defs, body) => {
        val b = if (defs.exists(_.isInstanceOf[Site]))
          SetDiscorporate(ctx.c) ::: expression(body)
        else
          expression(body)
        porc.DefDeclaration(defs.map(callable(defs.map(_.name), _)), b)
      }

      case New(self, _, bindings, _) => {
        val selfV = lookup(self)

        val fieldInfos = for ((f, b) <- bindings) yield {
          val varName = newVarName(f.field)
          val (value, binder) = b match {
            case FieldFuture(e) =>
              val newP = newVarName("P")
              val newC = newVarName("C")
              val binder = porc.SpawnBindFuture(varName, ctx.c, ctx.t, newP, newC, expression(e)(ctx.copy(p = newP, c = newC)))
              (porc.NewFuture(), Some(binder))
            case FieldArgument(a) =>
              (argument(a), None)
          }
          ((varName, value), (f, varName), binder)
        }
        val (fieldVars, fields, binders) = {
          val (fvs, fs, bs) = fieldInfos.unzip3
          (fvs.toSeq, fs.toMap, bs.flatten)
        }

        let(fieldVars :+ (selfV, porc.New(fields)): _*) {
          binders.foldRight(ctx.p(selfV): porc.Expr)((a, b) => porc.Sequence(Seq(a, b)))
        }
      }

      // We do not handle types
      case HasType(body, expectedType) => expression(body)
      case DeclareType(u, t, body) => expression(body)

      case FieldAccess(o, f) => {
        porc.GetField(ctx.p, ctx.c, ctx.t, argument(o), f)
      }
      case a: Argument => {
        ctx.p(argument(a))
      }
      // case e => throw new NotImplementedError("orctimizerToPorc is not implemented for: " + e)
    }
    code
  }

  def argument(a: Argument): porc.Value = {
    a match {
      case c @ Constant(v) => porc.OrcValue(v)
      case (x: BoundVar) => lookup(x)
      //case _ => ???
    }
  }

  def callable(recursiveGroup: Seq[BoundVar], d: Callable)(implicit ctx: ConversionContext): porc.Def = {
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val Callable(f, formals, body, _, _, _) = d
    val args = formals.map(lookup)
    val name = lookup(f)
    val bodyT = d match {
      case Def(_, _, body, typeformals, argtypes, returntype) =>
        newT
      case Site(_, _, body, typeformals, argtypes, returntype) =>
        ctx.t
    }
    porc.DefCPS(name, newP, newC, newT, args,
      expression(body)(ctx.copy(p = newP, c = newC, t = bodyT, recursives = ctx.recursives ++ recursiveGroup)))
  }

  private def newFlag(): SiteCallDirect = {
    SiteCallDirect(porc.OrcValue(NewFlag), List())
  }
  private def setFlag(flag: porc.Var): SiteCallDirect = {
    SiteCallDirect(porc.OrcValue(SetFlag), List(flag))
  }
  private def publishIfNotSet(flag: porc.Var): SiteCallDirect = {
    SiteCallDirect(porc.OrcValue(PublishIfNotSet), List(flag))
  }

}
