package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field
import orc.ast.porc
import orc.error.compiletime.CompilationException
import orc.error.compiletime.SeverityFatal

case class ConversionContext(p: porc.Var, c: porc.Var, t: porc.Var, recursives: Set[BoundVar]) {
}

case class FeatureNotSupportedException(feature: String, ast: NamedAST) extends CompilationException(s"$feature is unsupported") with SeverityFatal {
  setPosition(ast.pos)
}

/** @author amp
  */
class OrctimizerToPorc {
  def apply(prog: Expression): porc.SiteDefCPS = {
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val body = expression(prog)(ConversionContext(p = newP, c = newC, t = newT, recursives = Set()))
    porc.SiteDefCPS(newVarName("Prog"), newP, newC, newT, Nil, body)
  }
  
  val vars: mutable.Map[BoundVar, porc.Var] = new mutable.HashMap()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): porc.Var = {
    varCounter += 1
    new porc.Var(Some(prefix + "_" + varCounter))
  }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))

  def expression(expr: Expression)(implicit ctx: ConversionContext): porc.Expr = {
    import porc.PorcInfixNotation._
    val code = expr match {
      case Stop() => porc.Unit()
      case Call(target, args, typeargs) => {
        val call = porc.SiteCall(argument(target), ctx.p, ctx.c, ctx.t, args.map(argument(_)))
        porc.Spawn(ctx.c, ctx.t, call)
      }
      case left || right => {
        porc.Spawn(ctx.c, ctx.t, expression(left)) :::
          expression(right)
      }
      case Sequence(left, x, right) => {
        val newP = newVarName("P")
        val v = lookup(x)
        let((newP, porc.Continuation(v, expression(right)))) {
          expression(left)(ctx.copy(p = newP))
        }
      }
      case Limit(f) => {
        val newT = newVarName("T")
        val newP = newVarName("P")
        val v = newVarName()
        let((newT, porc.NewTerminator(ctx.t)),
            (newP, porc.Continuation(v, porc.Kill(newT) ::: ctx.p(v)))) {
          porc.TryOnKilled(expression(f)(ctx.copy(t = newT, p = newP)), porc.Unit())
        }
      }
      case Future(f) => {
        val fut = newVarName("fut")
        val newP = newVarName("P")
        val newC = newVarName("C")
        let((fut, porc.SpawnFuture(ctx.c, ctx.t, newP, newC, expression(f)(ctx.copy(p = newP, c = newC))))) {
          ctx.p(fut)
        }
      }
      case Force(f) => {
        porc.Force(ctx.p, ctx.c, argument(f))
      }
      case left Concat right => {
        val newC = newVarName("C")
        let((newC, porc.NewCounter(ctx.c, expression(right)))) {
          porc.TryFinally(expression(left)(ctx.copy(c = newC)), porc.Halt(newC))
        }
      }
      case DeclareDefs(defs, body) => {
        porc.Site(defs.map(orcdef), expression(body))
      }

      // We do not handle types
      case HasType(body, expectedType) => expression(body)
      case DeclareType(u, t, body) => expression(body)
      
      case VtimeZone(timeOrder, body) => 
        throw new FeatureNotSupportedException("Virtual time", expr)
        
      case FieldAccess(o, f) => {
        porc.GetField(ctx.p, ctx.c, ctx.t, argument(o), f)
      }
      case a: Argument => {
        ctx.p(argument(a))
      }
      case _ => ???
    }
    code
  }
  
  def argument(a: Argument): porc.Value = {
    a match {
      case c@Constant(v) => porc.OrcValue(v)
      case (x: BoundVar) => lookup(x)
      case _ => ???
    }
  }
  
  def orcdef(d: Def)(implicit ctx: ConversionContext): porc.SiteDef = {         
    val Def(f, formals, body, typeformals, argtypes, returntype) = d
    val newP = newVarName("P")
    val newC = newVarName("C")
    val newT = newVarName("T")
    val args = formals.map(lookup)
    val name = lookup(f)
    porc.SiteDefCPS(name, newP, newC, newT, args, expression(body)(ctx.copy(p = newP, c = newC, t = newT, recursives = ctx.recursives + f)))
  }
}

