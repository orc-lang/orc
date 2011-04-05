//
// Main.scala -- Scala object Main
// Project OrcDocExamples
//
// $Id$
//
// Created by dkitchin on Apr 04, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package extraction

import java.io.File
import scala.xml._

object Main {

	def isXmlFile(f: File): Boolean = {
		f.isFile() && """\.xml$""".r.findFirstIn(f.getName()).isDefined
	}

	
  def main(args: Array[String]): Unit = {  
  	
  	val sourcedir = new File(args(0))
	  val targetdir = new File(args(1))
	  
	  val files = sourcedir.listFiles().toList filter { isXmlFile(_) }
  	
  	for (f <- files) {
  		println("Processing " + f.toString)
  		val root = XML.loadFile(f)
  		for ((id, code) <- extractExamples(f)) {
  			val target = new File(targetdir, id + ".orc")
  			target.createNewFile()
  			val writer = new java.io.FileWriter(target)
  			writer.write(code)
  			writer.close()
  		}
  	}
  	
  }
  
  def useless = true
  
  def extractExamples(f: File): List[(String, String)] = {
  	val root = XML.loadFile(f)
  	val xmlPrefix = "http://www.w3.org/XML/1998/namespace"
  	val examples = 
  		(for (example <- root \\ "example") yield {
  		  (for (<programlisting>{Text(x)}</programlisting> <- example \ "programlisting") yield {
  		    example.attribute(xmlPrefix, "id") match {
  		    	case Some(id) => List( (id.text, x) )
  		    	case None => Nil
  		    }		
  		  }).toList.flatten
  	  }).toList.flatten
  	examples
  	//(for (<programlisting>{Text(x)}</programlisting> <- (root \\ "example" \ "programlisting")) yield x).toList
  }

}