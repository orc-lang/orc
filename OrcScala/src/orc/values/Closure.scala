//
// Closure.scala -- Scala class/trait/object Closure
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

import orc.oil.nameless.Def
import orc.oil.nameless.Expression
import orc.oil.nameless.AddNames

import orc.oil.named.PrettyPrint
import orc.oil.named.BoundVar


/**
 * 
 *
 * @author dkitchin
 */
class Closure(d: Def, ds: List[Def]) extends OrcValue {
    val arity: Int = d.arity
    val body: Expression = d.body
    var context: List[AnyRef] = Nil
    
    override def toOrcSyntax() = {
      val (defs, rest) = context.splitAt(ds.size)
      val newctx = (defs map {_ => None}) ::: (rest map { Some(_) })
      val subdef = d.subst(newctx)
      val myName = new BoundVar()
      val defNames = 
        for (d <- defs) yield 
          if (d == this) { myName } else { new BoundVar() }
      val namedDef = AddNames.namelessToNamed(myName, subdef, defNames, Nil)
      val pp = new PrettyPrint()
      "lambda" +
        pp.reduce(namedDef.name) + 
          pp.paren(namedDef.formals) + 
            " = " + 
              pp.reduce(namedDef.body)
    }
}
object Closure {
    def unapply(c: Closure) = Some((c.arity, c.body, c.context))
}