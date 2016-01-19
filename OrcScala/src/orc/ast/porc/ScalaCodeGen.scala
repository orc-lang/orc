//
// PrettyPrint.scala -- Scala class/trait/object PrettyPrint
// Project OrcScala
//
// $Id$
//
// Created by amp on May 28, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.values.Format
import orc.values.Field

/** Generate a Scala code string from Porc code.
  *
  * @author amp
  */
class ScalaCodeGen {
  import orc.values.Format.unparseString
  
  val imports = """
import orc.run.compiled._
import orc.OrcExecutionOptions
import orc.values.sites.DirectSite
import orc.values.sites.HaltedException
import orc.error.OrcException
import orc.CaughtEvent
import orc.run.porc.Expr
import orc.error.runtime.JavaException
import orc.run.porc.KilledException
"""

  def strip$(s: String): String = {
    if (s.charAt(s.length - 1) == '$') s.substring(0, s.length - 1) else s
  }

  def reduceSlowValue(v: AnyRef): Option[String] = v match {
    case x: orc.values.sites.JavaClassProxy => 
      Some(s"""orc.values.sites.JavaSiteForm.resolve("${x.name}")""")
    case x: orc.values.sites.Site => 
      Some(s"""orc.values.sites.OrcSiteForm.resolve("${strip$(x.getClass().getName)}")""")
    case _ => None
  }

  def apply(ast: PorcAST): (String, Map[String, AnyRef]) = {
    val code = reduce(ast in TransformContext.Empty, 6)
    
    val valueMap = fixedValues.map(_.swap).toMap
    val slowValues = valueMap.mapValues(reduceSlowValue).collect({ case (n, Some(s)) => (n,s) })
    val externalValues = valueMap.filterKeys(n => !slowValues.contains(n))

    val slows = slowValues.map((p) => s"    val ${p._1} = ${p._2};").mkString("", "\n", "\n")
    val externals = externalValues.map((p) => s"    // ${p._1} = ${p._2}: ${p._2.getClass.getCanonicalName()}").mkString("", "\n", "\n")

    val s = imports ++ s"""
(c: Counter, eH: (OrcEvent) => Unit, opts: OrcExecutionOptions, ctx: RuntimeContext) => { 
  new OrcModuleInstance(c, eH, opts, ctx) {
$externals
$slows
    def apply() = {
      val C = initCounter
      val oldC = null
      val T = new Terminator()
            
$code
    }
  }
}"""
    (s, externalValues)
  }

  def tag(ast: PorcAST, s: String): String = s"${ast.number.map(_ + ": ").getOrElse("")}$s"

  def indent(i: Int, n: Option[Int] = None) = {
    n match {
      case None => " " * i
      case Some(n) =>
        val sn = n.toString
        sn + ": " + (" " * (i - sn.size - 2))
    }
  }

  val currentTerminator = "T"
  val currentCounter = "C"
  val oldCounter = "oldC"

  def escape(s: String) = {
    s.replace("$", "$$").replace("`", "_").replace("'", "$_")
  }
  
  def mkArgumentList(args: List[Value], ctx: TransformContext, pre: String = "(", post: String = ")"): String = {
    mkArgumentList(args.map(_ in ctx), pre, post)
  }
  def mkArgumentList(args: List[WithContext[Value]], pre: String, post: String): String = {
    args.map(a => reduce(a, 7)).mkString(pre, ", ", post)
  }
  def mkArgumentList(args: List[WithContext[Value]]): String = {
    mkArgumentList(args, "(", ")")
  }
  
  def hasDirect(vc: WithContext[Var]) = {
    val v in ctx = vc
    ctx(v) match {
      case LetBound(_, Let(_, Lambda(_, _), _)) =>
        true
      case _ =>
        false
    }
  }
  def isSiteBound(vc: WithContext[Var]) = {
    val v in ctx = vc
    ctx(v) match {
      case SiteBound(_, _, _) | RecursiveSiteBound(_, _, _) =>
        true
      case _ =>
        false
    }
  }
    
  def variableRaw(vc: orc.ast.porc.WithContext[orc.ast.porc.Var]): String = {
    escape(vc.optionalVariableName.getOrElse(vc.e.toString)) 
  }
  
  def closureCall(vc: WithContext[Var], args: List[Value]) = {
    if(hasDirect(vc)) {
      variableRaw(vc) + "Direct" + mkArgumentList(args, vc.ctx)
    } else {
      variableRaw(vc) + "(Seq[AnyRef]" + mkArgumentList(args, vc.ctx) + ")"
    }
  }

  def closureNullary(vc: WithContext[Var]) = {
    if(hasDirect(vc)) {
      variableRaw(vc) + "Direct _"
    } else {
      s"() => ${variableRaw(vc)}(Nil)"
    }
  }
  
  def closureUnary(vc: WithContext[Var]) = {
    if(hasDirect(vc)) {
      variableRaw(vc) + "Direct _"
    } else {
      s"(v) => ${variableRaw(vc)}(Seq[AnyRef](v))"
    }
  }
  
  def variable(vc: WithContext[Var]) = {
    val v in ctx = vc
    ctx(v) match {
      case LetBound(_, Let(_, Lambda(_, _), _)) | SiteBound(_, _, _) | RecursiveSiteBound(_, _, _) =>
        variableRaw(vc) + " _"
      case _ =>
        variableRaw(vc)
    }
  }
  
  def newvariable(vc: WithContext[Var]) = variableRaw(vc)

  // Only generate literals for things that can be quickly computed. If precomputing it and storing the value is better then put in externals for later handling.
  def reduceValue(v: Any): String = {
    v match {
      case null => "null"
      case l: List[_] => "List(" + l.map(reduceValue).mkString(", ") + ")"
      case s: String => unparseString(s)
      case Some(v) => "Some(" + reduceValue(v) + ")"
      case None => "None"
      case true => "java.lang.Boolean.TRUE"
      case false => "java.lang.Boolean.FALSE"
      case _: Int | _: Double | _: Byte => s"$v.asInstanceOf[AnyRef]"
      case i: BigInt => {
        if (i.isValidInt) {
          s"BigInt(${i.toInt})"
        } else {
          val bs = i.toByteArray
          s"BigInt(${reduceValue(bs)})"
        }
      }
      case i: BigDecimal => {
        if (i.isValidDouble) {
          s"BigDecimal(${i.toDouble})"
        } else {
          val v = i.bigDecimal.unscaledValue()
          val s = i.bigDecimal.scale()
          s"BigDecimal(${reduceValue(v)}, $s)"
        }
      }
      case a: Array[Byte] => s"Array[Byte](${a.mkString(",")})"
      case f: Field => s"""orc.values.Field("${f.field}")"""
      case orc.values.Signal => "orc.values.Signal"

      case other: AnyRef => registerFixedValue(other)
    }
  }

  val fixedValues = new scala.collection.mutable.HashMap[AnyRef, String]()
  var nextFixedValueIndex = 0

  def registerFixedValue(v: AnyRef) = {
    fixedValues.get(v) match {
      case Some(name) =>
        name
      case None =>
        val name = s"_external_$nextFixedValueIndex"
        nextFixedValueIndex += 1
        fixedValues += (v -> name)
        name
    }
  }

  def reduce(ast: WithContext[PorcAST], i: Int = 0): String = {
    implicit class RecursiveReduce(val sc: StringContext) {
      import StringContext._
      import sc._

      def rd(args: Any*): String = {
        checkLengths(args)
        val pi = parts.iterator
        val ai = args.iterator
        val bldr = new java.lang.StringBuilder(treatEscapes(pi.next()))
        while (ai.hasNext) {
          val a = ai.next
          a match {
            case a: WithContext[PorcAST] => bldr append reduce(a, i)
            case a: PorcAST => bldr append "!!!" append a append "!!!"
            case _ => bldr append a
          }
          bldr append treatEscapes(pi.next())
        }
        bldr.toString
      }
    }
    
    def porcNum = ast.number match { 
      case Some(n) => s"/* Porc Instruction #$n */"
      case None => ""
    }

    val ind = indent(i)
    ast match {
      case OrcValue(v) in ctx => reduceValue(v)
      //case Tuple(l) => l.map(reduce(_, i+1)).mkString("(",", ",")")
      case (v: Var) in ctx => variable(v in ctx)

      case LetIn(x, LambdaIn(args, ctx, lambdaBody), b) => rd"""{
${ind}def ${newvariable(x)}Direct(${args.map(a => reduce(a in ctx, i) + ": AnyRef").mkString(", ")}) = { ${reduce(lambdaBody, i + 3)} };
${ind}def ${newvariable(x)}(args: Seq[AnyRef]) = { val Seq(${args.map(a => reduce(a in ctx, i)).mkString(", ")}) = args; ${newvariable(x)}Direct(${args.map(a => reduce(a in ctx, i)).mkString(", ")}) };
$ind$b\n$ind}"""

      case LetIn(x, v, b) => rd"{\n${ind}val ${newvariable(x)} = { ${reduce(v, i + 3)} };\n$ind$b\n$ind}"
      case SiteIn(l, ctx, b) => rd"${l.map(a => reduce(a in ctx, i + 3)).mkString(";\n" + indent(i + 2))}\n${indent(i + 2)}\n$ind${reduce(b, i)}"
      case SiteDefIn(name, args, p, bodyctx, body) => 
        rd"def ${newvariable(name in bodyctx)}(args: Seq[AnyRef], ${p in bodyctx}: (AnyRef) => Unit, $currentCounter: Counter, $currentTerminator: Terminator): Unit = {"+
        rd"\n${ind}val Seq${mkArgumentList(args, bodyctx)} = args;\n\n$ind$body\n$ind};"

      //case Lambda(args, b) => rd"(${args.map(reduce(_, i)).mkString(", ")}) => {\n$ind$b\n$ind}"

      case CallIn((t: Var) in _, a, ctx) => closureCall(t in ctx, a) + " " + porcNum
      case CallIn(t, a, ctx) => rd"$t(Seq[AnyRef](${a.map(a => reduce(a in ctx, i)).mkString(", ")})) $porcNum"
      
      case SiteCallIn((target: Var) in _, arguments, p: Var, ctx) if isSiteBound(target in ctx) => {
        rd"""
$ind${variableRaw(target in ctx)}(Seq[AnyRef]${mkArgumentList(arguments, ctx)}, ${closureUnary(p in ctx)}, $currentCounter, $currentTerminator);
    """
      }
      case SiteCallIn(target, arguments, p: Var, ctx) => {
        rd"""
    ($target) match { $porcNum
      case clos: ((Seq[AnyRef], (AnyRef) => Unit, Counter, Terminator) => Unit) =>
        Logger.finer(s"sitecall: $target "+${unparseString(arguments.toString)}+" { $$clos }")
        clos(Seq[AnyRef]${mkArgumentList(arguments, ctx)}, ${closureUnary(p in ctx)}, $currentCounter, $currentTerminator)
      case v =>
        Logger.finer(s"sitecall: $target "+${unparseString(arguments.toString)}+" { $$v }")
        forceFutures(Seq[AnyRef]${mkArgumentList(arguments, ctx)}, invokeExternal(v, _, ${closureUnary(p in ctx)}, $currentCounter, $currentTerminator), $currentCounter, $currentTerminator)
        Unit
    }
    """
      }
      
      case DirectSiteCallIn(target, arguments, ctx) => {
        rd"""{ val s = $target.asInstanceOf[DirectSite] $porcNum
    Logger.finer(s"directsitecall: $target " + ${unparseString(arguments.toString)})
    try {
      s.directcall(List[AnyRef]${mkArgumentList(arguments, ctx)})
    } catch {
      case e: HaltedException => throw e
      case e: OrcException => {
        eventHandler(CaughtEvent(e))
        throw Expr.HaltedException
      }
      case e: Throwable => {
        eventHandler(CaughtEvent(new JavaException(e)))
        throw Expr.HaltedException
      }
    }
}"""
      }

      //case Project(n, v) => rd"project_$n $v"

      case SequenceIn(es, ctx) => es.map(a => reduce(a in ctx, i)).mkString(s";\n$ind")

      case IfIn(b, t, e) => rd"if($b) \n${indent(i + 2)}${reduce(t, i + 2)}\n${ind}else\n${indent(i + 2)}${reduce(e, i + 2)}"
      case TryOnKilledIn(b, h) => rd"try {\n${indent(i + 2)}${reduce(b, i + 2)}\n${ind}} catch { case _: KilledException =>\n${indent(i + 2)}${reduce(h, i + 2)} }"
      case TryOnHaltedIn(b, h) => rd"try {\n${indent(i + 2)}${reduce(b, i + 2)}\n${ind}} catch { case _: HaltedException =>\n${indent(i + 2)}${reduce(h, i + 2)} }"

      case SpawnIn(v) => rd"spawn(${closureNullary(v)}, $currentCounter) $porcNum"

      case NewCounterIn(k) => rd"{ val $oldCounter = $currentCounter; { val $currentCounter = new Counter(); $currentCounter.increment();\n$ind$k\n$ind} }"
      //case NewCounterDisconnected(k) => rd"counter disconnected in\n$ind$k"
      case RestoreCounterIn(a, b) => rd"""if ($currentCounter.decrementAndTestZero()) { $porcNum
${indent(i + 1)}val $currentCounter = $oldCounter;
${indent(i + 1)}{ val $oldCounter = null;
${indent(i + 1)}${reduce(a, i + 1)}
$ind}} else {
${indent(i + 1)}val $currentCounter = null;
${indent(i + 1)}val $oldCounter = null;
${indent(i + 1)}${reduce(b, i + 1)}
$ind}
"""
      case SetCounterHaltIn(v) => rd"$currentCounter.haltHandler = ${closureNullary(v)}"
      case DecrCounter() in _ => rd"$currentCounter.decrementAndTestZero() $porcNum"
      case CallCounterHalt() in _ => rd"$currentCounter.haltHandler() $porcNum"
      //case CallParentCounterHalt() => rd"$currentCounter.callParentCounterHalt()"
      case MakeCounterTopLevel() in _ => rd"$currentCounter.makeCounterTopLevel()"

      case NewTerminator(k) in ctx => rd"{ val $currentTerminator = new Terminator();\n\n$ind${k in ctx} }"
      case GetTerminator() in _ => rd"$currentTerminator"
      case KillIn(a, b) => rd"""
    $currentTerminator.setIsKilled() match { $porcNum
      case Some(khs) => {
        try {
          ${reduce(a, i + 1)}
        } finally {
          khs.foreach(c => try {
            c()
          } catch {
            case _: KilledException => () // Ignore killed exceptions and go on to the next handler
            case e: StackOverflowError =>
              throw e
          })
        }
      }
      case None => {
        Logger.finest(s"Already killed terminator $${$currentTerminator}")
      }
    }
    ${reduce(b, i + 1)}
"""
      case Killed() in _ => rd"throw KilledException;"
      case CheckKilled() in _ => rd"if ($currentTerminator.isKilled) { throw KilledException }"
      case AddKillHandlerIn(u, (m: Var) in ctx) => rd"$u.addKillHandler(${closureNullary(m in ctx)})"
      case IsKilledIn(t) => rd"$t.isKilled()"

      case NewFuture() in _ => "new Future()"
      case ForceIn(vs, ctx, b) => rd"forceFutures(Seq[AnyRef](${vs.map(a => reduce(a in ctx, i)).mkString(", ")}), $b, $currentCounter, $currentTerminator) $porcNum"
      case ResolveIn(f, (b:Var) in ctx) => rd"resolveFuture($f, ${closureNullary(b in ctx)}, $currentCounter, $currentTerminator) $porcNum"
      case BindIn(f, v) => rd"$f.bind($v) $porcNum"
      case StopIn(f) => rd"$f.halt() $porcNum"

      case NewFlag() in _ => "new Flag()"
      case SetFlagIn(f) => rd"$f.set() $porcNum"
      case ReadFlagIn(f) => rd"$f.get $porcNum"

      case ExternalCallIn(s, args, ctx, (p: Var) in pctx) => 
        rd"invokeExternal(${reduceValue(s)}, Seq[AnyRef](${args.map(a => reduce(a in ctx, i)).mkString(", ")}), ${closureUnary(p in pctx)}, $currentCounter, $currentTerminator)"

      case Unit() in _ => "()"

      //case v if v.productArity == 0 => v.productPrefix

      case e => assert(false, s"Found object that I don't know how to handle in Scala: $e"); ???
    }
  }
}