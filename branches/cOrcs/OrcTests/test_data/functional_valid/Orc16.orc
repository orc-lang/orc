{- Orc16.orc
 - 
 - Test for Orc synchronization idiom
 - 
 - Created by Brian on Jun 3, 2010 1:07:10 PM
 -}

def u() = Println("U ran")
def v() = Println("V ran")
def f() = Println("F ran") >> stop
def g() = Println("G ran") >> stop
Let(u(),v()) >> (f() | g())

{-
OUTPUT:
V ran
U ran
F ran
G ran
-}
{-
OUTPUT:
U ran
V ran
F ran
G ran
-}
{-
OUTPUT:
V ran
U ran
G ran
F ran
-}
{-
OUTPUT:
U ran
V ran
G ran
F ran
-}
