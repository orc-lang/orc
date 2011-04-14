{- ident_norm.orc -- Orc compiler test case program
 - 
 - $Id: ident_norm.orc 2278 2010-12-18 08:05:25Z jthywissen $
 - 
 - Created by jthywiss on Dec 17, 2010 6:18:57 PM
 -}

{-
   This tests identifier normalization.
   The two identifiers in the code below appear to be the same,
   but in fact are different character sequences.
-}

-- Do NOT touch this file with an editor that might "help" you
-- by normalizing text -- that would invalidate the test.

val Trîcky_Nåme = 1  -- Not NFC

Trîcky_Nåme = Trîcky_Nåme && "Trîcky_Nåme" /= "Trîcky_Nåme"
{- Not NFC        NFC           Not NFC           NFC    -}

{-
OUTPUT:
true
-}
