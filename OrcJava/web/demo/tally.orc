-- Tally the responses received from a list of sites within 5 seconds

-- Query a site for a number, but publish 0
-- if it does not respond within 8 seconds
def timeout(M) = 
	n  <n<  M() | Rtimer(8000) >> 0

-- Tally up the responses from a list of sites,
-- where each call is on a 5 second timeout
def tally([]) = 0
def tally(M:MS) = timeout(M) + tally(MS)

-- Convert a chat site to a tally site
def convert(N) = 
	lambda () = 
		N("Pick a number: ") >s> parseInt(s)

-- Create a list of tally sites by mapping
-- convert over a list of chat sites
val sites = map(convert, [David, Adrian])

-- Tally this list of sites
tally(sites)
