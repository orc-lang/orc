include "net.inc"
include "ui.inc"

Prompt("Search for:") >term>
Google(term) >pages>
each(take(1, pages)) >results>
    each(results()) >result>
        (result.titleNoFormatting, result.url)