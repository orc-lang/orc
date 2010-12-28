//
// DocGen.scala -- Scala class/trait/object DocGen
// Project OrcDocgen
//
// $Id$
//
// Created by dkitchin on Dec 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package docgen

import java.io.File
import scala.xml._

object DocGen {

  def main(args: Array[String]) {
	  
	val sourcedir = new File(args(0))
	val target = new File(args(1))
	val files = sourcedir.listFiles().toList
    val xml = DocMaker.makeDoc(files)
    val writer = new java.io.FileWriter(target)
    XML.write(writer, xml, "UTF-8", true, null)
    writer.close()
  
  }
	
}