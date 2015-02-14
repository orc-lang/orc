{- gui.inc -- A GUI library for Orc programs
 -
 - $Id$
 -
 - Created by amp on Feb 11, 2015 7:43:34 PM
 -}

import class JComponent = "java.awt.Component"
import class JFrame = "javax.swing.JFrame"
import class JButton = "javax.swing.JButton"
import class SwingUtilities = "javax.swing.SwingUtilities"
import class WindowConstants = "javax.swing.WindowConstants"
import class Window = "java.awt.Window"
import class BoxLayout = "javax.swing.BoxLayout"

import class Runnable = "java.lang.Runnable"

import site CallableToRunnable = "orc.lib.util.CallableToRunnable"
import site Block = "orc.lib.util.Block"

import site ActionListenerAdapter = "orc.lib.gui.ActionListenerAdapter"
import site WindowListenerAdapter = "orc.lib.gui.WindowListenerAdapter"

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

-- Call callable when this def call is killed.
-- As a side effect this will never halt.
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)
  
class FanoutChannel {
  val lock = Semaphore(1)
  val listeners = Ref[List[Channel]]([])
  
  def put(v) = each(listeners?) >c> c.put(v)
  
  -- Return a listener channel that will be active until this call is killed.
  def newListener() = withLock(lock, {
    val c = Channel()
    runOnKill({ removeListener(c) }) >>
    listeners := c : listeners? >>
    c
  })
  
  def removeListener(c :: Channel) = withLock(lock, {
    listeners := filter(listeners?, {_ /= c})
  })
  
  def listen() =
    val c = newListener()
    repeat(c.get)
}
def FanoutChannel() = new FanoutChannel
  
def onEDT(f :: lambda() :: Top) = SwingUtilities.invokeLater(CallableToRunnable(f))
def onEDTBlock[A](f :: lambda() :: A) = 
  val c = Cell[A]()
  SwingUtilities.invokeLater(CallableToRunnable({ c.write(f()) })) >>
  c.read()

class Component {
  val impl :: JComponent
}

class Frame extends Component {
  val children :: List[Top]
  val impl = onEDTBlock({ JFrame() })
  val listener = WindowListenerAdapter(this)
  val _ = onEDT({
    impl.getContentPane().setLayout(BoxLayout(impl.getContentPane(), BoxLayout.PAGE_AXIS?))
    >> impl.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE?)
    >> impl.addWindowListener(listener)
  })
  
  def setVisible(b :: Boolean) = Println("setVisible") >> onEDT({
    Println("setVisible onEDT")
    >> impl.pack()
    >> impl.setVisible(b)
  })
  
  def dispose() = onEDT({ impl.removeWindowListener(listener) }) >> impl.dispose()
  
  def add(c) = Println("adding") >> onEDT({
    Println("Adding onEDT") >>
    impl.add(c.impl)
  })

  val _ = map(add, children) >> setVisible(true)
  val _ = runOnKill({ dispose() })
  
  val closingChannel = FanoutChannel()
  def windowClosing(e) = closingChannel.put(e)
  
  def onClosing() = closingChannel.listen()
}
def Frame(children_ :: List[Top]) = new Frame with { val children = children_ }

class Button extends Component {
  val text :: String
  val impl = onEDTBlock({ JButton() })
  val _ = setText(text)
  
  def setText(s :: String) = onEDT({
    impl.setText(s)
  })
  
  val listener = ActionListenerAdapter(this)
  val _ = onEDT({
    impl.addActionListener(listener)
  })
  val _ = runOnKill({ onEDT({
    impl.removeActionListener(listener)
  }) })
  
  val actionChannel = FanoutChannel()
  def actionPerformed(e) = actionChannel.put(e)  
  
  def onAction() = actionChannel.listen()
}
def Button(text_ :: String) = new Button with { val text = text_ }

-- def closeGUI() = each(arrayToList(Window.getWindows())) >w> Println("Disposing " + w) >> w.dispose()
