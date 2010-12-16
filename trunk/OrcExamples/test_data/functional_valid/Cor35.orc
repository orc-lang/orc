{- Cor35.orc
 - 
 - Simple test of Cor comments
 - 
 - Created by Brian on Jun 3, 2010 10:12:19 AM
 -}

{- I
am
a
multi-line
comment
-}

{- {- I am a nested comment -} -}

-- I am a single-line comment

1 + {- I am hidden in an expression -} 1

{-
OUTPUT:
2
-}