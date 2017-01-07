{- simple_button.orc -- Orc program simple_button
 -
 - Created by amp on Feb 11, 2015 7:37:38 PM
 -}

include "debug.inc"
include "gui.inc"

{|

val button = Button("Click me! Loaded.")
val frame = Frame([button, Button("Dead")]) #

button.onAction() >> Println("Clicked") >> button.setText("Click me! Started.") >> 
Rwait(3000) >> button.setText("Click me! Done.") >> stop

|

frame.onClosing() >e> Println("Killing: " + e) 

|} >> Println("Killed") >> stop
| Rwait(10000) >> DumpState()
