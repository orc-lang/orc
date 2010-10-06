{- Parser28.orc
 - 
 - Torture test for the Orc parser
 -
 - RUN-ON PROGRAM (NO USE OF BLOCK STRUCTURE)
 - 
 - Note this one works in the Java parser, and
 - may not necessarily be wrong, but a good
 - case to examine.
 -
 - Created by brian on Jun 29, 2010 11:22:01 AM
 -}

def a() = def b() = def c() = val x = 1 x c() b() a()

{- Above is version of below:
 -
 - def a() =
 -    def b() = 
 -       def c() =
 -          val x = 1
 -          x
 -       c()
 -    b()
 - a()
 -}