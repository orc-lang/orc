package orc.compile.tojava

import orc.ast.orctimizer.named._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field

/** @author amp
  */
class OrctimizerToJava {
  def apply(prog: Expression): String = {
    val code = expression(prog)(new ConversionContext(null, "ctx"))
    val name = "Prog"
    val constants = buildConstantPool()
    s"""
// GENERATED!!
import java.math.BigInteger;
import java.math.BigDecimal;

import orc.run.tojava.*;
import orc.values.Field;

public class $name extends OrcProgram {
  $constants

  @Override
  public void call(final Context ctx) {
    $code
  }

  public static void main(String[] args) throws Exception {
    runProgram(args, new $name());
  }
}
    """
  }
  
  
  def escapeIdent(s: String) = {
    s.map({ c =>
      if (c.isLetterOrDigit || c == '_') {
        c.toString
      } else {
        "$" + c.toInt
      }
    }).mkString
  }

  val vars: mutable.Map[BoundVar, String] = new mutable.HashMap()
  var varCounter: Int = 0
  def newVarName(): String = { varCounter += 1; "_t" + varCounter }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName())

  val constantPool: mutable.Map[AnyRef, ConstantPoolEntry] = new mutable.HashMap()
  var constantCounter: Int = 0
  def strip$(s: String): String = {
    if (s.charAt(s.length - 1) == '$') s.substring(0, s.length - 1) else s
  }
  def newConstant(v: AnyRef): ConstantPoolEntry = { 
    constantCounter += 1; 
    val name = escapeIdent(s"C${constantCounter}_${Format.formatValue(v)}")
    val typ = v match {
      case _: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character | _: BigInt 
        | _: java.lang.Float | _: java.lang.Double | _: BigDecimal => """Number"""
      case _: String => "String"
      case _: java.lang.Boolean => "Boolean"
      case _: orc.values.Field => "Field"
      case _: orc.values.sites.Site => "Callable"
      case _ => "Object"
    }
    val init = v match {
      case i @ (_: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character | _: BigInt) => s"""new BigInteger("$i")"""
      case n @ (_: java.lang.Float | _: java.lang.Double | _: BigDecimal) => s"""new BigDecimal("$n")"""
      case s: String => s""""$s""""
      case b: java.lang.Boolean => b.toString()
      case orc.values.Signal => "orc.values.Signal$.MODULE$"
      case orc.values.Field(s) => s"""new Field("$s")"""
      case x: orc.values.sites.JavaClassProxy => s"""Callable$$.MODULE$$.resolveJavaSite("${x.name}")"""
      case x: orc.values.sites.Site => s"""Callable$$.MODULE$$.resolveOrcSite("${strip$(x.getClass().getName)}")"""
      case _ => throw new AssertionError("Could not serialize value " + v.toString + " to a Java initializer.")
    }
    ConstantPoolEntry(v, typ, name, init)
  }
  def lookup(v: Constant) = constantPool.getOrElseUpdate(v.value, newConstant(v.value))
  
  def buildConstantPool() = {
    val orderedEntries = constantPool.values.toSeq.sortBy(_.name)
    orderedEntries.map(e => s"public static final ${e.typ} ${e.name} = ${e.initializer};").mkString("\n")
  }


  implicit class Interpolator(private val sc: StringContext)(implicit ctx: ConversionContext) {
    def j(args: Any*): String = {
      sc.checkLengths(args)
      val sb = new StringBuilder
      import sb.append
      append(sc.parts.head)
      for ((a, p) <- args zip sc.parts.tail) {
        a match {
          case a: Expression =>
            append(expression(a))
          case s: String =>
            append(s)
          case c: ConversionContext =>
            append(c.ctxname)
        }
       append(p)
      }
      sb.mkString.stripPrefix("\n").stripLineEnd.trim
    }
  }

  def expression(expr: Expression)(implicit ctx: ConversionContext): String = {
    expr match {
      case Stop() => ""
      case Call(target, args, typeargs) => {
        j"""
        orc.run.tojava.Callable$$.MODULE$$.coerceToCallable(${argument(target)}).call($ctx, new Object[] { ${args.map(argument(_)).mkString(",")} });
        """
      }
      case left || right => {
        j"""
        $ctx.spawn(() -> { $left });
        $right;
        """
      }
      case Sequence(left, x, right) => {
        val newctx = ctx.newContext()
        j"""
        final BranchContext $newctx = new BranchContext($ctx, (${newctx}_, ${argument(x)}) -> {
          $right
        });
        ${expression(left)(newctx)}
        """
      }
      case Limit(f) => {
        val newctx = ctx.newContext()
        j"""      
        final TerminatorContext $newctx = new TerminatorContext($ctx);
        try {
          ${expression(f)(newctx)}
        } catch (KilledException e) {
        }
        """
      }
      case Future(f) => {
        val newctx = ctx.newContext()
        j"""
        Future fut = $ctx.spawnFuture(($newctx) -> {
          ${expression(f)(newctx)}
        });
        $ctx.publish(fut);
        """
      }
      case Force(f) => {
        j"""
        Operations$$.MODULE$$.force($ctx, ${argument(f)});
        """
      }
      case left Concat right => {
        val newctx = ctx.newContext()
        j"""
        CounterContext $newctx = new CounterContext($ctx, (${newctx}_) -> {
          $right
        });
        try {
          ${expression(left)(newctx)}
        } finally {
          $newctx.halt();
        }
        """
      }
      case DeclareDefs(defs, body) => {
        ???
      }
      case HasType(body, expectedType) => expression(body)
      case DeclareType(u, t, body) => expression(body)
      case VtimeZone(timeOrder, body) => ???
      case FieldAccess(o, f) => {
        j"""
        Operations$$.MODULE$$.getField($ctx, ${argument(o)}, ${argument(Constant(f))});
        """
      }
      case a: Argument => {
        j"""
        $ctx.publish(${argument(a)});
        """
      }
      // We do not handle types
      case _ => "???"
    }
  }

  def argument(a: Argument): String = {
    a match {
      case c@Constant(v) => lookup(c).name
      case (x: BoundVar) => escapeIdent(x.optionalVariableName.getOrElse(lookup(x)))
      case _ => ???
    }
  }

  /*
         case Def(f, formals, body, typeformals, argtypes, returntype) => {
        val name = f.optionalVariableName.getOrElse(lookup(f))
        "def " + name + brack(typeformals) + paren(argtypes.getOrElse(Nil)) +
          (returntype match {
            case Some(t) => " :: " + reduce(t)
            case None => ""
          }) +
          "\n" +
          "def " + name + paren(formals) + " = " + reduce(body) +
          "\n"
      }
   
   */
}

case class ConversionContext(parent: ConversionContext, ctxname: String) {
  var nextChild: Int = 0

  def newContext(): ConversionContext = {
    nextChild += 1
    ConversionContext(this, s"${ctxname}_$nextChild")
  }
}

case class ConstantPoolEntry(value: AnyRef, typ: String, name: String, initializer: String)