{- spell.orc -- Orc program that spell checks words using Bing's spell service
 -}

{-
In order to use Bing in this demo, you must have:
1. a Azure database account with Bing dataset access
2. in the classpath, /bing.properties (see bing.sample.properties in OrcSites)
-}


include "net.inc"
include "ui.inc"

val BingSpell = BingSpellFactoryPropertyFile("bing.properties")

def spellCheck([], i) = stop
def spellCheck(word:words, i) =
    BingSpell(word) >(_:_) as suggs>
      (i, word, suggs)
  | spellCheck(words, i+1)

Prompt("Enter one or more words:") >text>
words(text) >words>
spellCheck(words, 1)
