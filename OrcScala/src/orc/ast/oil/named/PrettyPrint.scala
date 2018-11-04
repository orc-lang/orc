//
// PrettyPrint.scala -- Scala class PrettyPrint
// Project OrcScala
//
// Created by dkitchin on Jun 7, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

import scala.collection.mutable.{ HashMap, Map }

import orc.values._
import orc.util.PrettyPrintInterpolator
import orc.util.FragmentAppender

/** Nicer printing for named OIL syntax trees.
  *
  * @author dkitchin, amp
  */
class PrettyPrint {
  class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
    implicit def implicitInterpolator(sc: StringContext) = new MyInterpolator(sc)
    class MyInterpolator(sc: StringContext) extends Interpolator(sc) {
      override val processValue: PartialFunction[Any, FragmentAppender] = {
        case a: NamedAST =>
          reduce(a)
      }
    }
  }
  val interpolator = new MyPrettyPrintInterpolator
  import interpolator._

  val vars: Map[BoundVar, String] = new HashMap()
  var varCounter: Int = 0
  def newVarName(): String = {
    varCounter += 1
    val s = "`t" + varCounter
    //Logger.log(Level.FINE, s"Unnamed variable printed with name $s", new Exception(s).fillInStackTrace())
    s
  }
  def lookup(temp: BoundVar) = vars.getOrElseUpdate(temp, newVarName())

  val typevars: Map[BoundTypevar, String] = new HashMap()
  var typevarCounter: Int = 0
  def newTypevarName(): String = { typevarCounter += 1; "`T" + typevarCounter }
  def lookup(temp: BoundTypevar) = typevars.getOrElseUpdate(temp, newVarName())

  def commasep(l: List[NamedAST]): FragmentAppender = {
    FragmentAppender.mkString(l.map(reduce), ", ")
  }

  def reduce(ast: NamedAST): FragmentAppender =
    ast match {
      case Stop() => pp"stop"
      case Call(target, args, typeargs) => {
        val typePar = typeargs match {
            case Some(ts) => pp"[${commasep(ts)}]"
            case None => ""
          }
        pp"$target$typePar(${commasep(args)})"
      }
      case Parallel(left, right) => pp"($left | $right)"
      case Sequence(left, x, right) => pp"$left >$x>\n$right"
      case Graft(x, value, body) => pp"val $x = $StartIndent\n$value$EndIndent #\n$body"
      case Trim(f) => pp"{| $f |}"
      case Otherwise(left, right) => pp"($left ; $right)"
      case DeclareCallables(defs, body) => pp"-- mutual\n${FragmentAppender.mkString(defs.map(reduce))}\n$body"
      case c @ Callable(f, formals, body, typeformals, argtypes, returntype) => {
        val prefix = c match {
          case _: Def => "def"
          case _: Site => "site"
        }
        val name = f.optionalVariableName.getOrElse(lookup(f))
        val rettype =           (returntype match {
            case Some(t) => pp" :: $t"
            case None => pp""
          })

        pp"""$prefix $name[${commasep(typeformals)}](${commasep(argtypes.getOrElse(Nil))})$rettype
            "$prefix $name(${commasep(formals)}) = $StartIndent$body$EndIndent
          |"""
      }
      case New(self, st, bindings, t) => {
        def reduceField(f: (Field, Expression)) = {
          val (name, expr) = f
          pp"$name = $StartIndent$expr$EndIndent"
        }
        def fields = pp" #$StartIndent\n${FragmentAppender.mkString(bindings.map(reduceField), " #\n")}$EndIndent\n"
        pp"new ${t.map(reduce).getOrElse("")} { $self ${() => st.map(t => pp": $t").getOrElse(pp"")} ${() => if (bindings.nonEmpty) fields else pp""} }"
      }
      case FieldAccess(obj, f) => pp"$obj${f}"
      case HasType(body, expectedType) => pp"($body :: $expectedType)"
      case DeclareType(u, t, body) => pp"type $u = $t\n$body"
      case VtimeZone(timeOrder, body) => pp"VtimeZone($timeOrder, $body)"
      case Constant(v) => FragmentAppender(Format.formatValue(v))
      case (x: BoundVar) => FragmentAppender(x.optionalVariableName.getOrElse(lookup(x)))
      case UnboundVar(s) => pp"?$s"
      case u: BoundTypevar => FragmentAppender(u.optionalVariableName.getOrElse(lookup(u)))
      case UnboundTypevar(s) => pp"?$s"
      case Top() => FragmentAppender("Top")
      case Bot() => FragmentAppender("Bot")
      case FunctionType(typeformals, argtypes, returntype) => {
        pp"lambda[${commasep(typeformals)}](${commasep(argtypes)}) :: $returntype"
      }
      case TupleType(elements) => pp"(${commasep(elements)})"
      case TypeApplication(tycon, typeactuals) => pp"$tycon[${commasep(typeactuals)}]"
      case AssertedType(assertedType) => pp"$assertedType!"
      case TypeAbstraction(typeformals, t) => pp"[${commasep(typeformals)}]($t)"
      case ImportedType(classname) => FragmentAppender(classname)
      case ClassType(classname) => FragmentAppender(classname)
      case VariantType(_, typeformals, variants) => {
        val variantSeq =
          for ((name, variant) <- variants) yield {
            pp"$name(${commasep(variant)})"
          }
        pp"[${commasep(typeformals)}](${FragmentAppender.mkString(variantSeq, " | ")})"
      }
      case IntersectionType(a, b) => pp"$a & $b"
      case UnionType(a, b) => pp"$a | $b"
      case NominalType(t) => pp"nominal[$t]"
      case RecordType(mems) => {
        val m = FragmentAppender.mkString(mems.mapValues(reduce).map(p => pp"${p._1} :: ${p._2}"), " # ")
        pp"{. $m .}"
      }
      case StructuralType(mems) => {
        val m = FragmentAppender.mkString(mems.mapValues(reduce).map(p => pp"${p._1} :: ${p._2}"), " # ")
        pp"{ $m }"
      }
      case Hole(context, typecontext) => pp"HOLE[${typecontext.mapValues(reduce).map(p => pp"${p._1} = ${p._2}").mkString(" # ")}](${context.mapValues(reduce).map(p => pp"${p._1} = ${p._2}").mkString(" # ")})"
    }

}
