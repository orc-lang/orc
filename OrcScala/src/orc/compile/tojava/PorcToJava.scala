//
// PorcToJava.scala -- Scala class PorcToJava
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.tojava

import orc.ast.porc._
import orc.values.Format
import scala.collection.mutable
import orc.values.Field
import orc.util.PrettyPrintInterpolator
import orc.util.FragmentAppender

// Add context to convertion to carry the closed variables of sites.

/** @author amp
  */
class PorcToJava {
  import PorcToJava._

  class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
    override val lineNoOffset = 3
    implicit def implicitInterpolator(sc: StringContext)(implicit ctx: ConversionContext) = new MyInterpolator(sc)
    class MyInterpolator(sc: StringContext)(implicit ctx: ConversionContext) extends Interpolator(sc) {
      override val processValue: PartialFunction[Any, FragmentAppender] = {
        case a: Expr =>
          expression(a)
      }
    }
  }
  val interpolator = new MyPrettyPrintInterpolator
  import interpolator._

  // TODO: Because I am debugging this does a lot of code formatting and is is not very efficient. All the formatting should be
  //   removed or optimized and perhaps this whole things should be reworked to generate into a single StringBuilder.

  def apply(prog: DefCPS): String = {
    assert(prog.arguments.isEmpty)
    implicit val ctx = ConversionContext(Map())
    val code = expression(prog.body)
    val name = "Prog"
    val source = pp"""// GENERATED!!
import orc.run.tojava.*;
import orc.values.Field;
import orc.values.OrcValue;
import orc.values.OrcObjectBase;
import orc.CaughtEvent;
import scala.math.BigInt$$;
import scala.math.BigDecimal$$;

public class $name extends OrcProgram {$StartIndent
  |private static final java.nio.charset.Charset UTF8 = java.nio.charset.Charset.forName("UTF-8");
  |@Override
  |public void call(Execution $execution, Continuation ${argument(prog.pArg)}, Counter ${argument(prog.cArg)}, Terminator ${argument(prog.tArg)}, Object[] __args) {$StartIndent
    |// Name: ${prog.name.optionalVariableName.getOrElse("")}
    |$code$EndIndent
  |}
  |
  |public static void main(String[] args) throws Exception {
  |  runProgram(args, new $name());
  |}
  |${buildConstantPool()}$EndIndent
}
    """
    source.toString()
  }

  val vars: mutable.Map[Var, String] = new mutable.HashMap()
  val fields: mutable.Map[Field, String] = new mutable.HashMap()
  val usedNames: mutable.Set[String] = new mutable.HashSet()
  var varCounter: Int = 0
  def newVarName(prefix: String = "_t"): String = {
    val p = escapeIdent(prefix, true)
    val name = if (usedNames contains p) {
      varCounter += 1
      p + "$c" + counterToString(varCounter)
    } else p
    usedNames += name
    name
  }
  def lookup(temp: Var): String = vars.getOrElseUpdate(temp, newVarName(temp.optionalVariableName.getOrElse("_v")))
  // The function handling code directly modifies vars to make it point to an element of an array. See orcdef().

  def lookupField(temp: Field): String = fields.getOrElseUpdate(temp, escapeIdent(temp.field, false))

  val constantPool: mutable.Map[(Class[_], AnyRef), ConstantPoolEntry] = new mutable.HashMap()
  var constantCounter: Int = 0
  def strip$(s: String): String = {
    if (s.charAt(s.length - 1) == '$') s.substring(0, s.length - 1) else s
  }
  def newConstant(v: AnyRef): ConstantPoolEntry = {
    constantCounter += 1
    val name = escapeIdent(s"C_${Format.formatValue(v)}_${counterToString(constantCounter)}", false)
    val typ = v match {
      case _: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character
        | _: java.lang.Float | _: java.lang.Double | _: BigInt | _: BigDecimal => """Number"""
      case _: String => "String"
      case _: scala.collection.immutable.Nil.type => "scala.collection.immutable.Nil$"
      case _: scala.collection.immutable.::.type => "scala.collection.immutable.$colon$colon$"
      case _: java.lang.Boolean => "Boolean"
      case _: orc.values.Field => "Field"
      case _: orc.values.sites.DirectSite => "RuntimeDirectCallable"
      case _: orc.values.sites.Site => "RuntimeCallable"
      case _ => "Object"
    }
    val init = v match {
      case i @ (_: Integer | _: java.lang.Short | _: java.lang.Long | _: java.lang.Character) => s"""${i.getClass.getCanonicalName}.valueOf("$i")"""
      case i: BigInt if i.isValidLong => s"""BigInt$$.MODULE$$.apply(${i.toLong}L)"""
      // FIXME:HACK: This should use the underlying binary representation to make sure there is no loss of precision.
      case i: BigInt => s"""BigInt$$.MODULE$$.apply("$i")"""
      case n @ (_: java.lang.Float | _: java.lang.Double) => s"""${n.getClass.getCanonicalName}.valueOf("$n")"""
      case n: BigDecimal if n.isExactDouble => s"""BigDecimal$$.MODULE$$.apply(${n.toDouble})"""
      // FIXME:HACK: This should use the underlying binary representation to make sure there is no loss of precision.
      case n: BigDecimal => s"""BigDecimal$$.MODULE$$.apply("$n")"""
      case b: java.lang.Boolean => b.toString()
      case s: String => stringAsJava(s)
      case scala.collection.immutable.Nil => "scala.collection.immutable.Nil$.MODULE$"
      case scala.collection.immutable.:: => "scala.collection.immutable.$colon$colon$.MODULE$"
      case orc.values.Signal => "orc.values.Signal$.MODULE$"
      case orc.values.Field(s) => s"""new Field(${stringAsJava(s)})"""
      case x: orc.values.sites.JavaClassProxy => s"""Callable$$.MODULE$$.resolveJavaSite(${stringAsJava(x.name)})"""
      case x: orc.values.sites.DirectSite => s"""Callable$$.MODULE$$.resolveOrcDirectSite(${stringAsJava(strip$(x.getClass().getName))})"""
      case x: orc.values.sites.Site => s"""Callable$$.MODULE$$.resolveOrcSite(${stringAsJava(strip$(x.getClass().getName))})"""
      case null => "null"
      case _ => throw new AssertionError("Could not convert value " + v.toString + " to a Java initializer.")
    }
    ConstantPoolEntry(v, typ, name, init)
  }
  def lookup(f: Field) = constantPool.getOrElseUpdate((classOf[Field], f), newConstant(f))
  def lookup(v: OrcValue) = {
    constantPool.getOrElseUpdate((if (v.value != null) v.value.getClass else classOf[Null], v.value), newConstant(v.value))
  }

  def buildConstantPool() = {
    val orderedEntries = constantPool.values.toSeq.sortBy(_.name)
    FragmentAppender.mkString(orderedEntries.map(e => FragmentAppender(s"private static final ${e.typ} ${e.name} = ${e.initializer};")),
        "\n")
  }

  def expression(expr: Expr, isJavaExpression: Boolean = false)(implicit ctx: ConversionContext): FragmentAppender = {
    val code: FragmentAppender = expr match {
      case v: Value => FragmentAppender(argument(v))

      case Call(target, arg) => {
        pp"""($coerceToContinuation${argument(target)}).call(${argument(arg)});"""
      }
      case SiteCall(target, p, c, t, args) => {
        pp"""$coerceToSiteCallable(${argument(target)}).call($execution, $coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), new Object[] { ${args.map(argument(_)).mkString(",")} });"""
      }
      case Let(x, SiteCallDirect(target, args), b) => {
        assert(!isJavaExpression)
        pp"""$DNL
        |Object ${argument(x)};
        |${sitecalldirect(Some(x), target, args.toList)}
        |${expression(b)}
        """

      }
      case SiteCallDirect(target, args) => {
        assert(!isJavaExpression)
        sitecalldirect(None, target, args.toList)
      }
      case DefCall(target, p, c, t, args) => {
        pp"""($coerceToDefCallable${argument(target)}).call($execution, $coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), new Object[] { ${args.map(argument(_)).mkString(",")} });"""
      }
      case DefCallDirect(target, args) => {
        pp"""($coerceToDefDirectCallable${argument(target)}).directcall($execution, new Object[] { ${args.map(argument(_)).mkString(",")} });"""
      }
      case IfDef(arg, left, right) => {
        pp"""$DNL
        |if($isInstanceOfDef(${argument(arg)})) {$StartIndent
          |$left$EndIndent
        |} else {$StartIndent
          |$right$EndIndent
        |}
        """
      }
      case Let(x, v, b) => {
        assert(!isJavaExpression)
        pp"""$DNL
        |Object ${argument(x)} = $StartIndent${expression(v, true)}$EndIndent;
        |${expression(b)}
        """
      }
      case Sequence(es) => {
        assert(!isJavaExpression)
        FragmentAppender.mkString(es.map(expression(_)), "\n")
      }
      case Continuation(arg, b) => {
        pp"""(Continuation)((${argument(arg)}) -> { // Line#$LineNumber$StartIndent
          |$b$EndIndent
        |})"""
      }
      case DefDeclaration(defs, body) => {
        assert(!isJavaExpression)
        val decls = FragmentAppender.mkString((for (d <- defs) yield {
          val wrapper = newVarName(d.name.optionalVariableName.getOrElse("_f"))
          vars(d.name) = wrapper + "[0]"
          // FIXME:HACK: This uses an array to allow recursive lambdas. A custom invoke dynamic with recursive binding should be possible.
          d match {
            case _: DefCPS =>
              pp"""final Callable[] $wrapper = new Callable[1];"""
            case _: DefDirect =>
              pp"""final DirectCallable[] $wrapper = new DirectCallable[1];"""
          }
        }), "\n")
        // TODO: This gets all free variables regardless of where they came from. Some may be known not to be futures. Getting access to analysis would allow us to use isNotFuture.
        val fvs = closedVars(defs.flatMap(d => d.body.freevars -- d.allArguments).toSet)
        // TODO: It might be better to build a special join object that represents all the values we are closing over. Then all closures can use it instead of building a resolver for each.

        val newctx = ctx.copy(closedVars = ctx.closedVars ++ defs.map(d => (d.name, fvs)))

        pp"""
        |$decls
        |${FragmentAppender.mkString(defs.map(orcdef(_, fvs)(newctx)))}
        |${expression(body)(newctx)}"""
      }

      case Spawn(c, t, b) => {
        pp"""$DNL
        |$execution.spawn($coerceToCounter${argument(c)}, $coerceToTerminator${argument(t)}, () -> {$StartIndent $b $EndIndent});
        """
      }

      case NewTerminator(t) => {
        pp"""new TerminatorNested($coerceToTerminator(${argument(t)}))"""
      }
      case Kill(t) => {
        pp"""($coerceToTerminator${argument(t)}).kill();"""
      }

      case NewCounter(c, h) => {
        pp"""$DNL
        |new CounterNested($execution, $coerceToCounter(${argument(c)}), () -> { // Line#$LineNumber$StartIndent
          |$h$EndIndent
        |})"""
      }
      case Halt(c) => {
        pp"""($coerceToCounter${argument(c)}).halt();"""
      }
      case SetDiscorporate(c) => {
        pp"""($coerceToCounter${argument(c)}).setDiscorporate();"""
      }

      case New(bindings) => {
        def objectMember(p: (Field, Expr)) = {
          val (f, e) = p
          val field = lookupField(f)
          val body = expression(e, true)
          s"  public final Object $field = $body;"
        }
        val memberFields = bindings.map(p => lookup(p._1).name)
        val memberCases = bindings.map(p => {
          val n = stringAsJava(p._1.field)
          val field = lookupField(p._1)
          s"    case $n: return $field;"
        })
        pp"""$DNL
        |new OrcObjectBase() {$StartIndent
          |private final java.lang.Iterable<Field> MEMBERS = java.util.Arrays.asList(${memberFields.mkString(", ")});
          |public java.lang.Iterable<Field> getMembers() { return MEMBERS; }
          |public Object getMember(Field $$f$$) throws orc.error.runtime.NoSuchMemberException {$StartIndent switch($$f$$.field()) {$StartIndent
            |${memberCases.mkString("\n")}$EndIndent
            |}
            |throw new orc.error.runtime.NoSuchMemberException(this, $$f$$.field());
          |}
          |${bindings.map(objectMember).mkString("\n")}$EndIndent
        |}"""
      }

      // ==================== FUTURE ===================

      case NewFuture() => {
        pp"""new orc.run.tojava.Future();"""
      }
      case SpawnBindFuture(f, c, t, pArg, cArg, e) => {
        pp"""$execution.spawnBindFuture($coerceToFuture(${argument(f)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), (${argument(pArg)}, ${argument(cArg)}) -> {$StartIndent
          |$e$EndIndent
        |});
        """
      }
      case Force(p, c, t, true, vs) => {
        pp"""$execution.force($coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), new Object[] { ${vs.map(argument(_)).mkString(",")} });"""
      }
      case Force(p, c, t, false, vs) => {
        pp"""$execution.forceForCall($coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), new Object[] { ${vs.map(argument(_)).mkString(",")} });"""
      }
      case TupleElem(v, i) => {
        assert (isJavaExpression)
        pp"""((Object[])${argument(v)})[$i]"""
      }
      case GetField(p, c, t, o, f) => {
        pp"""$execution.getField($coerceToContinuation(${argument(p)}), $coerceToCounter(${argument(c)}), $coerceToTerminator(${argument(t)}), ${argument(o)}, ${lookup(f).name});"""
      }

      case TryFinally(b, h) => {
        assert(!isJavaExpression)
        pp"""$DNL
        |try {$StartIndent
          |$b$EndIndent
        |} finally {$StartIndent
          |$h$EndIndent
        |}
        """
      }
      case TryOnKilled(b, h) => {
        assert(!isJavaExpression)
        pp"""$DNL
        |try {$StartIndent
          |$b$EndIndent
        |} catch(KilledException __e) {$StartIndent
          |$h$EndIndent
        |}
        """
      }
      case TryOnHalted(b, h) => {
        assert(!isJavaExpression)
        val e = newVarName("e")
        pp"""$DNL
        |try {$StartIndent
          |$b$EndIndent
        |} catch(HaltException $e) {$StartIndent
          |assert $e.getCause() == null;
          |$h$EndIndent
        |}
        """
      }

      case Unit() => FragmentAppender("")

      //case _ => ???
    }
    code
  }

  def getIdent(x: Var): String = lookup(x)

  def sitecalldirect(bindTo: Option[Var], target: Value, args: List[Value])(implicit ctx: ConversionContext): FragmentAppender = {
    lazy val Signal = FragmentAppender(argument(OrcValue(orc.values.Signal)))
    lazy val throwHalt = "HaltException$.MODULE$.throwIt()"
    lazy val throwHaltStat = "throw HaltException$.MODULE$.SINGLETON()"

    def maybeBind(v: FragmentAppender) = bindTo.map(x => pp"${argument(x)} = $v;").getOrElse(pp"")

    (target, args) match {
      case (OrcValue(orc.lib.state.NewFlag), List()) =>
        maybeBind(pp"""new orc.lib.state.Flag();""")
      case (OrcValue(orc.lib.state.SetFlag), List(f)) if bindTo.isEmpty =>
        pp"""$DNL
        |((orc.lib.state.Flag)${argument(f)}).set();
        """
      case (OrcValue(orc.lib.state.PublishIfNotSet), List(f)) =>
        pp"""$DNL
        |if(((orc.lib.state.Flag)${argument(f)}).get()) $throwHaltStat;
        |${maybeBind(Signal)}
        """
      case (OrcValue(orc.lib.builtin.Ift), List(b)) =>
        pp"""$DNL
        |if(!(Boolean)${argument(b)}) $throwHaltStat;
        |${maybeBind(Signal)}
        """
      case (OrcValue(orc.lib.builtin.Iff), List(b)) =>
        pp"""$DNL
        |if((Boolean)${argument(b)}) $throwHaltStat;
        |${maybeBind(Signal)}
        """
      case (OrcValue(v: orc.values.sites.DirectSite), _) =>
        val temp = FragmentAppender(newVarName("temp"))
        val e = newVarName("exc")
        pp"""$DNL
        |Object $temp = null;
        |try {
        |  $temp = ${argument(target)}.site().calldirect(new Object[] {${args.map(argument(_)).mkString(", ")}});
        |} catch(Exception $e) {
        |  Callable$$.MODULE$$.rethrowDirectCallException($execution, $e);
        |}
        |${maybeBind(temp)}
        """
      /*case (OrcValue(v: orc.values.sites.DirectSite), _) =>
        maybeBind(j"""
        |${argument(target)}.directcall($execution, new Object[] { ${args.map(argument(_)).mkString(",")} })
        """)*/
      case _ =>
        maybeBind(pp"""$coerceToSiteDirectCallable(${argument(target)}).directcall($execution, new Object[] { ${args.map(argument(_)).mkString(",")} })""")
    }
  }

  def argument(a: Value): String = {
    a match {
      case c @ OrcValue(v) => lookup(c).name
      case (x: Var) => getIdent(x)
      case Unit() => {
        // In statement positions Unit is like void. Otherwise who knows so assume we are in a statement position.
        ""
      }
      //case _ => ???
    }
  }

  def objectMember(m: (Field, Expr))(implicit ctx: ConversionContext): String = {
    objectMember(m._1, m._2)
  }

  def objectMember(f: Field, body: Expr)(implicit ctx: ConversionContext): (FragmentAppender, FragmentAppender) = {
    val name = lookupField(f)
    // TODO: This needs to capture a value not sure what value.
    (pp"""public OrcValue $name = new orc.run.tojava.Future();\n""",
      pp"""$DNL
      |{ // Line#$LineNumber$StartIndent
        |${body}$EndIndent
      |}
    """)
  }

  def orcdef(d: Def, closedVars: Set[Var])(implicit ctx: ConversionContext): FragmentAppender = {
    val args = newVarName("args")
    val rt = newVarName("rt")
    val freevarsStr = pp"new Object[] { ${closedVars.map(argument).mkString(", ")} }"
    val renameargs = FragmentAppender.mkString(d.arguments.zipWithIndex.map(p => pp"Object ${argument(p._1)} = $args[${p._2}];"), "\n")
    // We assume that previous generated code has defined a mutable variable named by vars(name)
    // This will set it to close the recursion.
    d match {
      case DefCPS(name, p, c, t, formals, b) => {
        pp"""
        |${vars(name)} = new ForcableCallable($freevarsStr, ($rt, ${argument(p)}, ${argument(c)}, ${argument(t)}, $args) -> { // Line#$LineNumber$StartIndent
          |$renameargs
          |${expression(b)}$EndIndent
        |});
        """
      }
      case DefDirect(name, formals, b) => {
        pp"""
        |${vars(name)} = new ForcableDirectCallable($freevarsStr, ($rt, $args) -> { // Line#$LineNumber$StartIndent
          |$renameargs
          |${expression(b)}$EndIndent
        |};
        """
      }
    }
  }

  def closedVars(freeVars: Set[Var])(implicit ctx: ConversionContext): Set[Var] = {
    freeVars.flatMap { x => ctx.closedVars.get(x).getOrElse(Set(x)) }
  }
}

object PorcToJava {
  val execution = "exec"
  val coerceToContinuation = "(Continuation)"
  val isInstanceOfDef = "Coercions$.MODULE$.isInstanceOfDef"
  val coerceToDefCallable = "(Callable)"
  val coerceToDefDirectCallable = "(DirectCallable)"
  val coerceToSiteCallable = "Coercions$.MODULE$.coerceSiteToCallable"
  val coerceToSiteDirectCallable = "Coercions$.MODULE$.coerceSiteToDirectCallable"
  val coerceToCounter = "(Counter)"
  val coerceToFuture = "(Future)"
  val coerceToTerminator = "(Terminator)"

  val javaKeywords = Set(
    "abstract", "continue", "for", "new", "switch",
    "assert", "default", "goto", "package", "synchronized",
    "boolean", "do", "if", "private", "this",
    "break", "double", "implements", "protected", "throw",
    "byte", "else", "import", "public", "throws",
    "case", "enum", "instanceof", "return", "transient",
    "catch", "extends", "int", "short", "try",
    "char", "final", "interface", "static", "void",
    "class", "finally", "long", "strictfp", "volatile",
    "const", "float", "native", "super", "while")

  private def escapeJavaKeywords(s: String) = {
    if (javaKeywords contains s) {
      s + "_"
    } else {
      s
    }
  }

  def escapeIdent(s: String, includeStartMarker: Boolean) = {
    val q = s.map({ c =>
      c match {
        case '$' => "$$"
        case c if Character.isJavaIdentifierPart(c) => c.toString
        case '.' => "$dot"
        case '-' => "$minus"
        case ''' => "$quote"
        case '`' => "$bquote"
        case c => "$" + c.toHexString
      }
    }).mkString
    if (includeStartMarker)
      "$s" + q
    else
      escapeJavaKeywords(q)
  }

  def counterToString(i: Int) = java.lang.Integer.toString(i, 36)

  def stringAsUTF8Array(s: String): String =
    s"""new String(new byte[] { ${s.getBytes("UTF-8").map(c => "0x" + c.toHexString).mkString(",")} }, UTF8)"""

  val safeChars = " \t._;:${}()[]-+/*,&^%#@!=?<>|~`"
  def escapeChar(c: Char): String = c match {
    case _ if (c.isLetterOrDigit || (safeChars contains c)) => c.toString
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\f' => "\\f"
    case '\b' => "\\b"
    case '\'' => "\\'"
    case '\"' => "\\\""
    case '\\' => "\\\\"
    case _ => s"\\u${c.toInt.formatted("%04d")}"
  }

  /** Convert a string to an executable Java expression that produces that string.
    */
  def stringAsJava(s: String): String = {
    // We will generate an escaped string unless we have surrogates. It might work with escapes, but just going UTF8 seems better.
    if (s.forall(!_.isSurrogate))
      "\"" + s.map(escapeChar).mkString("") + "\""
    else
      stringAsUTF8Array(s)
  }

  case class ConversionContext(closedVars: Map[Var, Set[Var]])
}

case class ConstantPoolEntry(value: AnyRef, typ: String, name: String, initializer: String)
