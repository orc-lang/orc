{- include_resource.orc -- Include declaration test case for a JAR resource
 - 
 - $Id: include_resource.orc 2142 2010-09-29 21:36:35Z jthywissen $
 - 
 - Created by jthywiss on Aug 4, 2010 10:31:05 AM
 -}

include "test_include_resource.inc"

if includeTest=includeTest then "include_resource pass" else "huh?"

{-
OUTPUT:
"include_resource pass"
-}
