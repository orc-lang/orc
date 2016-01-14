//
// Read.scala -- Scala object Read
// Project OrcScala
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.values.sites.TotalSite
import orc.values.sites.TypedSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.compile.parse.OrcLiteralParser
import orc.error.compiletime.ParsingException
import orc.ast.ext.Expression
import orc.ast.ext.Constant
import orc.ast.ext.ListExpr
import orc.ast.ext.TupleExpr
import orc.ast.ext.RecordExpr
import orc.values.OrcTuple
import orc.values.OrcRecord
import orc.types._

object Read extends TotalSite with TypedSite {
  def evaluate(args: List[AnyRef]): AnyRef = {
    val parsedValue = args match {
      case List(s: String) => {
        OrcLiteralParser(s) match {
          case r: OrcLiteralParser.SuccessT[_] => r.get.asInstanceOf[Expression]
          case n: OrcLiteralParser.NoSuccess => throw new ParsingException(n.msg + " when reading \"" + s + "\"", n.next.pos)
        }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "String", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }
    convertToOrcValue(parsedValue)
  }
  def convertToOrcValue(v: Expression): AnyRef = v match {
    case Constant(v) => v
    case ListExpr(vs) => vs map convertToOrcValue
    case TupleExpr(vs) => OrcTuple(vs map convertToOrcValue)
    case RecordExpr(vs) => OrcRecord((vs.toMap) mapValues convertToOrcValue)
    case mystery => throw new ParsingException("Don't know how to convert a " + (if (mystery != null) mystery.getClass().toString() else "null") + " to an Orc value", null)
  }

  def orcType(): Type = SimpleFunctionType(StringType, Top)

}
