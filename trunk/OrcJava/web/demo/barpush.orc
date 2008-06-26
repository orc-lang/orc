-- Sequential composition evaluates the right hand side for every value published by the left-hand side.

(
	David("Pick a movie you like:")
	| Adrian("Pick a movie you like:")
) >x>
cat("I heard that ", x, " is a good movie.")