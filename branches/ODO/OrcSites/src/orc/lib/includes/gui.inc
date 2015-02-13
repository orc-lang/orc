{- gui.inc -- A GUI library for Orc programs
 -
 - $Id$
 -
 - Created by amp on Feb 11, 2015 7:43:34 PM
 -}

import class ImplComponent = "orc.lib.gui.ImplComponent"
import class ImplFrame = "orc.lib.gui.ImplFrame"
import class ImplButton = "orc.lib.gui.ImplButton"
import site Block = "orc.lib.util.Block"

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

-- Call callable when this def call is killed.
-- As a side effect this will never halt.
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)

class Component {
  val impl :: ImplComponent
}

class Frame extends Component {
  val children :: List[Top]
  val impl = ImplFrame()
  val _ = map({ impl.add(_.impl) }, children) >> impl.setVisible(true)
  val _ = runOnKill({ impl.dispose() })
  
  val onClosed = impl.onClosed()
}
def Frame(children_ :: List[Top]) = new Frame with { val children = children_ }

class Button extends Component {
  val text :: String
  val impl = ImplButton()
  val _ = impl.setText(text)
  val _ = runOnKill({ impl.dispose() })
  
  val setText = impl.setText
  val onClicked = impl.onClicked()
}
def Button(text_ :: String) = new Button with { val text = text_ }
