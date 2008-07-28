-- This example simulates a simple
-- booking agent: it gets quotes from a
-- list of airlines, and returns the best
-- quote under $200 received within 10
-- seconds.

-- Build a simulated airline quote site
def Airline(name) =
    def MakeQuote() =
        Prompt(name + " quote ($)") >n>
        (name, parseInt(n))
    MakeQuote

-- Return the lesser of two quotes
def min((n1,q1), (n2,q2)) =
    if(q1 <: q2) >> (n1, q1)
  | if(q2 <= q1) >> (n2, q2)

-- Return the best quote under 200 dollars
-- within 10 seconds
def bestQuote([]) = ("None of the above", 200)
def bestQuote(airline:rest) =
    val best = bestQuote(rest)
    val current = airline() | Rtimer(10000) >> best
    min(current, best)

bestQuote([Airline("Delta"),
           Airline("United")])
