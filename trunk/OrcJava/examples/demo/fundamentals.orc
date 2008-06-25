-- Fundamental sites

   if(3 /= 4) >> "three does not equal four"
   
 {- will never publish -}
 | if(false) >> "impossible!"
 
 {- will never publish -}
 | ( 6 | "seven" | David("What's your favorite number?") ) >> null
 
 {- wait three thousand milliseconds -}
 | Rtimer(3000) >> "...three seconds later..."
