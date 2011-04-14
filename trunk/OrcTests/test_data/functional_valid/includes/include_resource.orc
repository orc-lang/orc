{- include_resource.orc -- Include declaration test case for a JAR resource
 - 
 - $Id: include_resource.orc 2265 2010-12-16 15:49:41Z jthywissen $
 - 
 - Created by jthywiss on Aug 4, 2010 10:31:05 AM
 -}

include "test_include_resource.inc"

if includeTest=includeTest then "include_resource pass" else "huh?"

{-
OUTPUT:
"include_resource pass"
-}
