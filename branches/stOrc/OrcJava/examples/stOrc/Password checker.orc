-- Password checker.orc -- Demonstrate declassification in stOrc
--
-- $Id$
--
--
-- Demonstration of "declassifying" the results 
-- of an operation that used a secret.

val correctPassword = "secret" :: String{C5}

def untrustedPrintln(out::Top{A0})::Signal = println(out)

def checkPassword(String) :: Boolean
def checkPassword(enteredPassword) = (enteredPassword = correctPassword) :!: Boolean{}

  untrustedPrintln("checkPassword(wrong)=" + checkPassword("wrong"))
| untrustedPrintln("checkPassword(secret)=" + checkPassword("secret"))

{- 
-- The following will not type check, preventing a breach:
untrustedPrintln("correctPassword=" + correctPassword)    -- Try to reveal the secret
-}

{-
TYPE:  Top
OUTPUT:
"checkPassword(wrong)=false"
"checkPassword(secret)=true"
-}
