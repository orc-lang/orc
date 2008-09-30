{-
The following example searches both Yahoo and Google for some search term
obtained from the user and displays the first few results from whichever site
responds first.
-}
include "search.inc"

each(results)
  <results<
    Prompt("Search for:") >term>
    ( Yahoo(term) | Google(term) )
