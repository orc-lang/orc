//
// SourceSectionFromPorc.scala -- Scala class SourceSectionFromPorc
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.ast.porc.PorcAST
import orc.compile.parse.OrcInputContext
import orc.run.porce.Logger

import com.oracle.truffle.api.source.{ Source, SourceSection }

/** Function to build a Truffle SourceSection from a Porc node.
 *
 */
object SourceSectionFromPorc {
  val sourceCache = new java.util.WeakHashMap[OrcInputContext, Source]()

  def apply(e: PorcAST): SourceSection = synchronized {
    e.sourceTextRange match {
      case Some(range) =>
        val res = range.start.resource
        res.toURI.getScheme match {
          case "data" =>
            // TODO: Implement data URLs here and generate a string based Source.
            Logger.warning(s"Failed to translate a Orc source text range for Truffle. This will result in missing source markers in Truffle. The source in question is:\n$range")
            null
          case _ =>
            val url = res.toURL
            val start = range.start.offset
            val end = range.end.offset
            val source = sourceCache.computeIfAbsent(res, (_) => Source.newBuilder("Orc", url).build())
            source.createSection(start, end - start)
        }
      case None =>
        null
    }
  }
}
