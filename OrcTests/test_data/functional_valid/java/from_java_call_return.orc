{- from_java_call.orc -- Orc program from_java_call
 -
 - Created by amp on Feb 12, 2015 11:15:31 PM
 -}

import class Callable = "java.util.concurrent.Callable"
import site CallableToCallable = "orc.lib.util.CallableToCallable"

CallableToCallable({ "In Java" }).call()
|
Rwait(100) >> stop -- To keep the interpreter alive.

{-
OUTPUT:
"In Java"
-}
