-- Sequential composition: calling two sites in sequence.

-- For all values published by the left expression,
-- evaluate the right expression.

David("I'm about to ask Adrian about the weather.") >>
Adrian("How is the weather?")
