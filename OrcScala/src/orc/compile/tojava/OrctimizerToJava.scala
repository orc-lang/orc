package orc.compile.tojava

import orc.ast.orctimizer.named._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field

/** @author amp
  */
class OrctimizerToJava {
  import Deindent._, OrctimizerToJava._
  
  // TODO: Because I am debugging this does a lot of code formatting and is is not very efficient. All the formatting should be 
  //   removed or optimized and perhaps this whole things should be reworked to generate into a single StringBuilder. 
  
  def apply(prog: Expression): String = {
    val code = expression(prog)(new ConversionContext("ctx")).indent(2)
    val name = "Prog"
    val constants = buildConstantPool().indent(2)
    s"""
// GENERATED!!
import orc.run.tojava.*;
import orc.values.Field;
import scala.math.BigInt$$;
import scala.math.BigDecimal$$;


public class $name extends OrcProgram {
  private static final java.nio.charset.Charset UTF8 = java.nio.charset.Charset.forName("UTF-8");
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
  
  val vars: mutable.Map[BoundVar, String] = new mutable.HashMap()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): String = { varCounter += 1; escapeIdent(prefix) + "$c" + counterToString(varCounter) }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))
  // The function handling code directly modifies vars to make it point to an element of an array. See orcdef().

  val constantPool: mutable.Map[AnyRef, ConstantPoolEntry] = new mutable.HashMap()
  var constantCounter: Int = 0
  def strip$(s: String): String = {
    if (s.charAt(s.length - 1) == '$') s.substring(0, s.length - 1) else s
  }
  def newConstant(v: AnyRef): ConstantPoolEntry = {
    constantCounter += 1
    val name = escapeIdent(s"C_${Format.formatValue(v)}_${counterToString(constantCounter)}")
    val typ = v match {
      case _: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character
        | _: java.lang.Float | _: java.lang.Double => """Number"""
      case _: String => "String"
      case _: java.lang.Boolean => "Boolean"
      case _: orc.values.Field => "Field"
      case _: orc.values.sites.Site => "Callable"
      case _ => "Object"
    }
    val init = v match {
      case i @ (_: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character) => s"""${i.getClass.getCanonicalName}.valueOf("$i")"""
      case i: BigInt if i.isValidLong => s"""BigInt$$.MODULE$$.apply(${i.toLong}L)"""
      // FIXME:HACK: This should use the underlying binary representation to make sure there is no loss of precision.
      case i: BigInt => s"""BigInt$$.MODULE$$.apply("$i")"""
      case n @ (_: java.lang.Float | _: java.lang.Double) => s"""${n.getClass.getCanonicalName}.valueOf("$n")"""
      case n : BigDecimal if n.isExactDouble => s"""BigDecimal$$.MODULE$$.apply(${n.toDouble})"""
      // FIXME:HACK: This should use the underlying binary representation to make sure there is no loss of precision.
      case n : BigDecimal => s"""BigDecimal$$.MODULE$$.apply("$n")"""
      case s: String if s.forall(c => c >= 32 && c < 127 && c != '\n' && c != '\\') => "\""+s+"\""
      case s: String => s"""new String(new byte[] { ${s.getBytes("UTF-8").map(c => "0x"+c.toHexString).mkString(",")} }, UTF8)"""
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
    orderedEntries.map(e => s"private static final ${e.typ} ${e.name} = ${e.initializer};").mkString("\n")
  }


  implicit class Interpolator(private val sc: StringContext)(implicit ctx: ConversionContext) {
    def j(args: Any*): String = {
      sc.checkLengths(args)
      val sb = new StringBuilder
      import sb.append
      for ((p, a) <- sc.parts.init zip args) {
        a match {
          case a: Expression =>
            append(p)
            append(expression(a))
          case c: ConversionContext =>
            append(p)
            append(c.ctxname)
          case v =>
            append(p)
            append(v.toString)
        }
      }
      append(sc.parts.last)
      sb.mkString
    }
  }

  def expression(expr: Expression)(implicit ctx: ConversionContext): String = {
    val code = expr match {
      case Stop() => ""
      case Call(target, args, typeargs) => {
        j"""
        |Callable$$.MODULE$$.coerceToCallable(${argument(target)}).call($ctx, new Object[] { ${args.map(argument(_)).mkString(",")} });
        """
      }
      case left || right => {
        j"""
        |$ctx.spawn((${newVarName()}) -> { 
          |$left
        |});
        |${expression(right).deindentedAgressively};
        """
      }
      case Sequence(left, x, right) => {
        val newctx = ctx.newContext()
        j"""
        |final BranchContext $newctx = new BranchContext($ctx, (${newctx}_, ${argument(x)}) -> {
          |$right
        |});
        |${expression(left)(newctx).deindentedAgressively}
        """
      }
      case Limit(f) => {
        val newctx = ctx.newContext(1)
        j"""      
        |final TerminatorContext $newctx = new TerminatorContext($ctx);
        |try {
          |${expression(f)(newctx)}
        |} catch (KilledException e) {}
        """
      }
      case Future(f) => {
        val newctx = ctx.newContext(1)
        j"""
        |$ctx.publish($ctx.spawnFuture(($newctx) -> {
          |${expression(f)(newctx)}
        |}));
        """
      }
      case Force(f) => {
        j"""
        |Operations$$.MODULE$$.force($ctx, ${argument(f)});
        """
      }
      case left Concat right => {
        val newctx = ctx.newContext(1)
        j"""
        |CounterContext $newctx = new CounterContext($ctx, (${newctx}_) -> {
          |$right
        |});
        |try {
          |${expression(left)(newctx)}
        |} finally {
        |  $newctx.halt();
        |}
        """
      }
      case DeclareDefs(defs, body) => {
        j"""
        |${defs.map(orcdef(_)).mkString}
        |${expression(body).deindentedAgressively}"""
      }

      // We do not handle types
      case HasType(body, expectedType) => expression(body)
      case DeclareType(u, t, body) => expression(body)
      
      case VtimeZone(timeOrder, body) => ???
      case FieldAccess(o, f) => {
        j"""
        |Operations$$.MODULE$$.getField($ctx, ${argument(o)}, ${argument(Constant(f))});
        """
      }
      case a: Argument => {
        j"""
        |$ctx.publish(${argument(a)});
        """
      }
      case _ => "???"
    }
    
    //
    val r = s"""/*[\n${expr.prettyprint().withoutLeadingEmptyLines.indent(1)}\n]*/\n${code.deindented}""".indent(2)
    r
  }
  
  def getIdent(x: BoundVar): String = lookup(x)

  def argument(a: Argument): String = {
    a match {
      case c@Constant(v) => lookup(c).name
      case (x: BoundVar) => getIdent(x)
      case _ => ???
    }
  }
  
  def orcdef(d: Def)(implicit ctx: ConversionContext): String = {         
    val Def(f, formals, body, typeformals, argtypes, returntype) = d
    val newctx = ctx.newContext()
    val args = newVarName("args")
    val wrapper = newVarName(f.optionalVariableName.getOrElse("_f"))
    val name = wrapper + "[0]"
    vars(f) = name
    // FIXME:HACK: This uses an array to allow recursive lambdas. A custom invoke dynamic with recursive binding should be possible.
    j"""
    |final Callable[] $wrapper = new Callable[1]; 
    |$name = ($newctx, $args) -> {
      |${formals.zipWithIndex.map(p => j"Object ${argument(p._1)} = $args[${p._2}];").mkString("\n").indent(2)}
      |${expression(body)(newctx)}
    |};
    """.deindented
  }
}

object OrctimizerToJava {
  def escapeIdent(s: String) = {
    val q = s.map({ c =>
      c match {
        case c if c.isLetterOrDigit || c == '_' => c.toString
        case '$' => "$$"
        case '.' => "$_"
        case '-' => "$m"
        case ''' => "$p"
        case '`' => "$t"
        case c => "$" + c.toHexString
      }
    }).mkString
    if (q(0).isLetter || q(0) == '_' || q(0) == '$')
      q
    else
      "$s" + q
  }

  def counterToString(i: Int) = java.lang.Integer.toString(i, 36)
}

case class ConversionContext(ctxname: String) {
  var nextChild: Int = 0

  def newContext(levelChange: Int = 0): ConversionContext = {
    nextChild += 1
    ConversionContext(s"${ctxname}_${OrctimizerToJava.counterToString(nextChild)}")
  }
}


case class ConstantPoolEntry(value: AnyRef, typ: String, name: String, initializer: String)

object Deindent {
  val EmptyLine = raw"""(\p{Blank}*\n)+""".r
  val EmptyLinesEnd = raw"""(\n\p{Blank}*)+\z""".r
  val NonBlank = raw"""[^\p{Blank}]""".r
  
  implicit final class DeindentString(private val s: String) { 
    def deindentedAgressively = {
      val lines = s.withoutLeadingEmptyLines.withoutTrailingEmptyLines.split('\n')
      val indentSize = lines.map(l => NonBlank.findFirstMatchIn(l).map(_.start).getOrElse(Int.MaxValue)).min
      lines.map(_.substring(indentSize)).mkString("\n")
    }
    
    def deindented = {
      s.withoutLeadingEmptyLines.withoutTrailingEmptyLines.stripMargin
    }
    
    def indent(n: Int) = {
      val lines = s.split('\n')
      val p = " " * n
      lines.map(p + _).mkString("\n")
    }
    
    def withoutLeadingEmptyLines = {
      EmptyLine.findPrefixMatchOf(s).map({ m =>
        s.substring(m.end)
      }).getOrElse(s)
    }
    def withoutTrailingEmptyLines = {
      EmptyLinesEnd.findFirstMatchIn(s).map({ m =>
        s.substring(0, m.start)
      }).getOrElse(s)
    }
  }
}