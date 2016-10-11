{- bing.orc -- Orc program that performs a Bing search
 -}

{-
In order to run this demo, you must have:
1. a Azure database account with Bing dataset access
2. in the classpath, /bing.properties (see bing.sample.properties)
-}

include "net.inc"

val Bing = BingWebSearchFactoryPropertyFile("bing.properties")
Prompt("Search for:") >term>
Bing(term) >results>
each(results)
