{- java_call.orc -- Test Orc to Java bridge facility
 - 
 - $Id$
 - 
 - Created by jthywiss on Jul 14, 2010 8:48:56 PM
 -}

import class JavaFormatter = "java.util.Formatter"
import class JavaString = "java.lang.String"
import class Locale = "java.util.Locale"

(if JavaString.format("a") = "a" then "0 pass" else "0 FAIL") |
(if JavaString.format("a%s", 42) = "a42" then "1 pass" else "1 FAIL") |
(if JavaString.format("a%s%s", "test", 42) = "atest42" then "2 pass" else "2 FAIL") |
(if JavaString.format(Locale.US?, "a%s", 42) = "a42" then "3 pass" else "3 FAIL") |

(if JavaFormatter().format("a%s", 42).toString() = "a42" then "4 pass" else "4 FAIL") |
(if JavaFormatter().format("a%s%s", "test", 42).toString() = "atest42" then "5 pass" else "5 FAIL") |
(if JavaFormatter().format(Locale.US?, "a%s", 42).toString() = "a42" then "6 pass" else "6 FAIL") |

--TODO: These require a custom Java class to test against:
-- Non-object varargs lists both with correct and incorrect arguments
-- A vararg method without initial arguments
-- Something that doesn't involve a String in the initial arguments
-- Vararg constructor

stop


{-
OUTPUT:PERMUTABLE
"0 pass"
"1 pass"
"2 pass"
"3 pass"
"4 pass"
"5 pass"
"6 pass"
-}
