//
// DotUtils.scala -- Scala object DotUtils
// Project OrcScala
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.nio.file.Files

object DotUtils {
  import java.nio.file.Path
  import orc.run.Logger

  def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
  def quote(s: String) = s.replace('"', '\'').replace("\n", "\\n")

  type DotAttributes = Map[String, String]

  trait WithDotAttributes {
    def dotAttributes: DotAttributes

    def dotAttributeString = {
      s"[${dotAttributes.map(p => s"${p._1}=${'"'}${quote(p._2)}${'"'}").mkString(",")}]"
    }
  }

  implicit class MapWithDotAttributes(m: Map[String, Any]) extends WithDotAttributes {
    def dotAttributes: DotAttributes = m.mapValues(_.toString)
  }

  def render(fn: Path) = {
    import scala.sys.process._
    val outformat = "svg"
    val tmpSvg = Files.createTempFile(fn.getParent, fn.getFileName.toString, s".$outformat");

    Seq("dot", s"-T$outformat", fn.toAbsolutePath.toString, s"-o${tmpSvg.toAbsolutePath.toString}").!
    Logger.info(s"Rendered $fn to $tmpSvg")
    tmpSvg
  }

  def display(fn: Path): Unit = {
    import scala.sys.process._
    Seq("sensible-browser", fn.toAbsolutePath.toString).!
  }
}
