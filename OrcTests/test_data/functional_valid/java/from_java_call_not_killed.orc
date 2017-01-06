{- from_java_call.orc -- Orc program from_java_call
 -
 - Created by amp on Feb 12, 2015 11:15:31 PM
 -}

import class Thread = "java.lang.Thread"
import class Runnable = "java.lang.Runnable"
import site CallableToRunnable = "orc.lib.util.CallableToRunnable"

val t = Thread(CallableToRunnable({ 
Rwait(100) >> Println("EDT1") >> Rwait(100) 
| Rwait(400) >> Println("Later") 
}))
t.start() >>
Thread(CallableToRunnable({ t.join() >> Println("EDT2") })).start() 
>> stop
|
Rwait(500) >> stop -- To keep the interpreter alive.

{-
OUTPUT:
EDT1
EDT2
Later
-}
