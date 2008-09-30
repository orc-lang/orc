def rbool() = random(2) >x> (x = 0)
{- EXAMPLE -}
-- 2 | 3

-- 2 | 3 | 4

-- rbool() | random(10)

{-
  "immediately"
| Rtimer(2000) >> "...two seconds later..."
| Rtimer(4000) >> "...four seconds later..."
-}
