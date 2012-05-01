//
// AggregateDef.scala -- Scala class AggregateDef
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import orc.util.OptionMapExtension._
import orc.ast.ext._
import orc.ast.oil.named
import orc.error.compiletime._
import orc.error.OrcExceptionExtension._

case class AggregateDef(clauses: List[Clause],
  typeformals: Option[List[String]],
  argtypes: Option[List[Type]],
  returntype: Option[Type])(implicit translator: Translator) extends orc.ast.AST {

  import translator._

  def unify[A](x: Option[A], y: Option[A], reportCollision: => Unit): Option[A] =
    (x, y) match {
      case (None, None) => None
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (Some(x), Some(y)) => reportCollision; Some(x)
    }
  def unifyList[A](x: Option[List[A]], y: Option[List[A]], reportCollision: => Unit): Option[List[A]] =
    (x, y) match {
      case (None, None) => None
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (Some(Nil), Some(Nil)) => Some(Nil) // Nils are allowed to unify
      case (Some(x), Some(y)) => reportCollision; Some(x)
    }

  def +(defn: DefDeclaration): AggregateDef =
    defn -> {
      case Def(_, maybeTypeFormals, formals, maybeReturnType, maybeGuard, body) => {
        val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(formals)
        val newclause = defn ->> Clause(newformals, maybeGuard, body)
        val newTypeFormals = unifyList(typeformals, maybeTypeFormals, reportProblem(RedundantTypeParameters() at defn))
        val newArgTypes = unifyList(argtypes, maybeArgTypes, reportProblem(RedundantArgumentType() at defn))
        val newReturnType = unify(returntype, maybeReturnType, reportProblem(RedundantReturnType() at defn))
        AggregateDef(clauses ::: List(newclause), newTypeFormals, newArgTypes, newReturnType)
      }
      case DefClass(name, maybeTypeFormals, formals, maybeReturnType, maybeGuard, body) => {
        this + Def(name, maybeTypeFormals, formals, maybeReturnType, maybeGuard, new DefClassBody(body))
      }
      case DefSig(_, maybeTypeFormals, argtypes2, maybeReturnType) => {
        val newTypeFormals = unifyList(typeformals, maybeTypeFormals, reportProblem(RedundantTypeParameters() at defn))
        val newArgTypes = unifyList(argtypes, Some(argtypes2), reportProblem(RedundantArgumentType() at defn))
        val newReturnType = unify(returntype, Some(maybeReturnType), reportProblem(RedundantReturnType() at defn))
        AggregateDef(clauses, newTypeFormals, newArgTypes, newReturnType)
      }
    }

  def +(lambda: Lambda): AggregateDef = {
    val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(lambda.formals)
    val newclause = lambda ->> Clause(newformals, lambda.guard, lambda.body)
    val newTypeFormals = unifyList(typeformals, lambda.typeformals, reportProblem(RedundantTypeParameters() at lambda))
    val newArgTypes = unifyList(argtypes, maybeArgTypes, reportProblem(RedundantArgumentType() at lambda))
    val newReturnType = unify(returntype, lambda.returntype, reportProblem(RedundantReturnType() at lambda))
    AggregateDef(clauses ::: List(newclause), newTypeFormals, newArgTypes, newReturnType)
  }

  def convert(x: named.BoundVar, context: Map[String, named.Argument], typecontext: Map[String, named.Type]): named.Def = {
    if (clauses.isEmpty) { reportProblem(UnusedFunctionSignature() at this) }

    val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals.getOrElse(Nil), this)
    val newtypecontext = typecontext ++ dtypecontext
    val newArgTypes = argtypes map { _ map { convertType(_)(newtypecontext) } }
    val newReturnType = returntype map { convertType(_)(newtypecontext) }

    val (newformals, newbody) = Clause.convertClauses(clauses)(context, newtypecontext, translator)

    named.Def(x, newformals, newbody, newTypeFormals, newArgTypes, newReturnType)
  }

  def ClassCheck {
    var existsClass = false
    var existsNotClass = false
    for (aclause <- clauses) {
      aclause match {
        case Clause(_, _, DefClassBody(_)) =>
          if (existsNotClass) { reportProblem(ClassDefInNonclassContext() at aclause) }
          else existsClass = true
        case _ =>
          if (existsClass) { reportProblem(NonclassDefInClassContext() at aclause) }
          else existsNotClass = true
      }
    }
  }

}

object AggregateDef {

  def formalsPartition(formals: List[Pattern]): (List[Pattern], Option[List[Type]]) = {
    val maybePartitioned =
      formals optionMap {
        case TypedPattern(p, t) => Some(p, t)
        case _ => None
      }
    maybePartitioned match {
      case Some(l) => {
        val (ps, ts) = l.unzip
        (ps, Some(ts))
      }
      case None => (formals, None)
    }
  }

  def empty(implicit translator: Translator) = new AggregateDef(Nil, None, None, None)

  def apply(defn: DefDeclaration)(implicit translator: Translator) = defn -> { empty + _ }
  def apply(lambda: Lambda)(implicit translator: Translator) = lambda -> { empty + _ }

}
