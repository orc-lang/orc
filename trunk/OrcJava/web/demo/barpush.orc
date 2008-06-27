-- Sequential composition

-- Evaluate the right hand side for every value
-- published by the left-hand side.

( David("Pick a movie you like:")
  | Adrian("Pick a movie you like:") ) >movie>
cat("I heard that ", movie, " is a good movie.")