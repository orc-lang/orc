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