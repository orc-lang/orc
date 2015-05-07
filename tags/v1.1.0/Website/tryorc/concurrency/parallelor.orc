-- By analogy to a traditional short-
-- circuiting sequential "or" operation, we
-- return true as soon as any expression
-- evaluates to true, and false if all
-- expressions evaluate to false.

def ift(x) = if(x) >> true

z <z< (

  ift(x) | ift(y) | (x || y)

    <x< Prompt("true or false?") >x> read(x)
    <y< Prompt("true or false?") >y> read(y)
)

-- Source:
-- Computation Orchestration: A Basis for Wide-Area Computing (DOI)
-- Jayadev Misra and William R. Cook
-- Journal of Software and Systems Modeling, May 2006.
