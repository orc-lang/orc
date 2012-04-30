{- Parser19.orc
 - 
 - Torture test for the Orc compiler
 -
 - IMPROPER USE OF REFERENCE
 - 
 - Created by brian on Jun 24, 2010 3:37:04 PM
 -}

val x = Ref(3)
def echo(a) = a
val q = Table(10, echo)
q(x)?