//
// DocExamplesTest.scala -- Scala object DocExamplesTest
// Project OrcTests
//
// Created by dkitchin on Apr 04, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.File

import scala.xml.{ Text, XML }
import scala.xml.NodeSeq.seqToNodeSeq

import orc.script.OrcBindings
import orc.test.util.{ ExpectedOutput, TestUtils }

import junit.framework.Test

/** Test suite for examples extracted from documentation.
  *
  * @author dkitchin
  */
object DocExamplesTest extends ExamplesTest {

  def suite(): Test = {
    val userguideOutDir = new File("build/docexamples/userguide")
    val refmanualOutDir = new File("build/docexamples/refmanual")
    userguideOutDir.mkdirs()
    refmanualOutDir.mkdirs()
    extractAllExamples(new File("../OrcDocs/src/userguide"), userguideOutDir)
    extractAllExamples(new File("../OrcDocs/src/refmanual"), refmanualOutDir)
    val bindings = new OrcBindings()
    TestUtils.buildSuite(getClass.getSimpleName, (s, t, f, e, b) => new DocExamplesTestCase(s, t, f, e, b), bindings, new File("build/docexamples"))
  }

  class DocExamplesTestCase(suitename: String, testname: String, file: File, expecteds: ExpectedOutput, bindings: OrcBindings) extends TestUtils.OrcTestCase(suitename, testname, file, expecteds, bindings) {}

  def extractAllExamples(sourcedir: File, targetdir: File) {
    val files = sourcedir.listFiles().toList filter { isXmlFile(_) }

    for (f <- files) {
      println("Processing " + f.toString)
      var anyExamples = false
      for ((id, code) <- extractExamples(f)) {
        val target = new File(targetdir, id + ".orc")
        println("-> " + id + ".orc")
        target.createNewFile()
        val writer = new java.io.FileWriter(target)
        writer.write(code)
        writer.close()
        anyExamples = true
      }
      if (!anyExamples) { println("   No examples found.") }
      println()
    }

  }

  def isXmlFile(f: File): Boolean = {
    f.isFile() && """\.xml$""".r.findFirstIn(f.getName()).isDefined
  }

  def extractExamples(f: File): List[(String, String)] = {
    val root = XML.loadFile(f)
    val xmlPrefix = "http://www.w3.org/XML/1998/namespace"
    val examples =
      (for (example <- root \\ "example") yield {
        (for (<programlisting>{ Text(x) }</programlisting> <- example \ "programlisting") yield {
          example.attribute(xmlPrefix, "id") match {
            case Some(id) => List((id.text, x))
            case None => Nil
          }
        }).toList.flatten
      }).toList.flatten
    examples
    //(for (<programlisting>{Text(x)}</programlisting> <- (root \\ "example" \ "programlisting")) yield x).toList
  }

}
