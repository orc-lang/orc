-- Fundamental sites: if, null, Rtimer

-- if publishes if the condition is true
  if(3 /= 4) >> "three does not equal four"
-- and does not publish if it is false
| if(false) >> "impossible!"

-- wait three thousand milliseconds
| Rtimer(3000) >> "...three seconds later..."

-- null never publishes
| David("Favorite number?") >> null

