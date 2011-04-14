include "net.inc"

-- Search google and return the first
-- page of results
val Google = GoogleSearchFactory("orc/orchard/orchard.properties")
Prompt("Search Google for:") >term>
Google(term) >pages>
each(take(1, pages)) >results>
    each(results()) >result>
        (result.titleNoFormatting, result.url)
