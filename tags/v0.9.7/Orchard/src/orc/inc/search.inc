include "net.inc"

def Google(term) = 
	def shorten(result) =
        (result.titleNoFormatting, result.url)
	val search = GoogleSearchFactory("orc/orchard/google.properties")
	search(term) >pages>
	each(take(1, pages)) >results>
	map(shorten, results())
	        
def Yahoo(term) = 
	def shorten(result) =
		(result.Title, result.Url)
	val search = YahooSearchFactory("orc/orchard/yahoo.properties")
	search(term, 5) >results>
	map(shorten, results)