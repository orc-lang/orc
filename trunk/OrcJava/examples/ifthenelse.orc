def downfrom(n) =
  if n = 0
  then 0
  else (n | downfrom(n-1))

downfrom(5)
{-
OUTPUT:
5
4
3
2
1
0
-}