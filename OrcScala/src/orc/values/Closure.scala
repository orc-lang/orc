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

import orc.oil.named.PrettyPrint
import orc.oil.named.BoundVar
import orc.oil.nameless.NamelessToNamed


/**
 * 
 *
 * @author dkitchin
 */
case class Closure(defs: List[Def], pos: Int, valueContext: List[AnyRef]) extends OrcValue with NamelessToNamed {
  
  if (!(defs isDefinedAt pos)) { throw new Error("Invalid arguments to Closure constructor"); }
  
  val arity: Int = defs(pos).arity
  val body: Expression = defs(pos).body
    
  override def toOrcSyntax() = "closure"
  /*
  {
    val (defs, rest) = context.splitAt(ds.size)
    val newctx = (defs map {_ => None}) ::: (rest map { Some(_) })
    val subdef = defs(pos).subst(context map { Some(_) })
    }
    val myName = new BoundVar()
    val defNames = 
      for (d <- defs) yield 
        if (d == this) { myName } else { new BoundVar() }
    val namedDef = namelessToNamed(myName, subdef, defNames, Nil)
    val pp = new PrettyPrint()
    "lambda" +
      pp.reduce(namedDef.name) + 
        pp.paren(namedDef.formals) + 
          " = " + 
            pp.reduce(namedDef.body)
    }
    */
}