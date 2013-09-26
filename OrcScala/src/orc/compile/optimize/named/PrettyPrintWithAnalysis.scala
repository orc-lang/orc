//
// PrettyPrintWithAnalysis.scala -- Scala class/trait/object PrettyPrintWithAnalysis
// Project OrcScala
//
// $Id$
//
// Created by amp on Sep 25, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.optimize.named

import orc.ast.oil.named._
import orc.values.Format

class PrettyPrintWithAnalysis(analyzer : ExpressionAnalysisProvider[Expression]) extends PrettyPrint {
  def mkTag(ast: WithContext[NamedAST]) : String = {
    import analyzer.ImplicitResults._
    def ic(c : String, b : Boolean) = if(b) c else ""
    ast match {
      case (_: Constant) in _ => ""
      case e : WithContext[Expression] if e.e.isInstanceOf[Expression] => // Ugly hack to reify check
        val range = if (e.publications != Range(0, None)) { val Range(l, u) = e.publications; l.toString + "-" + (u map (_.toString) getOrElse "inf") } else ""
        val tag = s"${ic("~", e.immediateHalt)}${ic("!", e.immediatePublish)}${ic("*", e.effectFree)}$range;${e.strictOnSet.mkString("", ",", "")};${e.forcesSet.mkString("", ",", "")}"
        if (tag == ";;")
          ""
        else
          s"[$tag]"
      case _ => ""
    } 
  }
  def reduce(ast: WithContext[NamedAST]): String = {
    import WithContext._
    mkTag(ast) + (ast match {
      case Stop() in _ => "stop"
      case CallAt(target, args, typeargs, ctx) => {
        reduce(target) +
          (typeargs match {
            case Some(ts) => brack(ts)
            case None => ""
          }) +
          paren(args)
      }
      case left || right => "(" + reduce(left) + " | " + reduce(right) + ")"
      case left > x > right => "(" + reduce(left) + " >" + reduce(x) + "> " + reduce(right) + ")"
      case left < x <| right => "(" + reduce(left) + " <" + reduce(x) + "<| " + reduce(right) + ")"
      case left ow right => "(" + reduce(left) + " ; " + reduce(right) + ")"
      case LimitAt(expr) => "limit(" + reduce(expr) + ")"
      case DeclareDefsAt(defs, ctx, body) => "\n" + (defs map {(x) => reduce(x in ctx)}).foldLeft("")({ _ + _ }) + reduce(body)
      case DefAt(f, formals, body, typeformals, argtypes, returntype, ctx) => {
        val name = f.optionalVariableName.getOrElse(lookup(f)) // "@" + f.##.toString
        "def " + name + brack(typeformals) + paren(argtypes.getOrElse(Nil)) +
          (returntype match {
            case Some(t) => " :: " + reduce(t)
            case None => ""
          }) +
          "\n" +
          "def " + name + paren(formals) + " = " + reduce(body) +
          "#\n"
      }
      case HasType(body, expectedType) in ctx => "(" + reduce(body in ctx) + " :: " + reduce(expectedType in ctx) + ")"
      case DeclareTypeAt(u, t, body) => "type " + reduce(u) + " = " + reduce(t) + "\n" + reduce(body)
      case VtimeZone(timeOrder, body) in ctx => "VtimeZone(" + reduce(timeOrder in ctx) + ", " + reduce(body in ctx) + ")"
      case Constant(v) in _ => Format.formatValue(v)
      case (x: BoundVar) in _ => x.optionalVariableName.getOrElse(lookup(x)) //+ "@" + x.##.toString
      case UnboundVar(s) in _ => "?" + s
      case (u: BoundTypevar) in _ => u.optionalVariableName.getOrElse(lookup(u))
      case UnboundTypevar(s) in _ => "?" + s
      case Top() in _ => "Top"
      case Bot() in _ => "Bot"
      case FunctionType(typeformals, argtypes, returntype) in _ => {
        "lambda" + brack(typeformals) + paren(argtypes) + " :: " + reduce(returntype)
      }
      case TupleType(elements) in _ => paren(elements)
      case TypeApplication(tycon, typeactuals) in _ => reduce(tycon) + brack(typeactuals)
      case AssertedType(assertedType) in _ => reduce(assertedType) + "!"
      case TypeAbstraction(typeformals, t) in _ => brack(typeformals) + "(" + reduce(t) + ")"
      case ImportedType(classname) in _ => classname
      case ClassType(classname) in _ => classname
      case VariantType(_, typeformals, variants) in _ => {
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