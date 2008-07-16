-- Using '!' to publish matching results as they match.
-- Note: '!' will only release these publications if the whole pattern succeeds.
-- Output: 2 and true and (2,2)

( (1,2,true) | (2,5,true) ) >(1,!x,!true)> (x,x) 