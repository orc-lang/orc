-- Tally the responses received from a list of sites within 10 seconds

-- Query a site for a number, and default to 0
-- if it does not respond within 10 seconds
def timeout(M) = n
    <n< M() | Rtimer(10000) >> 0

-- Add up the responses from a list of sites in parallel
def tally([]) = 0
def tally(first:rest) = timeout(first) + tally(rest)

tally([David, Adrian])
