//
// PrettyPrint.scala -- Scala class PrettyPrint
// Project OrcScala
//
// Created by dkitchin on Jun 7, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

import scala.collection.mutable.{ HashMap, Map }

import orc.values._

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

  def commasep(l: List[NamedAST]): String = l match {
    case Nil => ""
    case x :: Nil => reduce(x)
    case x :: y => y.foldLeft(reduce(x))({ _ + ", " + reduce(_) })
  }

  def brack(l: List[NamedAST]): String = "[" + commasep(l) + "]"
  def paren(l: List[NamedAST]): String = "(" + commasep(l) + ")"

  def reduce(ast: NamedAST): String =
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
      case Parallel(left, right) => "(" + reduce(left) + " | " + reduce(right) + ")"
      case Sequence(left, x, right) => "(" + reduce(left) + " >" + reduce(x) + "> " + reduce(right) + ")"
      case Graft(x, value, body) => "(val " + reduce(x) + " = " + reduce(value) + " # " + reduce(body) + ")"
      case Trim(f) => "{| " + reduce(f) + " |}"
      case left ow right => "(" + reduce(left) + " ; " + reduce(right) + ")"
      case DeclareCallables(defs, body) => "\n" + (defs map reduce).foldLeft("")({ _ + _ }) + reduce(body)
      case c @ Callable(f, formals, body, typeformals, argtypes, returntype) => {
        val prefix = c match {
          case _: Def => "def"
          case _: Site => "site"
        }
        val name = f.optionalVariableName.getOrElse(lookup(f))
        prefix + " " + name + brack(typeformals) + paren(argtypes.getOrElse(Nil)) +
          (returntype match {
            case Some(t) => " :: " + reduce(t)
            case None => ""
          }) +
          "\n" +
          prefix + " " + name + paren(formals) + " = " + reduce(body) +
          "\n"
      }
      case New(os) => "new " + os.map(reduce).mkString("(", ",", ")") + ""
      case FieldAccess(obj, f) => s"${reduce(obj)}${f}"
      case Classvar(name) => reduce(name)
      case DeclareClasses(clss, body) => (clss map reduce).mkString("\n", "\n", "\n") + reduce(body)
      case Class(name, self, supr, fields, linearization) => {
        def reduceField(f: (Field, Expression)) = {
          val (name, expr) = f
          s"${name} = ${reduce(expr)}"
        }
        val superDesc = if (supr.optionalVariableName != Some("super")) s"${reduce(supr)} = super #" else ""
        s"class $name (${linearization.map(reduce).mkString(",")}) { ${reduce(self)} # $superDesc ${fields.map(reduceField).mkString(" # ")} }"
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
    }

}
