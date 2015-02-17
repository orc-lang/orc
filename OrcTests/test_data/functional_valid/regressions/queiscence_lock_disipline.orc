{- queiscence_lock_disipline.orc -- Attempt to trigger a dead-lock between various resolving classes and defs.
 -
 - $Id$
 -
 - Created by amp on Feb 12, 2015 2:15:07 PM
 -}

def test() =
class Component {
}

class Frame extends Component {
  val children :: List[Top]
  
  def onClosed() = Rwait(1000)
}
def Frame(children_ :: List[Top]) = new Frame with { val children = children_ }

class Button extends Component {
  val text :: String
  
  def setText(x) = signal
  def onClicked() = stop
}
def Button(text_ :: String) = new Button with { val text = text_ }

{|
val button = Button("Click me!")
val frame = Frame([button, Button("Dead")]) 
frame.onClosed()
|} 

val N = 1000
val counter = Counter(N)

upto(N) >> test() >> counter.dec() >> stop
|
counter.onZero() >> "Done" 

{-
OUTPUT:
"Done"
-}