//
// OrcTuple.scala -- Scala class/trait/object OrcTuple
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

import orc.values.sites.UntypedSite
import orc.values.sites.PartialSite


/**
 * 
 *
 * @author dkitchin
 */
case class OrcTuple(values: List[AnyRef]) extends PartialSite with UntypedSite {
  def evaluate(args: List[AnyRef]) = 
    args match {
      case List(bi: BigInt) => {
        val i: Int = bi.intValue
        if (0 <= i  &&  i < values.size) 
          { Some(values(i)) }
        else
          { None }
      }
      case _ => None
    }
  override def toOrcSyntax() = "(" + Format.formatSequence(values) + ")" 
}