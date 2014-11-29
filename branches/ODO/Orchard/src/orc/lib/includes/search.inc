-- search.inc
--
-- $Id$
--

include "net.inc"

def Google(term) =
  def shorten(result) = (result.titleNoFormatting, result.url)
  val search = GoogleSearchFactory("orc/orchard/orchard.properties")
  search(term)  >pages>
  each(take(1, pages))  >results>
  map(shorten, results())

def Bing(term) =
  val search = BingWebSearchFactoryPropertyFile("orc/orchard/orchard.properties")
  search(term)
