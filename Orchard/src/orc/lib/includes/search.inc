-- search.inc

include "net.inc"

def Google(term) =
  def shorten(result) = (result.title, result.link)
  val GoogleSearch = GoogleSearchFactory("orc/orchard/orchard.properties")
  GoogleSearch(term, 1)  >results>
  map(shorten, results)

def Bing(term) =
  val BingWebSearch = BingWebSearchFactoryPropertyFile("orc/orchard/orchard.properties")
  BingWebSearch(term)
