-- Example of an expression call.

-- Publishes different values at different times
def Query() = Random(2,100)
-- Returns x if x is acceptable; is silent otherwise
def Accept(x) = if(isPrime(x)) >> x

-- Produce all acceptable values by calling Query
-- at unit intervals forever.
Metronome() >> Query() >x> Accept(x)