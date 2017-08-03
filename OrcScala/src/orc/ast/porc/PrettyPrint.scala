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
class PrettyPrint(includeNestedCode: Boolean = true) {
  class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
    implicit def implicitInterpolator(sc: StringContext) = new MyInterpolator(sc)
    class MyInterpolator(sc: StringContext) extends Interpolator(sc) {
      override val processValue: PartialFunction[Any, FragmentAppender] = {
        case a: Expression =>
          reduce(a)
      }
    }
  }
  val interpolator = new MyPrettyPrintInterpolator
  import interpolator._

  def tag(ast: PorcAST, s: FragmentAppender): FragmentAppender = s // pp"${ast.number.map(_ + ": ").getOrElse("")}$s"

  def nestedCode(ast: PorcAST): FragmentAppender = {
     if (includeNestedCode)
       reduce(ast)
     else
       pp"[...]"
  }
  
  def reduce(ast: PorcAST): FragmentAppender = {
    tag(ast,
      ast match {
        case Constant(v) => FragmentAppender(Format.formatValue(v))
        case v: Variable => FragmentAppender(v.optionalVariableName.getOrElse(v.toString))

        case Let(x, v, b) => pp"let $x = $StartIndent$v$EndIndent in\n$b"
        case MethodDeclaration(t, l, b) => pp"def($t) $StartIndent${FragmentAppender.mkString(l.map(reduce), ";\n")}$EndIndent in\n$b"
        case MethodCPS(name, p, c, t, isDef, args, body) => pp"cps${if (isDef) "_d" else "_s"} $name ($p, $c, $t)(${args.map(reduce(_)).mkString(", ")}) =$StartIndent\n${nestedCode(body)}$EndIndent"
        case MethodDirect(name, isDef, args, body) => pp"direct${if (isDef) "_d" else "_s"} $name (${args.map(reduce(_)).mkString(", ")}) =$StartIndent\n${nestedCode(body)}$EndIndent"

        case Continuation(args, b) => pp"\u03BB(${args.map(reduce(_)).mkString(", ")}).$StartIndent\n${nestedCode(b)}$EndIndent"

        case CallContinuation(t, args) => pp"$t (${args.map(reduce(_)).mkString(", ")})"
        case MethodCPSCall(isExt, target, p, c, t, args) => pp"call cps $isExt $target ($p, $c, $t)(${args.map(reduce(_)).mkString(", ")})"
        case MethodDirectCall(isExt, target, args) => pp"call direct $isExt $target (${args.map(reduce(_)).mkString(", ")})"
        case IfLenientMethod(arg, f, g) => pp"iflenient $arg then$StartIndent\n$f$EndIndent\nelse$StartIndent\n$g$EndIndent"

        case Sequence(es) => FragmentAppender.mkString(es.map(reduce(_)), ";\n")

        case TryOnException(b, h) => pp"try$StartIndent\n$b$EndIndent\ncatch$StartIndent\n$h$EndIndent"
        case TryFinally(b, h) => pp"try$StartIndent\n$b$EndIndent\nfinally$StartIndent\n$h$EndIndent"

        case Spawn(c, t, b, e) => pp"spawn_${ if (b) "must" else "may" } $c $t $e"

        case NewCounter(c, h) => pp"counter $c $h"
        case HaltToken(c) => pp"haltToken $c"
        case NewToken(c) => pp"newToken $c"
        case SetDiscorporate(c) => pp"setDiscorporate $c"

        case NewTerminator(t) => pp"terminator $t"
        case Kill(t) => pp"kill $t"
        case CheckKilled(t) => pp"checkKilled $t"

        case NewFuture() => pp"newFuture"
        case Bind(f, v) => pp"bind $f $v"
        case BindStop(f) => pp"bind $f stop"

        case Force(p, c, t, vs) => pp"force $p $c $t (${vs.map(reduce(_)).mkString(", ")})"
        case Resolve(p, c, t, vs) => pp"resolve $p $c $t (${vs.map(reduce(_)).mkString(", ")})"

        case GetField(o, f) => pp"$o$f"
        case GetMethod(o) => pp"method $o"

        case New(bindings) => {
          def reduceField(f: (Field, Expression)) = {
            val (name, expr) = f
            pp"${name} = ${reduce(expr)}"
          }
          def fields = pp" #$StartIndent\n${FragmentAppender.mkString(bindings.map(reduceField), " #\n")}$EndIndent\n"
          pp"new { ${if (bindings.nonEmpty) fields else ""} }"
        }

        case PorcUnit() => FragmentAppender("unit")

        //case v if v.productArity == 0 => v.productPrefix

        // case v => throw new NotImplementedError("Cannot convert: " + v.getClass.toString)
      })
  }
}
