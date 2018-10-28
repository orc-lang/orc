//
// ValueSetAnalysis.scala -- Scala class ValueSetAnalysis
// Project OrcScala
//
// Created by jthywiss on Oct 9, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.distrib

import orc.ast.hasOptionalVariableName
import orc.ast.oil.named._
import orc.run.distrib.DOrcPlacementPolicy
import orc.values.Signal

/** Applying ValueSetAnalysis to an OIL named AST results in an annotated
  * OIL named AST.  The annotations indicate the set of values referred to
  * in expression AST subtrees.  For an expression e, the annotated form is
  * f(a1, a2, ..., an) >> e, where f is a dummy def, a1...an are referred-
  * to values, and e is the analyzed expression.  These annotations are
  * applied recursively, but only placed where the set of values changes.
  *
  * @author jthywiss
  */
object ValueSetAnalysis extends NamedASTTransform {
  val subAstValueSetDefNamePrefix = "ᑅSubAstValueSetDef"

  protected val subAstValueSetDefNames = new scala.collection.mutable.HashMap[Int, BoundVar]()
  def subAstValueSetDefName(arity: Int) = subAstValueSetDefNames.getOrElseUpdate(arity, new BoundVar(Some(subAstValueSetDefNamePrefix + arity)))
  def isSubAstValueSetTarget(target: Var) =
    target.optionalVariableName.isDefined && target.optionalVariableName.get.startsWith(subAstValueSetDefNamePrefix)

  override def apply(e: Expression): Expression = {
    val annotatedExpressionDirty = super.apply(e)
    val annotatedExpression = PostProcess(annotatedExpressionDirty)
    val annotatedExpressionWithDefs = e ->> subAstValueSetDefNames.toList.foldRight(annotatedExpression)({(arityAndName, e) =>
      DeclareCallables(List(Def(arityAndName._2, List.fill(arityAndName._1)(new BoundVar(Some(hasOptionalVariableName.unusedVariable))), Constant(Signal), Nil, Some(List.fill(arityAndName._1)(Top())), Some(ImportedType("orc.types.SignalType")))), e)
    })
    println(annotatedExpressionWithDefs)
    annotatedExpressionWithDefs
  }

  override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Expression, Expression] = {

    //case s @ Stop() => s

    case c @ Call(target, args, typeargs) => {
      annotate(c, args.toSet + target)
    }

    case Parallel(left, right) => {
      val (lte, lvs) = transformAndGetValueSet(left, context, typecontext)
      val (rte, rvs) = transformAndGetValueSet(right, context, typecontext)
      if (lvs == rvs || lvs.isEmpty || rvs.isEmpty) {
        annotate(Parallel(deannotate(lte), deannotate(rte)), lvs ++ rvs)
      } else {
        //TODO: Mark as superset
        annotate(Parallel(lte, rte), lvs ++ rvs)
      }
    }

    case Sequence(left, x, right) => {
      val (lte, lvs) = transformAndGetValueSet(left, context, typecontext)
      val (rte, rvs) = transformAndGetValueSet(right, context, typecontext)
      val vs = rvs.flatMap({ case `x` => lvs; case a => Set(a) })
      annotate(Sequence(if (vs == lvs) deannotate(lte) else lte, x, if (vs == rvs) deannotate(rte) else rte), vs)
    }

    case Graft(x, value, body) => {
      val (vte, vvs) = transformAndGetValueSet(value, context, typecontext)
      val (bte, bvs) = transformAndGetValueSet(body, context, typecontext)
      val vs = bvs.flatMap({ case `x` => vvs; case a => Set(a) })
      annotate(Graft(x, if (vs == vvs) deannotate(vte) else vte, if (vs == bvs) deannotate(bte) else bte), vs)
    }

    case Trim(expr) => {
      val (te, vs) = transformAndGetValueSet(expr, context, typecontext)
      annotate(Trim(deannotate(te)), vs)
    }

    case Otherwise(left, right) => {
      val (lte, lvs) = transformAndGetValueSet(left, context, typecontext)
      val (rte, rvs) = transformAndGetValueSet(right, context, typecontext)
      if (lvs == rvs || lvs.isEmpty || rvs.isEmpty) {
        annotate(Otherwise(deannotate(lte), deannotate(rte)), lvs)
      } else {
        //TODO: Mark as superset
        annotate(Otherwise(lte, rte), lvs ++ rvs)
      }
    }

    //case dc @ DeclareCallables(defs: List[Callable], body: Expression) => ???

    //case dt @ DeclareType(name: BoundTypevar, t: Type, body: Expression) => dt

    case HasType(body, expectedType) => {
      val (tb, vs) = transformAndGetValueSet(body, context, typecontext)
      annotate(HasType(deannotate(tb), expectedType), vs)
    }

    //case h @ Hole(context: Map[String, Argument], typecontext: Map[String, Type]) => h

    case VtimeZone(timeOrder: Argument, body: Expression) => {
      val (tb, vs) = transformAndGetValueSet(body, context, typecontext)
      annotate(VtimeZone(timeOrder, deannotate(tb)), vs + timeOrder)
    }

    //case n @ New(self: BoundVar, selfType: Option[Type], bindings: Map[orc.values.Field, Expression], objType: Option[Type]) => {
    //  ???
    //}

    //case fa @ FieldAccess(obj: Argument, field: orc.values.Field) => ??? //annotate(fa, Set(obj, Constant(field)))

    case a: Argument => annotate(a, Set(a))

  }

  protected def transformAndGetValueSet(expr: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): (Expression, Set[Argument]) = {
    val transformedExpr = transform(expr, context, typecontext)
    (transformedExpr, getValueSet(transformedExpr))
  }

  protected def annotate(expr: Expression, valueSet: Set[Argument]): Expression = {
    val filteredValues = filterRelevantValues(valueSet)
    if (filteredValues.isEmpty) {
      expr
    } else {
      expr ->> Sequence(Call(subAstValueSetDefName(filteredValues.size), filteredValues.toList, None), new BoundVar(Some(hasOptionalVariableName.unusedVariable)), expr)
    }
  }

  protected def deannotate(expr: Expression): Expression = expr match {
    case Sequence(Call(target: BoundVar, vs, None), _: BoundVar, innerExpr) if isSubAstValueSetTarget(target) => innerExpr
    case _ => expr
  }

  protected def getValueSet(expr: Expression): Set[Argument] = expr match {
    case Sequence(Call(target: BoundVar, vs, None), _: BoundVar, expr) if isSubAstValueSetTarget(target) => vs.toSet
    case _ => Set.empty
  }

  protected def filterRelevantValues(values: Set[Argument]): Set[Argument] = {
    values.filter({
      case _: Var => true
      case Constant(null) => false
      case Constant("hiya") => true
      case Constant(_: java.lang.Boolean) | Constant(_: java.lang.Character) | Constant(_: java.lang.Number) | Constant(_: String) => false
      case Constant(_: DOrcPlacementPolicy) => true
      //FIXME: Ask ValueLocators if this value is of interest
      case Constant(c: Class[_]) if c.getName.contains("VertexWithPathLen") => true
      case Constant(v) if v.getClass.getName.contains("VertexWithPathLen") => true
      case _ => false
    })
  }


//  override def transform(a: Argument, context: List[BoundVar]): Argument = {
//    val ta = super.transform(a, context)
//    println(s"Argument $a -> $ta")
//    ta
//  }
//
//  override def transform(e: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): Expression = {
//    val te = super.transform(e, context, typecontext)
//    println(s"Expression $e -> $te")
//    te
//  }
//
//  override def transform(t: Type, typecontext: List[BoundTypevar]): Type = {
//    val tt = super.transform(t, typecontext)
//    println(s"Type $t -> $tt")
//    tt
//  }
//
//  override def transform(d: Callable, context: List[BoundVar], typecontext: List[BoundTypevar]): Callable = {
//    val td = super.transform(d, context, typecontext)
//    println(s"Callable $d -> $td")
//    td
//  }

  object PostProcess extends NamedASTTransform {
    override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Expression, Expression] = {
      /* Clean up obviously redundant annotations */
      case Sequence(Call(target: BoundVar, vs, None), _: BoundVar, innerExpr: Call) if isSubAstValueSetTarget(target) => innerExpr
      case Sequence(c: Constant, bv, Sequence(Call(target: BoundVar, vs, None), _: BoundVar, innerExpr)) if isSubAstValueSetTarget(target) && bv == innerExpr => c
    }
  }
}
