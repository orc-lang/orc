//
// PrettyPrint.scala -- Scala class PrettyPrint
// Project OrcScala
//
// $Id: PrettyPrint.scala 3182 2013-02-19 01:23:40Z jthywissen $
//
// Created by dkitchin on Jun 7, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

import scala.collection.mutable._
import orc.values.Format
import orc.values.sites.Site

/** Nicer printing for named OIL syntax trees.
  *
  * @author dkitchin
  */

class PrettyPrint {

  val vars: Map[BoundVar, String] = new HashMap()
  var varCounter: Int = 0
  def newVarName(): String = { varCounter += 1; "`t" + varCounter }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName())

  val typevars: Map[BoundTypevar, String] = new HashMap()
  var typevarCounter: Int = 0
  def newTypevarName(): String = { typevarCounter += 1; "`T" + typevarCounter }
  def lookup(temp: BoundTypevar) = typevars.getOrElseUpdate(temp, newVarName())

  def commasep(l: List[Orc5CAST]): String = l match {
    case Nil => ""
    case x :: Nil => reduce(x)
    case x :: y => y.foldLeft(reduce(x))({ _ + ", " + reduce(_) })
  }

  def brack(l: List[Orc5CAST]): String = "[" + commasep(l) + "]"
  def paren(l: List[Orc5CAST]): String = "(" + commasep(l) + ")"

  /*def tag(ast: Orc5CAST, s: String) = {
    def ic(c : String, b : Boolean) = if(b) c else ""
    ast match {
    case e : Expression => 
      val range = if( e.publications != (0, None) ) e.publications else "" 
      s"[${ic("~", e.immediateHalt)}${ic("!", e.immediatePublish)}$range$s]" 
    case _ => s
    } 
  }*/
  
  def tag(ast: Orc5CAST, s: String) : String = s
  
  def reduce(ast: Orc5CAST): String = {
    tag(ast, 
    ast match {
      case Stop() => "stop"
      case Call(target, args, typeargs) => {
        reduce(target) +
          (typeargs match {
            case Some(ts) => brack(ts)
            case None => ""
          }) +
          paren(args)
      }
      case left || right => "(" + reduce(left) + " | " + reduce(right) + ")"
      case Sequence(left, x, right) => "(" + reduce(left) + " >" + reduce(x) + "> " + reduce(right) + ")"
      case LateBind(left, x, right) => "(" + reduce(left) + " <" + reduce(x) + "<| " + reduce(right) + ")"
      case left ow right => "(" + reduce(left) + " ; " + reduce(right) + ")"
      case Limit(expr) => "limit(" + reduce(expr) + ")"
      case DeclareDefs(defs, body) => "\n" + (defs map reduce).foldLeft("")({ _ + _ }) + reduce(body)
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
      case HasType(body, expectedType) => "(" + reduce(body) + " :: " + reduce(expectedType) + ")"
      case DeclareType(u, t, body) => "type " + reduce(u) + " = " + reduce(t) + "\n" + reduce(body)
      case VtimeZone(timeOrder, body) => "VtimeZone(" + reduce(timeOrder) + ", " + reduce(body) + ")"
      case Constant(v) => Format.formatValue(v)
      case (x: BoundVar) => x.optionalVariableName.getOrElse(lookup(x))
      case UnboundVar(s) => "?" + s
      case u: BoundTypevar => u.optionalVariableName.getOrElse(lookup(u))
      case UnboundTypevar(s) => "?" + s
      case Top() => "Top"
      case Bot() => "Bot"
      case FunctionType(typeformals, argtypes, returntype) => {
        "lambda" + brack(typeformals) + paren(argtypes) + " :: " + reduce(returntype)
      }
      case TupleType(elements) => paren(elements)
      case TypeApplication(tycon, typeactuals) => reduce(tycon) + brack(typeactuals)
      case AssertedType(assertedType) => reduce(assertedType) + "!"
      case TypeAbstraction(typeformals, t) => brack(typeformals) + "(" + reduce(t) + ")"
      case ImportedType(classname) => classname
      case ClassType(classname) => classname
      case VariantType(_, typeformals, variants) => {
        val variantSeq =
          for ((name, variant) <- variants) yield {
            name + "(" + (variant map reduce).mkString(",") + ")"
          }
        brack(typeformals) + "(" + variantSeq.mkString(" | ") + ")"
      }
      case _ => "???"
    })
  }
}

class PrettyPrintWithAnalysis(analyzer : ExpressionAnalysisStore) extends PrettyPrint {
  override def tag(ast: Orc5CAST, s: String) : String = {
    import analyzer.ImplicitResults._
    def ic(c : String, b : Boolean) = if(b) c else ""
    ast match {
      case _: Constant | _: Var => s
      case e: Expression =>
        val range = if (e.publications != (0, None)) { val (l, u) = e.publications; l.toString + "-" + (u map (_.toString) getOrElse "inf") } else ""
        val tag = s"${ic("~", e.immediateHalt)}${ic("!", e.immediatePublish)}${ic("*", e.effectFree)}$range${e.strictOnSet.mkString("", ",", "")}"
        if (tag.isEmpty)
          s
        else
          s"[$tag]$s"
      case _ => s
    } 
  }
}

