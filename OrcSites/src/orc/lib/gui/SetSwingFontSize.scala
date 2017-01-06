package orc.lib.gui

import orc.values.sites.TotalSite
import orc.values.sites.SpecificArity
import javax.swing.UIManager
import scala.collection.JavaConversions._
import orc.values.Signal
import ScalaSwingUtils._

class SetSwingFontSize extends TotalSite with SpecificArity {
  val arity = 1

  def evaluate(args: List[AnyRef]): AnyRef = {
    val List(sizeN: Number) = args
    val size = sizeN.intValue()

    onEDTNow {
      val keySet = UIManager.getLookAndFeelDefaults().keySet().toSeq

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