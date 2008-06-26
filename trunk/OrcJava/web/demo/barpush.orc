-- Sequential composition evaluates the right hand side for every value published by the left-hand side.

(
	David("Write a message for William:")
	| Adrian("Write a message for William:")
) >x>
Email(william, x)