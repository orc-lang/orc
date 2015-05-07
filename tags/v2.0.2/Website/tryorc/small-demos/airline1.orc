-- This example simulates a simple
-- booking agent: it gets quotes from a
-- list of airlines, and returns the best
-- quote within $200 received within 15
-- seconds.

-- Build a simulated airline quote site
def Airline(name) =
    def MakeQuote() =
        Prompt(name + " quote ($)") >n>
        (name, Read(n))
    MakeQuote

-- Return the lesser of two quotes
def min((n1,q1), (n2,q2)) =
    Ift(q1 <= q2) >> (n1, q1)
  | Ift(q2 <: q1) >> (n2, q2)

-- Return the best quote at or under $200
-- received within 15 seconds
def bestQuote([]) = ("None of the above", 200)
def bestQuote(airline:rest) =
    val best = bestQuote(rest)
    val current = airline() | Rwait(15000) >> best
    min(current, best)

bestQuote([Airline("Delta"),
           Airline("United")])
