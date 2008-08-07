include "search.inc"

each(results)
  <results<
    Prompt("Search for:") >term>
    ( Yahoo(term) | Google(term) )