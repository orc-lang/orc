{- simple.orc -- Orc program simple
 - 
 - $Id$
 - 
 - Created by rjrocha on Jul 26, 2012 4:59:30 PM
 -}
--include "types.inc"
include "math.inc"
--include "comp.inc"
{-
import site Let = "orc.lib.builtin.Let"
import site Ift = "orc.lib.builtin.Ift"
import site Iff = "orc.lib.builtin.Iff"
import site Error = "orc.lib.util.Error"
-}
{-
import site (~) = "orc.lib.bool.Not"
import site (&&) = "orc.lib.bool.And"
import site (||) = "orc.lib.bool.Or"
-}
import site (:) = "orc.lib.builtin.structured.ConsConstructor"
import site Prompt = "orc.lib.util.Prompt"

{-
def for(Integer, Integer) :: Integer
def for(low, high) =
  if low >= high then stop
  else ( low | for(low+1, high) )
-}

--def length(List[Top]) :: Integer
def length([]) = 0
def length(x:xs) = 1 + length(xs)

--def each[A](List[A]) :: A
def each([]) = stop
def each(h:t) = h | each(t)

length([0,0,0,0]) >y>
(each([1,2,3,4,5]) | 6 | 7 | 8 | 9 | 10 | Prompt("number")) >i>
(i,y)
--Prompt("number") | Prompt("number 2") >j>
--j

