{- gOrc.orc
 - 
 - gOrc - a bunch of Orc gibberish
 - 
 - gOrc creates a number of tokens equal to totalTokens that
 - are valid Orc tokens and puts them together with a space
 - as the token separator.  This simulates gibberish
 - input to the parser.  The tokens are randomized from a Map().
 - You can modify it to make it an actual program if a sequence pops up that
 - looks like it would make a good test case.  Another good way to 
 - use it is to put the totalTokens at a really high number, then
 - examine the output to see if any interesting sequences appear.
 - 
 - This is not meant to automatically produce programs,
 - only to generate new ideas for parser test cases.
 - 
 - Created by brian on Jun 23, 2010 3:00:53 PM
 -}

-- CHANGE NUMBER OF TOKENS HERE
val totalTokens = 1000


import class Map = "java.util.HashMap"

val wordlist = Map()
def popList() =
   wordlist.put(0, "def") >>
   wordlist.put(1, "val") >>
   wordlist.put(2, "|") >>
   wordlist.put(3, ">>") >>
   wordlist.put(4, "<x<") >>
   wordlist.put(5, ";") >>
   wordlist.put(6, "stop") >>
   wordlist.put(7, "true") >>
   wordlist.put(8, "false") >>
   wordlist.put(9, "signal") >>
   wordlist.put(10, "site()") >>
   wordlist.put(11, "=") >>
   wordlist.put(12, "string") >>
   wordlist.put(13, "var") >>
   wordlist.put(14, " ") >>
   wordlist.put(15, "   ") >>
   wordlist.put(16, "\n") >>
   wordlist.put(17, "1") >>
   wordlist.put(18, "2") >>
   wordlist.put(19, "\t") >>
   wordlist.put(20, "\"")

def randToken() = 
   val num = Random(wordlist.size())
   Print(wordlist.get(num) + " ") >> stop

def gib(0) = stop
def gib(times) = randToken() | gib(times - 1)

popList() >> gib(totalTokens)


