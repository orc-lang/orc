{- vclock_designate_closures.orc -- Orc program vclock_designate_closures
 - 
 - Created by amp on Sep 17, 2013 1:26:07 PM
 -}

import class Thread = "java.lang.Thread"

def test() = 
  Vclock(IntegerTimeOrder) >>
  Vawait(0) >>
  (
  val x = Thread.sleep(100) >> 42
  def f() = x
    Vawait(2) >> Println(2) >> stop
  | x >> Vawait(1) >> Println(1) >> stop
  )
  
test()

{- 
OUTPUT:
1
2
-}
