{-
In order to run this demo, you must have:
1. a google developer account
2. in the classpath, /google.properties (see google.sample.properties)
-}

include "net.inc"
include "ui.inc"

val Google = GoogleSearchFactory("google.properties")
Prompt("Search for:") >term>
Google(term) >pages>
each(take(1, pages)) >page>
each(page()) >result>
(result.titleNoFormatting, result.url)