//
// SetSwingFontSize.scala -- Scala class SetSwingFontSize
// Project OrcSites
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.gui

import orc.values.sites.TotalSite
import orc.values.sites.SpecificArity
import javax.swing.UIManager
import scala.collection.JavaConverters._
import orc.values.Signal
import ScalaSwingUtils._
import orc.util.ArrayExtensions.Array1

class SetSwingFontSize extends TotalSite with SpecificArity {
  val arity = 1

  def evaluate(args: Array[AnyRef]): AnyRef = {
    val Array1(sizeN: Number) = args
    val size = sizeN.intValue()

    onEDTNow {
      val keySet = UIManager.getLookAndFeelDefaults().keySet().asScala

      for (key <- keySet if key != null) {
        val font = UIManager.getDefaults().getFont(key);
        if (font != null) {
          // The explicit type Float is used because deriveFont has dramatically different behavior for different overloads.
          UIManager.put(key, font.deriveFont((font.getSize2D() * size): Float));
        }
      }
    }

    Signal
  }
}
