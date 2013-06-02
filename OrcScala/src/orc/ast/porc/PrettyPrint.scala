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
  def tag(ast: PorcAST, s: String) : String = s
  
  def indent(i: Int) = " " * i
  
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
      case Constant(v) => Format.formatValue(v)
      case Tuple(l) => l.map(reduce(_, i+1)).mkString("(",", ",")")
      case v : Var => v.optionalVariableName.getOrElse(v.toString)
      
      case Let(l, b) => s"let ${l.map(reduce(_, i+3)).mkString(";\n"+indent(i+2))}\n${indent(i+2)} in\n$ind${reduce(b, i)}"
      case Site(l, b) => s"site ${l.map(reduce(_, i+3)).mkString(";\n"+indent(i+2))}\n${indent(i+2)} in\n$ind${reduce(b, i)}"
      case ClosureDef(name, args, body) => rd"$name(${args.map(reduce(_, i)).mkString(", ")}) =\n$ind$body"
      case SiteDef(name, args, body) => rd"$name(${args.map(reduce(_, i)).mkString(", ")}) =\n$ind$body"

      case ClosureCall(t, a) => rd"$t $a"
      case SiteCall(t, a) => rd"sitecall $t $a"
      
      case Unpack(vars, v, k) => rd"unpack ${vars.map(reduce(_,i)).mkString("(",", ",")")} = $v in\n$ind$k"
      
      case Spawn(v, k) => rd"spawn $v\n$ind$k"
      case Die() => "die"
        
      case NewCounter(k) => rd"counter in\n$ind$k"
      case RestoreCounter(a, b) => rd"restoreCounter {\n${indent(i+1)}${reduce(a, i+1)}\n$ind}{\n${indent(i+1)}${reduce(b, i+1)}\n$ind}"
      case SetCounterHalt(v, k) => rd"setCounterHalt $v\n$ind$k"
      case GetCounterHalt(x, k) => rd"getCounterHalt $x in\n$ind$k"

      case NewTerminator(k) => rd"terminator in\n$ind$k"
      case GetTerminator(x, k) => rd"getTerminator $x in\n$ind$k"
      case Kill(a, b) => rd"kill {\n${indent(i+1)}${reduce(a, i+1)}\n$ind}{\n${indent(i+1)}${reduce(b, i+1)}\n$ind}"
      case IsKilled(a, b) => rd"isKilled { ${reduce(a, i+2)} }\n$ind$b"      
      case AddKillHandler(u, m, k) => rd"addKillHandler $u $m\n$ind$k"
      case CallKillHandlers(k) => rd"callKillHandlers\n$ind$k"

      case NewFuture(x, k) => rd"future $x in\n$ind$k"
      case Force(vs, a, b) => rd"force $vs $a $b"
      case Bind(f, v, k) => rd"bind $f $v\n$ind$k"
      case Stop(f, k) => rd"stop $f\n$ind$k"
      
      case NewFlag(x, k) => rd"flag $x in\n$ind$k"
      case SetFlag(f, k) => rd"setFlag $f\n$ind$k"
      case ReadFlag(f, a, b) => rd"readFlag $f {\n${indent(i+1)}${reduce(a, i+1)}\n$ind}{\n${indent(i+1)}${reduce(b, i+1)}\n$ind}"

      case ExternalCall(s, args, h, p) => rd"external $s $args $h $p"
      
      case _ => ???
    })
  }
}