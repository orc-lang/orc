val Google = Webservice("http://api.google.com/GoogleSearch.wsdl")
val GoogleDevKey = "YOURDEVKEY"
def GoogleSearch(keywords) = Google.doGoogleSearch(
	GoogleDevKey, keywords, 0, 10, true, "", true, "", "", "")

each(GoogleSearch("test").getResultElements()) >r>
	r.getURL()