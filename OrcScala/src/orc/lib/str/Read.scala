//
// Read.scala -- Scala object Read
// Project OrcScala
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.ast.ext.{ Constant, Expression, ListExpr, RecordExpr, TupleExpr }
import orc.compile.parse.{ OrcLiteralParser, ToTextRange }
import orc.error.compiletime.ParsingException
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException }
import orc.types.{ SimpleFunctionType, StringType, Top, Type }
import orc.values.{ OrcRecord, OrcTuple }
import orc.values.sites.{ TotalSite1Simple, TypedSite }
import orc.util.ArrayExtensions.Array1

object Read extends TotalSite1Simple[String] with TypedSite {
  def eval(s: String): AnyRef = {
    val parsedValue =
        OrcLiteralParser(s) match {
          case r: OrcLiteralParser.SuccessT[_] => r.get.asInstanceOf[Expression]
          case n: OrcLiteralParser.NoSuccess => throw new ParsingException(n.msg + " when reading \"" + s + "\"", ToTextRange(n.next.pos))
        }
    convertToOrcValue(parsedValue)
  }
  def convertToOrcValue(v: Expression): AnyRef = v match {
    case Constant(v) => v
    case ListExpr(vs) => vs map convertToOrcValue
    case TupleExpr(vs) => OrcTuple(vs.map(convertToOrcValue).toArray)
    case RecordExpr(vs) => OrcRecord((vs.toMap) mapValues convertToOrcValue)
    case mystery => throw new ParsingException("Don't know how to convert a " + (if (mystery != null) mystery.getClass().toString() else "null") + " to an Orc value", null)
  }

  def orcType(): Type = SimpleFunctionType(StringType, Top)

}
