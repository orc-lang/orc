//
// OrcTuple.scala -- Scala class OrcTuple
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

import orc.values.sites.UntypedSite
import orc.values.sites.PartialSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.TupleIndexOutOfBoundsException

/**
  * @author dkitchin
  */
case class OrcTuple(values: List[AnyRef]) extends PartialSite with UntypedSite {

  assert(values.size > 1)

  def evaluate(args: List[AnyRef]) =
    args match {
      case List(bi: BigInt) => {
        val i: Int = bi.intValue
        if (0 <= i && i < values.size) { Some(values(i)) }
        else { throw new TupleIndexOutOfBoundsException(i) }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }
  override def toOrcSyntax() = "(" + Format.formatSequence(values) + ")"
}
