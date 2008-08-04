{-
In order to run this demo, you must have:
1. a yahoo application ID
2. in the classpath, /yahoo.properties (see yahoo.sample.properties)
-}

include "net.inc"
include "ui.inc"

val Yahoo = YahooSearchFactory("yahoo.properties")
Prompt("Search for:") >term>
Yahoo(term) >results>
each(take(5, results)) >result>
(result.Title, result.Url)