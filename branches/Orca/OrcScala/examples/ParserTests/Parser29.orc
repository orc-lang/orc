{- Parser29.orc
 - 
 - Torture test for the Orc parser
 -
 - UNGUARDED RECURSION
 - 
 - Created by brian on Jun 29, 2010 11:36:48 AM
 -}

def forkmonster() = forkmonster() | forkmonster()
forkmonster()