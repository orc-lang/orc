-- Using '!' to publish matching results as they match.
-- Note: '!' will only release these publications if the whole pattern succeeds.

{-
OUTPUT:
true
2
(2, 2)
-}
( (1,2,true) | (2,5,true) ) >(1,!x,!true)> (x,x) 