//
// Read.scala -- Scala object Read
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.values.sites.TotalSite
import orc.values.sites.UntypedSite
import orc.values.Value
import orc.values.Literal
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.compile.parse.OrcParser
import orc.error.compiletime.ParsingException
import orc.compile.ext.Expression
import orc.compile.ext.Constant
import orc.compile.ext.ListExpr
import orc.compile.ext.TupleExpr
import orc.values.OrcList
import orc.values.OrcTuple

object Read extends TotalSite with UntypedSite {
  def evaluate(args: List[Value]): Value = {
    val parsedValue = args match {
      case List(Literal(s: String)) => {
        val parser = new OrcParser(null)
        parser.scanAndParseLiteral(s) match {
          case parser.Success(v, _) => v
          case parser.NoSuccess(msg, _) => throw new ParsingException(msg+" when reading \""+s+"\"")
        }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "String", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
    convertToOrcValue(parsedValue)
  }
  def convertToOrcValue(v: Expression): Value = v match {
    case Constant(v) => Literal(v)
    case ListExpr(vs) => OrcList(vs map convertToOrcValue)
    case TupleExpr(vs) => OrcTuple(vs map convertToOrcValue)
    case mystery => throw new ParsingException("Don't know how to convert a "+mystery.getClass().getName()+" to a Value")
  }
}
