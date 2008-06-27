-- Fundamental sites: if, null, Rtimer

if(3 /= 4) >> "three does not equal four"

| if(false) >> "impossible!"   {- will never publish -}

-- wait three thousand milliseconds
| Rtimer(3000) >> "...three seconds later..."

| David("Favorite number?") >> null   {- will never publish -}

