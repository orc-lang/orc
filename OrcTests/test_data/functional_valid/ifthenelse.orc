def downfrom(Integer) :: Integer
def downfrom(n) =
  if n = 0
  then 0
  else (n | downfrom(n-1))

downfrom(5)
{-
OUTPUT:PERMUTABLE:
5
4
3
2
1
0
-}

{- Nested comment {- one -} ... -}
{-
Nested comment
{- two -}
-}
