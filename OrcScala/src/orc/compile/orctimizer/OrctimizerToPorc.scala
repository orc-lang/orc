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

  def expression(expr: Expression)(implicit ctx: ConversionContext): porc.Expr = ??? 
  /*{
    import porc.PorcInfixNotation._
    val code = expr match {
      case Stop() => porc.Unit()
      case Call(target, args, typeargs) => {
        target match {
          case target: BoundVar if ctx.recursives contains target => {
            // For recusive functions spawn before calling and spawn before passing on the publication.
            // This provides trampolining.
            val newP = newVarName("P")
            val v = newVarName("temp")
            let((newP, porc.Continuation(v, porc.Spawn(ctx.c, ctx.t, ctx.p(v))))) {
              val call = porc.SiteCall(argument(target), newP, ctx.c, ctx.t, args.map(argument(_)))
              porc.Spawn(ctx.c, ctx.t, call)
            }
          }
          case _ => 
            porc.SiteCall(argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)))
        }
      }
      case left || right => {
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
      case Future(x, f, g) => {
        val fut = lookup(x)
        val newP = newVarName("P")
        val newC = newVarName("C")
        let((fut, porc.SpawnFuture(ctx.c, ctx.t, newP, newC, expression(f)(ctx.copy(p = newP, c = newC))))) {
          expression(g)
        }
      }
      case Force(f, forceClosures) => {
        porc.Force(ctx.p, ctx.c, argument(f), forceClosures)
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
      case DeclareDefs(defs, body) => {
        porc.Site(defs.map(orcdef(defs.map(_.name), _)), expression(body))
      }

      // We do not handle types
      case HasType(body, expectedType) => expression(body)
      case DeclareType(u, t, body) => expression(body)
      
      case VtimeZone(timeOrder, body) => 
        throw new FeatureNotSupportedException("Virtual time", expr.pos)
        
      case FieldAccess(o, f) => {
        porc.GetField(ctx.p, ctx.c, ctx.t, argument(o), f)
      }
      case a: Argument => {
        ctx.p(argument(a))
      }
      case _ => ???
    }
    code
  }*/
  
  def argument(a: Argument): porc.Value = {
    a match {
      case c@Constant(v) => porc.OrcValue(v)
      case (x: BoundVar) => lookup(x)
      case _ => ???
    }
  }
  
  def orcdef(recursiveGroup: Seq[BoundVar], d: Def)(implicit ctx: ConversionContext): porc.Def = {         
    val Def(f, formals, body, typeformals, argtypes, returntype) = d
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val args = formals.map(lookup)
    val name = lookup(f)
    porc.DefCPS(name, newP, newC, newT, args, expression(body)(ctx.copy(p = newP, c = newC, t = newT, recursives = ctx.recursives ++ recursiveGroup)))
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

