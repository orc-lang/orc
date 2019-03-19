//
// Main.scala -- Scala object Main
// Project OrcDocgen
//
// Created by dkitchin on Dec 28, 2010.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package docgen

import java.nio.file.{ Files, Path, Paths }

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.xml.{ Node, XML }

object Main {

  def isDocFile(f: Path): Boolean = {
    Files.isRegularFile(f) && """\.inc$""".r.findFirstIn(f.getFileName.toString).isDefined
  }

  def main(args: Array[String]) {

    val sourcedir = Paths.get(args(0))
    val targetdir = Paths.get(args(1))

    val files = Files.newDirectoryStream(sourcedir).asScala filter { isDocFile(_) }
    val prefix = "ref.stdlib"
    val maker = new DocMaker(prefix)

    val targets = files map { f =>
      val sectionName = """\w+""".r.findPrefixOf(f.getFileName.toString).get
      val xml = maker.renderSection(f)(sectionName)
      val target = targetdir.resolve(prefix + "." + sectionName + ".xml")
      xmlwrite(xml, target)
      target
    }

    val xml = maker.renderChapter(targets)
    val target = Paths.get(targetdir.toString, prefix + ".xml")
    xmlwrite(xml, target)

  }

  def xmlwrite(xml: Node, target: Path) {
    val writer = Files.newBufferedWriter(target)
    XML.write(writer, xml, "UTF-8", true, null)
    writer.close()
  }

}
