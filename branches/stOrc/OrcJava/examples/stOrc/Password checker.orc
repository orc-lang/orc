-- Password checker.orc -- Demonstrate declassification in stOrc
--
-- $Id$
--
--
-- Demonstration of "declassifying" the results 
-- of an operation that used a secret.

val correctPassword = "secret" :: String{5}

def checkPassword(String) :: Boolean
def checkPassword(enteredPassword) = (enteredPassword = correctPassword) :!: Boolean{0}

(
  "checkPassword(wrong)=" + checkPassword("wrong")
| "checkPassword(secret)=" + checkPassword("secret")
) :: Top -- expecting a non-confidential result

{- 
-- The following will not type check, preventing a breach:
(
  "correctPassword=" + correctPassword    -- Try to reveal the secret
) :: Top
-}

{-
TYPE:  Top
OUTPUT:
"checkPassword(wrong)=false"
"checkPassword(secret)=true"
-}
