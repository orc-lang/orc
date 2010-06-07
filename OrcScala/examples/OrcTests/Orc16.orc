{- Orc16.orc
 - 
 - Test for Orc synchronization idiom
 - 
 - Created by Brian on Jun 3, 2010 1:07:10 PM
 -}

def u() = println("U ran")
def v() = println("V ran")
def f() = println("F ran") >> stop
def g() = println("G ran") >> stop
let(u(),v()) >> (f() | g())

{-
OUTPUT:
V ran
U ran
F ran
G ran

OR

V and U may switch places and F and G may switch places
however V and U must strictly come before F and G
-}