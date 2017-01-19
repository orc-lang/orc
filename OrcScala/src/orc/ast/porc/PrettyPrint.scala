//
// PrettyPrint.scala -- Scala class/trait/object PrettyPrint
// Project OrcScala
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

/**
  * @author amp
  */
class PrettyPrint {
  def tag(ast: PorcAST, s: String): String = s"${ast.number.map(_ + ": ").getOrElse("")}$s"

  def indent(i: Int, n: Option[Int] = None) = {
    n match {
      case None => " " * i
      case Some(n) =>
        val sn = n.toString
        sn + ": " + (" " * (i - sn.size - 2))
    }
  }

  def reduce(ast: PorcAST, i: Int = 0): String = {
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
            case a: PorcAST => bldr append reduce(a, i)
            case _ => bldr append a
          }
          bldr append treatEscapes(pi.next())
        }
        bldr.toString
      }
    }

    val ind = indent(i)
    tag(ast,
      ast match {
        case OrcValue(v) => Format.formatValue(v)
        //case Tuple(l) => l.map(reduce(_, i+1)).mkString("(",", ",")")
        case v: Var => v.optionalVariableName.getOrElse(v.toString)

        case Let(x, v, b) => rd"let $x = ${reduce(v, i + 3)} in\n$ind$b"
        case DefDeclaration(l, b) => rd"def ${l.map(reduce(_, i + 3)).mkString(";\n" + indent(i + 2))}\n${indent(i + 2)} in\n$ind${reduce(b, i)}"
        case DefCPS(name, p, c, t, args, body) => rd"$name ($p, $c, $t)(${args.map(reduce(_, i)).mkString(", ")}) =\n$ind$body"
        case DefDirect(name, args, body) => rd"direct $name (${args.map(reduce(_, i)).mkString(", ")}) =\n$ind$body"

        case Continuation(arg, b) => rd"\u03BB($arg).\n$ind$b"

        case Call(t, a) => rd"$t ($a)"
        case SiteCall(target, p, c, t, args) => rd"sitecall $target ($p, $c, $t)(${args.map(reduce(_, i)).mkString(", ")})"
        case SiteCallDirect(target, args) => rd"sitecall direct $target (${args.map(reduce(_, i)).mkString(", ")})"
        case DefCall(target, p, c, t, args) => rd"defcall $target ($p, $c, $t)(${args.map(reduce(_, i)).mkString(", ")})"
        case DefCallDirect(target, args) => rd"defcall direct $target (${args.map(reduce(_, i)).mkString(", ")})"
        case IfDef(arg, f, g) => rd"ifdef $arg then\n${indent(i + 2)}${reduce(f, i + 2)}\n${ind}else\n${indent(i + 2)}${reduce(g, i + 2)}"

        case Sequence(es) => es.map(reduce(_, i)).mkString(s";\n$ind")

        case TryOnKilled(b, h) => rd"try\n${indent(i + 2)}${reduce(b, i + 2)}\n${ind}onKilled\n${indent(i + 2)}${reduce(h, i + 2)}"
        case TryOnHalted(b, h) => rd"try\n${indent(i + 2)}${reduce(b, i + 2)}\n${ind}onHalted\n${indent(i + 2)}${reduce(h, i + 2)}"
        case TryFinally(b, h) => rd"try\n${indent(i + 2)}${reduce(b, i + 2)}\n${ind}finally\n${indent(i + 2)}${reduce(h, i + 2)}"

        case Spawn(c, t, e) => rd"spawn $c $t {\n${indent(i + 2)}${reduce(e, i + 2)}\n$ind}"

        case NewCounter(c, h) => rd"counter $c { $h }"
        case Halt(c) => rd"halt $c"

        case NewTerminator(t) => rd"terminator $t"
        case Kill(t) => rd"kill $t"

        case SpawnFuture(c, t, pArg, cArg, e) => rd"spawnFuture $c $t ($pArg, $cArg) {\n${indent(i + 2)}${reduce(e, i + 2)}\n$ind}"

        case Force(p, c, t, b, vs) => rd"force[${if (b) "publish" else "call"}] $p $c $t (${vs.map(reduce(_, i)).mkString(", ")})"
        case TupleElem(v, i) => rd"elem($v, $i)"

        case GetField(p, c, t, o, f) => rd"getField $p $c $t $o$f"

        case v if v.productArity == 0 => v.productPrefix

        case v => throw new NotImplementedError("Cannot convert: " + v.getClass.toString)
      })
  }
}
