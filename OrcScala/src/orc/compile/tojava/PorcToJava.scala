package orc.compile.tojava

import orc.ast.porc._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field

/** @author amp
  */
class PorcToJava {
  import Deindent._, PorcToJava._
  
  // TODO: Because I am debugging this does a lot of code formatting and is is not very efficient. All the formatting should be 
  //   removed or optimized and perhaps this whole things should be reworked to generate into a single StringBuilder. 
  
  def apply(prog: SiteDefCPS): String = {
    assert(prog.arguments.isEmpty)
    val code = expression(prog.body).indent(2)
    val name = "Prog"
    val constants = buildConstantPool().indent(2)
    j"""
// GENERATED!!
import orc.run.tojava.*;
import orc.values.Field;
import scala.math.BigInt$$;
import scala.math.BigDecimal$$;


public class $name extends OrcProgram {
  private static final java.nio.charset.Charset UTF8 = java.nio.charset.Charset.forName("UTF-8");
$constants

  @Override
  public void call(ToJavaRuntime $runtime, Continuation ${argument(prog.pArg)}, Counter ${argument(prog.cArg)}, Terminator ${argument(prog.tArg)}, Object[] __args) {
    // Name: ${prog.name.optionalVariableName.getOrElse("")}
$code
  }

  public static void main(String[] args) throws Exception {
    runProgram(args, new $name());
  }
}
    """
  }
  
  val vars: mutable.Map[Var, String] = new mutable.HashMap()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): String = { varCounter += 1; escapeIdent(prefix) + "$c" + counterToString(varCounter) }
  def lookup(temp: Var) = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))
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
  def lookup(f: Field) = constantPool.getOrElseUpdate(f, newConstant(f))
  def lookup(v: OrcValue) = constantPool.getOrElseUpdate(v.value, newConstant(v.value))
  
  def buildConstantPool() = {
    val orderedEntries = constantPool.values.toSeq.sortBy(_.name)
    orderedEntries.map(e => s"private static final ${e.typ} ${e.name} = ${e.initializer};").mkString("\n")
  }


  implicit class Interpolator(private val sc: StringContext) {
    def j(args: Any*): String = {
      sc.checkLengths(args)
      val sb = new StringBuilder
      import sb.append
      for ((p, a) <- sc.parts.init zip args) {
        a match {
          case a: Expr =>
            append(p)
            append(expression(a))
          case v =>
            append(p)
            append(v.toString)
        }
      }
      append(sc.parts.last)
      sb.mkString
    }
  }

  def expression(expr: Expr): String = {
    val code = expr match {
      case Call(target, arg) => {
        j"""
        |$coerceToContinuation(${argument(target)}).call(${argument(arg)});
        """
      }
      case SiteCall(target, p, c, t, args) => {
        j"""
        |$coerceToCallable(${argument(target)}).call($runtime, $coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), new Object[] { ${args.map(argument(_)).mkString(",")} });
        """
      }
      case SiteCallDirect(target, args) => {
        j"""
        |$coerceToDirectCallable(${argument(target)}).directcall($runtime, new Object[] { ${args.map(argument(_)).mkString(",")} });
        """
      }
      case Let(x, v, b) => {
        j"""
        |Object ${argument(x)} = $v;
        |${expression(b).deindentedAgressively}
        """
      }
      case Sequence(es) => {
        es.map(expression(_)).mkString("\n")
      }
      case Continuation(arg, b) => {
        j"""
        |(Continuation)((${argument(arg)}) -> {
          |$b
        |})
        """
      }
      case Site(defs, body) => {
        j"""
        |${defs.map(orcdef(_)).mkString}
        |${expression(body).deindentedAgressively}"""
      }

      case Spawn(c, t, b) => {
        j"""
        |$runtime.spawn($coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), () -> { 
          |$b
        |});
        """
      }

      case NewTerminator(t) => {
        j"""new TerminatorNested($coerceToTerminator(${argument(t)}))"""
      }
      case Kill(t) => {
        j"""$coerceToTerminator(${argument(t)}).kill();"""
      }
      
      case NewCounter(c, h) => {
        j"""
        |new CounterNested($coerceToCounter(${argument(c)}), () -> {
          |$h
        |})"""
      }
      case Halt(c) => {
        j"""$coerceToCounter(${argument(c)}).halt();"""
      }

      // ==================== FUTURE ===================
      
      case SpawnFuture(c, t, p, e) => {
        j"""
        |$runtime.spawnFuture($coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), (${argument(p)}) -> {
          |$e
        |});
        """
      }
      case Force(p, c, f) => {
        j"""
        |$runtime.force($coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), ${argument(f)});
        """
      }
      case GetField(p, c, t, o, f) => {
        j"""
        |$runtime.getField($coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), ${argument(o)}, ${lookup(f).name});
        """
      }

      case Resolve(c, t, f, e) => ???
      case ForceMany(c, t, fs, args, e) => ???

      case TryFinally(b, h) => {
        j"""
        |try {
          |$b
        |} finally {
          |$h
        |}
        """
      }
      case TryOnKilled(b, h) => {
        j"""
        |try {
          |$b
        |} catch(KilledException __e) {
          |$h
        |}
        """
      }
      case TryOnHalted(b, h) => {
        j"""
        |try {
          |$b
        |} catch(HaltException __e) {
          |$h
        |}
        """
      }

      case Unit() => ""
        
      case _ => "???"
    }
    
    ///*[\n${expr.prettyprint().withoutLeadingEmptyLines.indent(1)}\n]*/\n
    val r = s"""${code.deindented}""".indent(2)
    r
  }
  
  def getIdent(x: Var): String = lookup(x)

  def argument(a: Value): String = {
    a match {
      case c@OrcValue(v) => lookup(c).name
      case (x: Var) => getIdent(x)
      case _ => ???
    }
  }
  
  def orcdef(d: SiteDef): String = {
    d match {
      case SiteDefCPS(name, p, c, t, formals, b) => {
        val args = newVarName("args")
        val wrapper = newVarName(name.optionalVariableName.getOrElse("_f"))
        val accessname = wrapper + "[0]"
        vars(name) = accessname
        // FIXME:HACK: This uses an array to allow recursive lambdas. A custom invoke dynamic with recursive binding should be possible.
        j"""
        |final Callable[] $wrapper = new Callable[1]; 
        |$accessname = (${argument(p)}, ${argument(c)}, ${argument(t)}, $args) -> {
          |${formals.zipWithIndex.map(p => j"Object ${argument(p._1)} = $args[${p._2}];").mkString("\n").indent(2)}
          |${expression(b)}
        |};
        """.deindented
      }
      case SiteDefDirect(name, formals, b) => {
        val args = newVarName("args")
        val wrapper = newVarName(name.optionalVariableName.getOrElse("_f"))
        val accessname = wrapper + "[0]"
        vars(name) = accessname
        // FIXME:HACK: This uses an array to allow recursive lambdas. A custom invoke dynamic with recursive binding should be possible.
        j"""
        |final DirectCallable[] $wrapper = new DirectCallable[1]; 
        |$accessname = ($args) -> {
          |${formals.zipWithIndex.map(p => j"Object ${argument(p._1)} = $args[${p._2}];").mkString("\n").indent(2)}
          |${expression(b)}
        |};
        """.deindented        
      }
    }
  }
}

object PorcToJava {
  val runtime = "__runtime"
  val coerceToContinuation = "Coercions$.MODULE$.coerceToContinuation"
  val coerceToCallable = "Coercions$.MODULE$.coerceToCallable"
  val coerceToDirectCallable = "Coercions$.MODULE$.coerceToDirectCallable"
  val coerceToCounter = "Coercions$.MODULE$.coerceToCounter"
  val coerceToTerminator = "Coercions$.MODULE$.coerceToTerminator"

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

