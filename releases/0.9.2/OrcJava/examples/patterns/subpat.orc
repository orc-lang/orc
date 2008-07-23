-- Using 'as' to capture sub-patterns
-- Output: (1,2,3,(2,3)) and (4,5,6,(5,6))

( (1,(2,3)) | (4,(5,6)) | (7,8,9) ) >(a,(b,c) as d)> (a,b,c,d)
