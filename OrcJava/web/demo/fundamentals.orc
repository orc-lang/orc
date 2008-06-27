-- Fundamental sites: if, null, Rtimer

if(3 /= 4) >> "three does not equal four"

-- will never publish
| if(false) >> "impossible!"

-- wait three thousand milliseconds
| Rtimer(3000) >> "...three seconds later..."

-- will never publish
| David("Favorite number?") >> null

