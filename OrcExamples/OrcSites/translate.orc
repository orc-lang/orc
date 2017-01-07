{- translate.orc -- Translate simple text to German using Microsoft Translator
 -
 - Created by amp on Oct 10, 2016 6:06:59 PM
 -}

include "net.inc"

val Translate = MicrosoftTranslatorFactoryPropertyFile("bing.properties")
repeat({ Prompt("Translate:") }) >term>
Translate(term, "de")
