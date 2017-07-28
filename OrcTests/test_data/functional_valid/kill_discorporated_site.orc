{- kill_discorporated_site.orc -- Test that sites whose bodies discorporate still behave correctly.
 -
 - Created by amp on Jul 28, 2017
 -}
 
val c = 
  val check = Cell()
  val optimizerDefeater = Cell()

  {|
  site c() :: Top = (site s() = stop # optimizerDefeater := s) >> stop
  check := c >> Rwait(200)
  |} | check?
  #

(c() ; "Halted") |
Rwait(100) >> "Before" |
Rwait(300) >> "After"

{-
OUTPUT:
"Before"
"Halted"
"After"
-}