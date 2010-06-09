{- Orc10.orc
 - 
 - Simple test for Orc nested expressions
 - 
 - Created by Brian on Jun 3, 2010 11:11:05 AM
 -}

1 + ( 2 | 3 )

{-
OUTPUT:
3
-}

{-
 - 4 is also a valid output, but our test harness can't test disjunctions yet.
 -
 - If this test produces an error, it means that the order of evaluation for |
 - has changed somehow, which is not necessarily wrong, but may be notable.
 -}
