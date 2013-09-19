{- vclock_designate_closures.orc -- Orc program vclock_designate_closures
 - 
 - $Id$
 - 
 - Created by amp on Sep 17, 2013 1:26:07 PM
 -}

def test() = 
  Channel[Integer]() >c>
  (
  Rwait(100) >> c.put(2) >> stop |
  Vclock(IntegerTimeOrder) >>
  Vawait(0) >>
  (
  val x = c.get() >> 42
  def f() = x
    x >> Vawait(1) >> Println(1) >> stop
  | Vawait(2) >> Println(2) >> stop
  )
  )
  
test()

{- 
OUTPUT:
1
2
-}
