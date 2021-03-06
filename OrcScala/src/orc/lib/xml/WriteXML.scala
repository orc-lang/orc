//
// WriteXML.scala -- Scala class WriteXML
// Project OrcScala
//
// Created by dkitchin on Nov 17, 2010.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.xml

import scala.xml.Node

import orc.types.{ SimpleFunctionType, StringType }
import orc.values.sites.{ TotalSite1Simple, TypedSite }

/**
  * @author dkitchin
  */
object WriteXML extends TotalSite1Simple[Node] with TypedSite {

  def eval(xml: Node): AnyRef = {
    xml.toString
  }

  def orcType() = SimpleFunctionType(XMLType, StringType)

}
