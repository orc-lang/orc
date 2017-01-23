//
// ScalaSwingUtils.scala -- Scala object ScalaSwingUtils
// Project OrcSites
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.gui

import javax.swing.SwingUtilities

object ScalaSwingUtils {
  def onEDT(f: => Unit): Unit = {
    if (SwingUtilities.isEventDispatchThread()) {
      f
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        def run() = f
      })
    }
  }
  def onEDTNow[T](f: => T): T = {
    if (SwingUtilities.isEventDispatchThread()) {
      f
    } else {
      var r: Option[T] = None
      SwingUtilities.invokeAndWait(new Runnable() {
        def run() = r = Some(f)
      })
      r.get
    }
  }
}
