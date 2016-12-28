{- from_java_call.orc -- Orc program from_java_call
 -
 - $Id$
 -
 - Created by amp on Feb 12, 2015 11:15:31 PM
 -}
 
import class Thread = "java.lang.Thread"
import class Runnable = "java.lang.Runnable"
import site CallableToRunnable = "orc.lib.util.CallableToRunnable"

val y = Rwait(200) >> 42

site s1(x :: Integer) = x + y 
site s2() = Println(y)

val t = Thread(CallableToRunnable(s2))
t.start() >>
Thread(CallableToRunnable({ t.join() >> Println(s1(42)) })).start() 
>> stop
|
Rwait(500) >> stop -- To keep the interpreter alive.

{-
OUTPUT:
42
84
-}
