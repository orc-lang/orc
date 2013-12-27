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

/**
  *
  * @author amp
  */
class PrettyPrint {
  def tag(ast: PorcAST, s: String) : String = s"${ast.number.map(_+": ").getOrElse("")}$s"
  
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
            case a : PorcAST => bldr append reduce(a, i)
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
      case v : Var => v.optionalVariableName.getOrElse(v.toString)
      
      case Let(x, v, b) => rd"let $x = ${reduce(v, i+3)} in\n$ind$b"
      case Site(l, b) => rd"site ${l.map(reduce(_, i+3)).mkString(";\n"+indent(i+2))}\n${indent(i+2)} in\n$ind${reduce(b, i)}"
      case SiteDef(name, args, p, body) => rd"$name (${args.map(reduce(_, i)).mkString(", ")}) $p =\n$ind$body"
      
      case Lambda(args, b) => rd"\u03BB(${args.map(reduce(_, i)).mkString(", ")}).\n$ind$b"

      case Call(t, a) => rd"$t (${a.map(reduce(_, i)).mkString(", ")})"
      case SiteCall(t, a, p) => rd"sitecall $t (${a.map(reduce(_, i)).mkString(", ")}) $p"
      case DirectSiteCall(t, a) => rd"directsitecall $t (${a.map(reduce(_, i)).mkString(", ")})"
      
      //case Project(n, v) => rd"project_$n $v"
      
      case Sequence(es) => es.map(reduce(_, i)).mkString(s";\n$ind")
      
      case If(b, t, e) => rd"if $b then\n${indent(i+2)}${reduce(t, i+2)}\n${ind}else\n${indent(i+2)}${reduce(e, i+2)}"
      case TryOnKilled(b, h) => rd"try\n${indent(i+2)}${reduce(b, i+2)}\n${ind}onKilled\n${indent(i+2)}${reduce(h, i+2)}"
      case TryOnHalted(b, h) => rd"try\n${indent(i+2)}${reduce(b, i+2)}\n${ind}onHalted\n${indent(i+2)}${reduce(h, i+2)}"
      
      case Spawn(v) => rd"spawn $v"
        
      case NewCounter(k) => rd"counter in\n$ind$k"
      //case NewCounterDisconnected(k) => rd"counter disconnected in\n$ind$k"
      case RestoreCounter(a, b) => rd"restoreCounter {\n${indent(i+1)}${reduce(a, i+1)}\n$ind}{\n${indent(i+1)}${reduce(b, i+1)}\n$ind}"
      case SetCounterHalt(v) => rd"setCounterHalt $v"
      case DecrCounter() => "decrCounter"
      case CallCounterHalt() => "callCounterHalt"
      case CallParentCounterHalt() => "callParentCounterHalt"

      case NewTerminator(k) => rd"terminator in\n$ind$k"
      case GetTerminator() => "getTerminator"
      case Kill(a, b) => rd"kill {\n${indent(i+1)}${reduce(a, i+1)}\n$ind}{\n${indent(i+1)}${reduce(b, i+1)}\n$ind}"
      case Killed() => "killed"
      case CheckKilled() => "checkKilled"
      case AddKillHandler(u, m) => rd"addKillHandler $u $m"
      case IsKilled(t) => rd"isKilled $t"

      case NewFuture() => "newFuture"
      case Force(vs, b) => rd"force (${vs.map(reduce(_, i)).mkString(", ")}) $b"
      case Resolve(f, b) => rd"resolve $f $b"
      case Bind(f, v) => rd"bind $f $v"
      case Stop(f) => rd"stop $f"
      
      case NewFlag() => "newFlag"
      case SetFlag(f) => rd"setFlag $f"
      case ReadFlag(f) => rd"readFlag $f"

      case ExternalCall(s, args, p) => rd"external $s (${args.map(reduce(_, i)).mkString(", ")}) $p"

      case v if v.productArity == 0 => v.productPrefix

      case _ => ???
    })
  }
}