//
// EnhancedErrorMessages.scala -- Scala trait EnhancedErrorMessages
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.parse

import scala.util.parsing.combinator.syntactical._
import orc.ast.ext.Expression

/**
 * 
 * Re-write some error messages with explanatory detail.
 * 
 * @author amshali
 */
trait EnhancedErrorMessages extends StandardTokenParsers {
  
  def enhanceErrorMsg(r: ParseResult[Expression]): ParseResult[Expression] = {
    r match {
      case Failure(msg, in) => {
        if (msg.equals("``('' expected but EOF found")) {
          Failure(msg+".\n"+
              "  This error usually means that the expression is incomplete.\n" +
              "The following cases can create this parser problem:\n" +
              " 1. The goal expression of the program is missing.\n" +
              " 2. The right hand side expression of a combinator is missing.\n" +
              " 3. An expression is missing after a comma.\n"
              , in)
        } 
        else if (msg.startsWith("``::'' expected but")) {
          Failure(msg+".\n"+
              "  This error usually means that the `=' is missing in front of\n" +
              "the function definition.\n" +
              "  In case you want to specify the return type of the function\n" +
              "use `::' along with the type name."
              , in)
        }
        else if (msg.startsWith("``('' expected but `)' found")) {
          Failure(msg+".\n"+
              "  This error usually means an illegal start for an expression with `)'.\n" +
              "Check for mismatched parentheses."
              , in)
        }
        else if (msg.startsWith("``('' expected but")) {
          val name = msg.substring(19, msg.lastIndexOf("found")-1)
          Failure(msg+".\n"+
              "  This error usually means an illegal start for an expression with "+name+".\n"
              , in)
        }
        else if (msg.startsWith("``<'' expected but")) {
          Failure(msg+".\n"+
              "  This error usually happens in the following occasions:\n" +
              "  1. You are intending to use the pruning combinator `<<' and\n" +
              "     you have typed `<'.\n" +
              "  2. You are intending to use `<' as less than operator in which\n" +
              "     case you should know that the less than operator in Orc is `<:'."
              , in)
        }
        else if (msg.startsWith("``>'' expected but")) {
          Failure(msg+".\n"+
              "  This error usually happens in the following occasions:\n" +
              "  1. You are intending to use the sequential combinator `>>' and\n" +
              "     you have typed `>'.\n" +
              "  2. You are intending to use `>' as greater than operator in which\n" +
              "     case you should know that the greater than operator in Orc is `:>'."
              , in)
        }
        else r
      }
      case _ => r
    }
  }

}
