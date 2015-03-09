{- gui.inc -- A GUI library for Orc programs
 -
 - $Id$
 -
 - Created by amp on Feb 11, 2015 7:43:34 PM
 -}

import class JComponent = "java.awt.Component"
import class Color = "java.awt.Color"
import class JFrame = "javax.swing.JFrame"
import class JButton = "javax.swing.JButton"
import class SwingUtilities = "javax.swing.SwingUtilities"
import class WindowConstants = "javax.swing.WindowConstants"
import class Window = "java.awt.Window"
import class BoxLayout = "javax.swing.BoxLayout"
import class CanvasImpl = "orc.lib.gui.CanvasImpl"

import class Runnable = "java.lang.Runnable"

import site CallableToRunnable = "orc.lib.util.CallableToRunnable"
import site Block = "orc.lib.util.Block"

import site ActionListenerAdapter = "orc.lib.gui.ActionListenerAdapter"
import site WindowListenerAdapter = "orc.lib.gui.WindowListenerAdapter"
import site MouseListenerAdapter = "orc.lib.gui.MouseListenerAdapter"

site runOnKillHandler(check :: lambda() :: Top, callback :: lambda() :: Top) =
  (check() ; callback()) >> stop | signal

-- Call callable when this def call is killed.
-- As a side effect this will never halt.
def runOnKill(callback :: lambda() :: Top) = 
  site check() :: Top = Block()
  runOnKillHandler(check, callback)
  
class def FanoutChannel() :: FanoutChannel {
  -- A lock to protect internal data structure
  val lock = Semaphore(1)
  -- A list of output ("listener") channels
  val listeners = Ref[List[Channel]]([])
  
  -- Put an value on every listener channel
  def put(v) = each(listeners?) >c> c.put(v)
  
  -- Return a listener channel that will be active until this call is killed
  def newListener() = withLock(lock, {
    val c = Channel()
    runOnKill({ removeListener(c) }) >>
    listeners := c : listeners? >>
    c
  })
  
  -- Remove a listener channel
  def removeListener(c :: Channel) = withLock(lock, {
    listeners := filter(listeners?, { _ /= c })
  })
  
  -- Publish all values put on this channel while this call is active
  def listen() =
    val c = newListener()
    repeat(c.get)
}
  
def onEDTProfile(f :: lambda() :: Top) = SwingUtilities.invokeLater(CallableToRunnable({ Rtime() >t> f() >> Println("onEDT for " + (Rtime() - t) + " with " + f) }))
def onEDT(f :: lambda() :: Top) = SwingUtilities.invokeLater(CallableToRunnable(f))
def onEDTBlock[A](f :: lambda() :: A) = 
  val c = Cell[A]()
  SwingUtilities.invokeLater(CallableToRunnable({ c.write(f()) })) >>
  c.read()

def installOnEDTUntilKill(install, uninstall, arg) = 
	runOnKill({ onEDT({ uninstall(arg) }) })
	>> onEDT({ install(arg) })

class Component {
  val impl :: JComponent
}

class def Frame(children :: List[Component]) :: Frame extends Component {
  val impl = onEDTBlock({ JFrame() })
  val listener = WindowListenerAdapter(this)
  val _ = children >> impl -- First block on the fields we need and then take the "EDT lock"
	  >> onEDT({
	    impl.getContentPane().setLayout(BoxLayout(impl.getContentPane(), BoxLayout.PAGE_AXIS?))
	    >> impl.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE?)
	    >> impl.addWindowListener(listener)
	    >> map({ impl.add(_.impl) }, children)
	    >> impl.pack()
	    >> impl.setVisible(true)
	  })
  
  site setVisible(b :: Boolean) = Println("setVisible") >> onEDT({
    Println("setVisible onEDT")
    >> impl.pack()
    >> impl.setVisible(b)
  })
  
  def dispose() = onEDT({ impl.removeWindowListener(listener) }) >> impl.dispose()
  
  site add(c) = onEDT({ impl.add(c.impl) })

  val _ = runOnKill({ dispose() })
  
  val closingChannel = FanoutChannel()
  site windowClosing(e) = closingChannel.put(e)
  
  site onClosing() = closingChannel.listen()
}

class def Button(text :: String) :: Button extends Component {
  val impl = onEDTBlock({ JButton() })
  val _ = setText(text)
  
  site setText(s :: String) = onEDT({
    impl.setText(s)
  })
  
  val _ = installOnEDTUntilKill( impl.addActionListener, impl.removeActionListener, ActionListenerAdapter(this))
  
  val actionChannel = FanoutChannel()
  site actionPerformed(e) = actionChannel.put(e)  
  
  site onAction() = actionChannel.listen()
}

class def Canvas(width :: Integer, height :: Integer) :: Canvas extends Component {
  val width :: Integer
  val height :: Integer

  val impl = onEDTBlock({ CanvasImpl(width, height) })
  val image = impl.image()
  
  site setPixel(x, y, color) = onEDT({
  	image.setRGB(x, y, color)
  })
  
  site fill(color) = onEDT({
	image.createGraphics()
	>g> g.setColor(Color(color))
	>> g.fillRect(0, 0, width, height)
	-->> g.dispose()
  }) >> repaint()
  
  site repaint() = onEDT({
  	impl.repaint(0, 0, impl.getWidth(), impl.getHeight())
  })
  
  site getDisplayWidth() = onEDTBlock({ impl.getWidth() })
  site getDisplayHeight() = onEDTBlock({ impl.getHeight() })
  
  val _ = installOnEDTUntilKill( impl.addMouseListener, impl.removeMouseListener, MouseListenerAdapter(this))
  
  val mouseClickedChannel = FanoutChannel()
  site mouseClicked(e) = mouseClickedChannel.put(e)  
  
  site onClicked() = mouseClickedChannel.listen()
}

-- def closeGUI() = each(arrayToList(Window.getWindows())) >w> Println("Disposing " + w) >> w.dispose()
