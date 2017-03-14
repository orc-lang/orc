//
// PrettyPrint.scala -- Scala class/trait/object PrettyPrint
// Project OrcScala
//
// Created by amp on May 28, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.values.Format
import orc.values.Field
import orc.util.PrettyPrintInterpolator
import orc.util.FragmentAppender

/** @author amp
  */
class PrettyPrint {
  class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
    implicit def implicitInterpolator(sc: StringContext) = new MyInterpolator(sc)
    class MyInterpolator(sc: StringContext) extends Interpolator(sc) {
      override val processValue: PartialFunction[Any, FragmentAppender] = {
        case a: Expr =>
          reduce(a)
      }
    }
  }
  val interpolator = new MyPrettyPrintInterpolator
  import interpolator._

  def tag(ast: PorcAST, s: FragmentAppender): FragmentAppender = s // pp"${ast.number.map(_ + ": ").getOrElse("")}$s"

  def reduce(ast: PorcAST): FragmentAppender = {
    tag(ast,
      ast match {
        case OrcValue(v) => FragmentAppender(Format.formatValue(v))
        case v: Var => FragmentAppender(v.optionalVariableName.getOrElse(v.toString))

        case Let(x, v, b) => pp"let $x = $StartIndent$v$EndIndent in\n$b"
        case DefDeclaration(l, b) => pp"def $StartIndent$StartIndent${FragmentAppender.mkString(l.map(reduce), ";\n")}$EndIndent in\n$b$StartIndent"
        case DefCPS(name, p, c, t, args, body) => pp"$name ($p, $c, $t)(${args.map(reduce(_)).mkString(", ")}) =$StartIndent\n$body$EndIndent"
        case DefDirect(name, args, body) => pp"direct $name (${args.map(reduce(_)).mkString(", ")}) =$StartIndent\n$body$EndIndent"

        case Continuation(arg, b) => pp"\u03BB($arg).$StartIndent\n$b$EndIndent"

        case Call(t, a) => pp"$t ($a)"
        case SiteCall(target, p, c, t, args) => pp"sitecall $target ($p, $c, $t)(${args.map(reduce(_)).mkString(", ")})"
        case SiteCallDirect(target, args) => pp"sitecall direct $target (${args.map(reduce(_)).mkString(", ")})"
        case DefCall(target, p, c, t, args) => pp"defcall $target ($p, $c, $t)(${args.map(reduce(_)).mkString(", ")})"
        case DefCallDirect(target, args) => pp"defcall direct $target (${args.map(reduce(_)).mkString(", ")})"
        case IfDef(arg, f, g) => pp"ifdef $arg then$StartIndent\n$f$EndIndent\nelse$StartIndent\n$g$EndIndent"

        case Sequence(es) => FragmentAppender.mkString(es.map(reduce(_)), ";\n")

        case TryOnKilled(b, h) => pp"try$StartIndent\n$b$EndIndent\nonKilled$StartIndent\n$h$EndIndent"
        case TryOnHalted(b, h) => pp"try$StartIndent\n$b$EndIndent\nonHalted$StartIndent\n$h$EndIndent"
        case TryFinally(b, h) => pp"try$StartIndent\n$b$EndIndent\nfinally$StartIndent\n$h$EndIndent"

        case Spawn(c, t, e) => pp"spawn $c $t {$StartIndent\n$e$EndIndent\n}"

        case NewCounter(c, h) => pp"counter $c { $StartIndent$h$EndIndent }"
        case Halt(c) => pp"halt $c"
        case SetDiscorporate(c) => pp"setDiscorporate $c"

        case NewTerminator(t) => pp"terminator $t"
        case Kill(t) => pp"kill $t"

        case NewFuture() => pp"newFuture"
        case SpawnBindFuture(f, c, t, pArg, cArg, e) => pp"spawnBindFuture $f $c $t ($pArg, $cArg) {$StartIndent\n$e$EndIndent\n}"

        case Force(p, c, t, b, vs) => pp"force[${if (b) "publish" else "call"}] $p $c $t (${vs.map(reduce(_)).mkString(", ")})"
        case TupleElem(v, i) => pp"elem($v, $i)"

        case GetField(p, c, t, o, f) => pp"getField $p $c $t $o$f"

        case New(bindings) => {
          def reduceField(f: (Field, Expr)) = {
            val (name, expr) = f
            pp"${name} = ${reduce(expr)}"
          }
          def fields = pp" #$StartIndent\n${FragmentAppender.mkString(bindings.map(reduceField), " #\n")}$EndIndent\n"
          pp"new { ${if (bindings.nonEmpty) fields else ""} }"
        }

        case Unit() => FragmentAppender("unit")

        //case v if v.productArity == 0 => v.productPrefix

        // case v => throw new NotImplementedError("Cannot convert: " + v.getClass.toString)
      })
  }
}
