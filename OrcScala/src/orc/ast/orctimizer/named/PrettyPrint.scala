//
// PrettyPrint.scala -- Scala class PrettyPrint
// Project OrcScala
//
// Created by dkitchin on Jun 7, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.orctimizer.named

import scala.collection.mutable._
import orc.values.Format
import orc.compile.orctimizer.ExpressionAnalysisProvider

/** Nicer printing for named OIL syntax trees.
  *
  * @author dkitchin, amp
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

  def commasep(l: List[NamedAST]): String = l match {
    case Nil => ""
    case x :: Nil => reduce(x)
    case x :: y => y.foldLeft(reduce(x))({ _ + ", " + reduce(_) })
  }

  def brack(l: List[NamedAST]): String = "[" + commasep(l) + "]"
  def paren(l: List[NamedAST]): String = "(" + commasep(l) + ")"

  def reduce(ast: NamedAST): String = {
    val exprStr = ast match {
      case Stop() => "stop"
      case CallDef(target, args, typeargs) => {
        "defcall " + reduce(target) +
          (typeargs match {
            case Some(ts) => brack(ts)
            case None => ""
          }) +
          paren(args)
      }
      case CallSite(target, args, typeargs) => {
        "sitecall " + reduce(target) +
          (typeargs match {
            case Some(ts) => brack(ts)
            case None => ""
          }) +
          paren(args)
      }
      case left || right => "(" + reduce(left) + " | " + reduce(right) + ")"
      case Branch(left, x, right) => "(" + reduce(left) + " >" + reduce(x) + "> " + reduce(right) + ")"
      case Trim(f) => "{|" + reduce(f) + "|}"
      case Future(x, f, g) => "future " + reduce(x) + " = " + reduce(f) + " #\n" + reduce(g)
      case Force(xs, vs, b, e) => s"force_${if(b) "p" else "c"} ${commasep(xs)} = ${commasep(vs)} #\n${reduce(e)}"
      case left Otherwise right => "(" + reduce(left) + " ; " + reduce(right) + ")"
      case IfDef(a, l, r) => s"ifdef ${reduce(a)} then\n  ${reduce(l)}\nelse\n  ${reduce(r)}"
      case DeclareCallables(defs, body) => "\n" + (defs map reduce).foldLeft("")({ _ + _ }) + reduce(body)
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
      case Site(f, formals, body, typeformals, argtypes, returntype) => {
        val name = f.optionalVariableName.getOrElse(lookup(f))
        "site " + name + brack(typeformals) + paren(argtypes.getOrElse(Nil)) +
          (returntype match {
            case Some(t) => " :: " + reduce(t)
            case None => ""
          }) +
          "\n" +
          "site " + name + paren(formals) + " = " + reduce(body) +
          "\n"
      }
      case HasType(body, expectedType) => "(" + reduce(body) + " :: " + reduce(expectedType) + ")"
      case DeclareType(u, t, body) => "type " + reduce(u) + " = " + reduce(t) + "\n" + reduce(body)
      case VtimeZone(timeOrder, body) => "VtimeZone(" + reduce(timeOrder) + ", " + reduce(body) + ")"
      case FieldAccess(o, f) => reduce(o) + "." + f.field
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
    }
    exprStr
  }
}
