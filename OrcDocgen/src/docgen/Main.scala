//
// Main.scala -- Scala object Main
// Project OrcDocgen
//
// Created by dkitchin on Dec 28, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package docgen

import java.io.File
import scala.xml._

object Main {

  def isDocFile(f: File): Boolean = {
    f.isFile() && """\.inc$""".r.findFirstIn(f.getName()).isDefined
  }

  def main(args: Array[String]) {

    val sourcedir = new File(args(0))
    val targetdir = new File(args(1))

    val files = sourcedir.listFiles().toList filter { isDocFile(_) }
    val prefix = "ref.stdlib"
    val maker = new DocMaker(prefix)

    val targets = files map { f =>
      val sectionName = """\w+""".r.findPrefixOf(f.getName()).get
      val xml = maker.renderSection(f)(sectionName)
      val target = new File(targetdir, prefix + "." + sectionName + ".xml")
      xmlwrite(xml, target)
      target
    }

    val xml = maker.renderChapter(targets)
    val target = new File(targetdir, prefix + ".xml")
    xmlwrite(xml, target)

  }

  def xmlwrite(xml: Node, target: File) {
    val writer = new java.io.FileWriter(target)
    XML.write(writer, xml, "UTF-8", true, null)
    writer.close()
  }

}
