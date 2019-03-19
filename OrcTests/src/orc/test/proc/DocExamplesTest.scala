//
// DocExamplesTest.scala -- Scala object DocExamplesTest
// Project OrcTests
//
// Created by dkitchin on Apr 04, 2010.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.nio.file.{ Files, Path, Paths, StandardOpenOption }

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
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
    val userguideOutDir = Paths.get("build/docexamples/userguide")
    val refmanualOutDir = Paths.get("build/docexamples/refmanual")
    Files.createDirectories(userguideOutDir)
    Files.createDirectories(refmanualOutDir)
    extractAllExamples(Paths.get("../OrcDocs/src/userguide"), userguideOutDir)
    extractAllExamples(Paths.get("../OrcDocs/src/refmanual"), refmanualOutDir)
    val bindings = new OrcBindings()
    TestUtils.buildSuite(getClass.getSimpleName, (s, t, f, e, b) => new DocExamplesTestCase(s, t, f, e, b), bindings, Paths.get("build/docexamples"))
  }

  class DocExamplesTestCase(suitename: String, testname: String, file: Path, expecteds: ExpectedOutput, bindings: OrcBindings) extends TestUtils.OrcTestCase(suitename, testname, file, expecteds, bindings) {}

  def extractAllExamples(sourcedir: Path, targetdir: Path) {
    val files = Files.newDirectoryStream(sourcedir).asScala filter { isXmlFile(_) }

    for (f <- files) {
      println("Processing " + f.toString)
      var anyExamples = false
      for ((id, code) <- extractExamples(f)) {
        val target = targetdir.resolve(id + ".orc")
        println("-> " + id + ".orc")
        val writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW)
        try {
          writer.write(code)
        } finally {
          writer.close()
        }
        anyExamples = true
      }
      if (!anyExamples) { println("   No examples found.") }
      println()
    }

  }

  def isXmlFile(f: Path): Boolean = {
    Files.isRegularFile(f) && """\.xml$""".r.findFirstIn(f.getFileName.toString).isDefined
  }

  def extractExamples(f: Path): List[(String, String)] = {
    val root = XML.loadFile(f.toFile)
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
