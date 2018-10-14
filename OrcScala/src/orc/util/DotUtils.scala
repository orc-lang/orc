//
// DotUtils.scala -- Scala object DotUtils
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

object DotUtils {
  import orc.run.Logger
  import java.io.File

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

  def render(fn: File) = {
    import scala.sys.process._
    val outformat = "svg"
    val tmpSvg = File.createTempFile(fn.getName, s".$outformat", fn.getParentFile);

    Seq("dot", s"-T$outformat", fn.getAbsolutePath, s"-o${tmpSvg.getAbsolutePath}").!
    Logger.info(s"Rendered $fn to $tmpSvg")
    tmpSvg
  }

  def display(fn: File): Unit = {
    import scala.sys.process._
    Seq("sensible-browser", fn.getAbsolutePath).!
  }
}
